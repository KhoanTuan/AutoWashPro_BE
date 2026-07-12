# MoMo Payment Integration - Implementation Notes

## Project Structure

```
src/main/java/com/autowashpro/autowashpro_be/
├── config/
│   ├── MomoProperties.java          # Configuration properties binding
│   └── RestTemplateConfig.java      # HTTP client configuration
├── common/
│   └── util/
│       └── HmacSHA256Util.java      # Signature generation utility
└── modules/
    └── booking/
        ├── controller/
        │   └── PaymentController.java           # REST endpoints
        ├── dto/
        │   ├── MomoPaymentRequest.java
        │   ├── MomoPaymentResponse.java
        │   ├── MomoIpnCallbackRequest.java
        │   ├── CheckoutRequest.java
        │   └── CheckoutResponse.java
        ├── entity/
        │   ├── PaymentTransaction.java          # JPA entity
        │   └── (existing booking entities)
        ├── repository/
        │   └── PaymentTransactionRepository.java
        └── service/
            ├── MomoPaymentService.java          # Core service
            └── (existing booking service)
```

## Class Descriptions

### 1. MomoProperties (Configuration)

**Purpose:** Spring Configuration Properties binding for MoMo credentials.

**Key Fields:**
- `partnerCode`: MoMo partner identifier
- `accessKey`: API authentication key
- `secretKey`: HMAC signing key (kept private)
- `endpoint`: MoMo Sandbox API URL
- `redirectUrl`: Frontend success page
- `ipnUrl`: Backend callback webhook

**Annotation:** `@ConfigurationProperties(prefix = "app.payment.momo")`

**Usage:**
```java
@Autowired
private MomoProperties momoProperties;

// Access properties
String endpoint = momoProperties.getEndpoint();
```

---

### 2. HmacSHA256Util (Cryptographic Utility)

**Purpose:** Generate and verify HMAC-SHA256 signatures for payment security.

**Key Methods:**

#### `generateSignature(String message, String secretKey) -> String`
- Generates Base64-encoded HMAC-SHA256 signature
- **Input:** Raw payload data and secret key
- **Output:** Base64-encoded signature string
- **Exception:** RuntimeException if algorithm unavailable

#### `verifySignature(String message, String secretKey, String providedSignature) -> boolean`
- Verifies if provided signature matches computed signature
- **Input:** Original message, secret key, provided signature
- **Output:** true if valid, false otherwise
- **Use Case:** IPN callback validation

**Implementation Details:**
```
Algorithm: HmacSHA256
Charset: UTF-8
Encoding: Base64
```

**Example:**
```java
String rawData = "amount=100000&orderId=ORDER123&...";
String signature = HmacSHA256Util.generateSignature(rawData, secretKey);

// Verify incoming callback
boolean valid = HmacSHA256Util.verifySignature(
    rawData, 
    secretKey, 
    incomingSignature
);
```

---

### 3. DTOs (Data Transfer Objects)

#### MomoPaymentRequest
**Purpose:** Request payload sent to MoMo API

**Key Fields:**
- `partnerCode`: Merchant identifier
- `amount`: Payment amount in VND (must be >= 1,000)
- `orderId`: Unique order identifier
- `requestId`: Unique request identifier
- `signature`: HMAC-SHA256 signature of request
- `redirectUrl`: Customer redirect after payment
- `ipnUrl`: Webhook for payment notifications

#### MomoPaymentResponse
**Purpose:** Response from MoMo API

**Key Fields:**
- `resultCode`: 0 = success, others = failure
- `payUrl`: Payment gateway URL for customer
- `transId`: MoMo transaction ID
- `signature`: Response signature (optional validation)

#### MomoIpnCallbackRequest
**Purpose:** Payment notification sent by MoMo

**Key Fields:**
- `requestId`: Original request ID
- `orderId`: Original order ID
- `resultCode`: Payment result (0 = success)
- `transId`: MoMo transaction ID
- `signature`: HMAC-SHA256 signature for verification

#### CheckoutRequest (Customer Input)
**Purpose:** REST request from frontend for payment initiation

**Fields:**
- `bookingId`: Booking to pay for
- `paymentMethod`: Payment method ("MOMO", "CASH", etc.)
- `notes`: Optional customer notes

#### CheckoutResponse (API Response)
**Purpose:** Response to frontend with payment details

**Fields:**
- `transactionId`: Transaction reference in our system
- `paymentUrl`: URL to redirect customer to MoMo
- `status`: Payment status ("PENDING", "PROCESSING", etc.)
- `momoRequestId`: MoMo request tracking ID
- `momoOrderId`: MoMo order tracking ID

---

### 4. PaymentTransaction (JPA Entity)

**Purpose:** Persists payment transaction records for audit and reconciliation.

**Key Fields:**
```java
@ManyToOne
private Booking booking;              // Foreign key to booking

String paymentGateway;                // "MOMO", "BANK_TRANSFER", etc.
String momoTransId;                   // MoMo transaction reference
String momoRequestId;                 // Our request ID
String momoOrderId;                   // Our order ID
BigDecimal amount;                    // Payment amount
String status;                        // PENDING, PROCESSING, SUCCESS, FAILED
Integer resultCode;                   // MoMo result code
String resultMessage;                 // MoMo result message
String requestPayload;                // Raw request sent (TEXT, for debugging)
String responsePayload;               // Raw response received (TEXT, for debugging)
String callbackPayload;               // IPN callback data (TEXT, for auditing)
String errorDetails;                  // Error information if payment failed
```

**Database Table:**
```sql
payment_transaction (
  transaction_id BIGINT PRIMARY KEY,
  booking_id BIGINT FOREIGN KEY,
  payment_gateway VARCHAR(50),
  momo_trans_id VARCHAR(100),
  momo_request_id VARCHAR(100),
  momo_order_id VARCHAR(100),
  amount DECIMAL(12, 2),
  status VARCHAR(30),
  result_code INT,
  result_message TEXT,
  request_payload TEXT,
  response_payload TEXT,
  callback_payload TEXT,
  error_details TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

---

### 5. MomoPaymentService (Core Service)

**Purpose:** Orchestrates MoMo payment operations.

**Key Methods:**

#### `checkoutWithMoMo(Long bookingId, Long customerId) -> CheckoutResponse`

**Flow:**
1. Fetch and validate booking
2. Check authorization (booking belongs to customer)
3. Check booking not already paid
4. Generate unique request ID and order ID
5. Build MoMo payment request
6. Calculate HMAC-SHA256 signature
7. Create PaymentTransaction record (PENDING)
8. Send HTTP POST to MoMo endpoint
9. Parse MoMo response
10. Update PaymentTransaction with response
11. Return CheckoutResponse with payUrl

**Error Handling:**
- Booking not found → ResourceNotFoundException
- Unauthorized access → BadRequestException
- Already paid → BadRequestException
- MoMo API error → BadRequestException

**Response Example:**
```json
{
  "transactionId": "100",
  "bookingId": 5,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/confirm?...",
  "status": "PROCESSING",
  "message": "Successful"
}
```

---

#### `processIpnCallback(MomoIpnCallbackRequest callback) -> Map<String, Object>`

**Flow:**
1. Verify callback signature (security check)
2. Find PaymentTransaction by requestId
3. Store callback payload for audit
4. Check resultCode:
   - If 0 (success): Call processSuccessfulPayment()
   - If != 0 (failure): Call processFailedPayment()
5. Save updated PaymentTransaction
6. Return status response to MoMo

**processSuccessfulPayment() Logic:**
1. Mark transaction as SUCCESS
2. Update Booking:
   - paymentStatus = PAID
   - status = COMPLETED
3. Call processLoyaltyPoints()
4. Call checkAndUpdateTier()

**processFailedPayment() Logic:**
1. Mark transaction as FAILED
2. Store error details
3. Do NOT update booking (remains UNPAID)

**Loyalty Points Calculation:**
```java
pointsEarned = (amount / 1000) * tier_multiplier
customerLoyaltyPoints += pointsEarned
customerTotalSpending += amount
```

Example: Amount = 250,000 VND, Tier Multiplier = 1.5
- Points earned = (250000 / 1000) * 1.5 = 375 points

---

#### Signature Verification in IPN

**Process:**
1. Extract signature from callback
2. Reconstruct raw data with exact field order
3. Compute expected signature using secret key
4. Compare expected vs provided
5. Return false if mismatch (fraud detection)

**Raw Data Format (IPN):**
```
accessKey=...&amount=...&extraData=...&orderId=...
&orderInfo=...&orderType=...&partnerCode=...
&requestId=...&responseTime=...&resultCode=...
&resultMessage=...&transId=...
```

---

### 6. PaymentController (REST API)

**Purpose:** Exposes REST endpoints for payment operations.

#### `POST /api/v1/customer/bookings/checkout`

**Authentication:** Required (CUSTOMER role)

**Request:**
```json
{
  "bookingId": 123,
  "paymentMethod": "MOMO"
}
```

**Response (201 Created):**
```json
{
  "transactionId": "101",
  "bookingId": 123,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/...",
  "status": "PROCESSING",
  "message": "Successful"
}
```

**Error Responses:**
- 401 Unauthorized: Not authenticated
- 400 Bad Request: Invalid payment method or booking error
- 404 Not Found: Booking not found

**Implementation:**
```java
@PostMapping("/customer/bookings/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<CheckoutResponse> checkout(
    @Valid @RequestBody CheckoutRequest request,
    @AuthenticationPrincipal UserPrincipal principal) {
    
    // Delegate to service
    CheckoutResponse response = momoPaymentService.checkoutWithMoMo(
        request.getBookingId(),
        principal.getId()
    );
    
    return ResponseEntity.ok(response);
}
```

---

#### `POST /api/v1/callback/momo/ipn`

**Authentication:** Not required (public webhook for MoMo)

**Security:** Signature validation inside service layer

**Request (from MoMo):**
```json
{
  "partnerCode": "MOMO12345",
  "requestId": "REQ-123-...",
  "amount": 250000,
  "resultCode": 0,
  "resultMessage": "Success.",
  "transId": 202501...,
  "signature": "...",
  ...
}
```

**Response (200 OK):**
```json
{
  "statusCode": 0,
  "message": "Success"
}
```

**Error Response (200 OK with error status):**
```json
{
  "statusCode": 1,
  "message": "Invalid signature"
}
```

**Note:** Always return 200 OK, even on errors, to acknowledge receipt to MoMo.

---

### 7. RestTemplateConfig (HTTP Configuration)

**Purpose:** Configure HTTP client for external API calls.

**Configuration:**
- Connect timeout: 5 seconds
- Read timeout: 10 seconds

**Bean:**
```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(10))
        .build();
}
```

---

## Integration Points with Existing Code

### 1. Booking Entity Relationship
```java
// PaymentTransaction has @ManyToOne relationship with Booking
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "booking_id", nullable = false)
private Booking booking;
```

### 2. Customer Loyalty Points Integration
**Location:** `MomoPaymentService.processLoyaltyPoints()`

```java
// Updates after successful payment:
customer.setLoyaltyPoints(newPoints);
customer.setTotalSpending(newSpending);
customerRepository.save(customer);
```

### 3. Booking Status Updates
**Location:** `MomoPaymentService.processSuccessfulPayment()`

```java
// Updates on successful IPN callback:
booking.setPaymentStatus(PaymentStatus.PAID);
booking.setStatus(BookingStatus.COMPLETED);
bookingRepository.save(booking);
```

### 4. Security Integration
**Location:** `PaymentController`

```java
@PreAuthorize("hasRole('CUSTOMER')")
// Uses Spring Security to protect checkout endpoint
```

---

## Database Schema Changes

### New Table: payment_transaction
```sql
CREATE TABLE payment_transaction (
    transaction_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES booking(booking_id),
    payment_gateway VARCHAR(50) NOT NULL,
    momo_trans_id VARCHAR(100),
    momo_request_id VARCHAR(100),
    momo_order_id VARCHAR(100),
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    result_code INTEGER,
    result_message TEXT,
    request_payload TEXT,
    response_payload TEXT,
    callback_payload TEXT,
    error_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_payment_transaction_booking_id ON payment_transaction(booking_id);
CREATE INDEX idx_payment_transaction_momo_request_id ON payment_transaction(momo_request_id);
CREATE INDEX idx_payment_transaction_status ON payment_transaction(status);
```

### Existing Table Changes: None required
- Booking entity already has `paymentStatus` and `finalAmount`
- Customer entity already has `loyaltyPoints` and `totalSpending`

---

## Configuration in application.yaml

```yaml
app:
  payment:
    momo:
      partner-code: ${MOMO_PARTNER_CODE:MOMO12345}
      access-key: ${MOMO_ACCESS_KEY:F8635FE50A0FE6588}
      secret-key: ${MOMO_SECRET_KEY:NFcpIQIChnh0szvg6Z9zesZDZURAVESz}
      endpoint: ${MOMO_ENDPOINT:https://test-payment.momo.vn/v2/gateway/api/create}
      redirect-url: ${MOMO_REDIRECT_URL:http://localhost:3000/payment-success}
      ipn-url: ${MOMO_IPN_URL:http://localhost:8080/api/v1/callback/momo/ipn}
```

---

## Security Considerations

### 1. Secret Key Protection
- Never commit actual secret key to version control
- Always use environment variables in production
- Use `.env` files locally with `.gitignore`

### 2. Signature Verification
- All requests and callbacks are HMAC-SHA256 signed
- Signature verification prevents tampering/fraud
- IPN callbacks must verify signature before processing

### 3. HTTPS in Production
- All communication with MoMo must use HTTPS
- IPN callback endpoint must be HTTPS for production
- Never use HTTP URLs for production

### 4. Authentication
- Checkout endpoint requires authenticated user (CUSTOMER role)
- IPN callback endpoint is public but signature-protected
- Authorization checks booking ownership

### 5. Amount Validation
- MoMo requires amount >= 1,000 VND
- Amounts are stored with 2 decimal places
- Final amount used: booking.getFinalAmount() after voucher

---

## Testing Strategy

### Unit Tests
```java
// Test HmacSHA256Util
@Test
public void testSignatureGeneration() {
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    assert signature != null && !signature.isEmpty();
}

@Test
public void testSignatureVerification() {
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    boolean valid = HmacSHA256Util.verifySignature(rawData, secretKey, signature);
    assertTrue(valid);
}
```

### Integration Tests
```java
// Test checkout endpoint
@Test
@WithMockUser(roles = "CUSTOMER")
public void testCheckoutEndpoint() {
    CheckoutRequest request = new CheckoutRequest(bookingId, "MOMO", null);
    ResponseEntity<CheckoutResponse> response = 
        restTemplate.postForEntity("/api/v1/customer/bookings/checkout", 
                                   request, CheckoutResponse.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
}
```

### Manual Testing
1. Start application with MoMo sandbox credentials
2. Create booking through API
3. Initiate checkout with bookingId
4. Open returned payUrl in browser
5. Complete payment in MoMo sandbox
6. Verify IPN callback received and processed

---

## Performance Considerations

### Database Indexes
Created on frequently queried fields:
- `booking_id` (foreign key lookups)
- `momo_request_id` (IPN processing)
- `status` (payment tracking)
- `created_at` (time-based queries)

### RestTemplate Timeouts
- Connect timeout: 5s (connection establishment)
- Read timeout: 10s (waiting for response)
- Prevents hanging if MoMo servers slow/down

### Transaction Management
- `@Transactional` on service methods ensures atomic operations
- Database constraints prevent orphaned payment records
- Rollback if any step in payment processing fails

---

## Troubleshooting Guide

### Issue: "Signature verification failed"
**Cause:** Secret key incorrect or raw data order wrong
**Solution:** 
- Verify MOMO_SECRET_KEY matches dev.momo.vn dashboard
- Check field order in signature generation

### Issue: "Booking not found"
**Cause:** Invalid booking ID or wrong customer
**Solution:**
- Verify booking exists with `GET /api/v1/customer/bookings/{id}`
- Ensure authenticated user owns the booking

### Issue: "IPN callback not received"
**Cause:** Network/firewall issue or incorrect ipnUrl
**Solution:**
- Check ipnUrl is publicly accessible
- Use ngrok for localhost tunneling in development
- Check application logs for callback attempts

### Issue: "Amount validation failed"
**Cause:** Amount < 1000 VND
**Solution:**
- Ensure booking finalAmount is >= 1000 VND
- Add minimum booking amount requirement

---

## Migration from Non-MoMo to MoMo

1. Run database migration to create payment_transaction table
2. Update application.yaml with MoMo credentials
3. Test on Sandbox first
4. Deploy to production with production credentials
5. Existing bookings can still be paid via cash/bank transfer
6. MoMo is optional payment method alongside existing methods

---

## Future Enhancements

1. **Multiple Payment Methods:** Support bank transfer, credit card, etc.
2. **Payment Retry Logic:** Automatic retry on network failures
3. **Payment Status Polling:** Fallback if IPN callback not received
4. **Refund Processing:** Handle payment refunds/cancellations
5. **Analytics Dashboard:** Track payment metrics and trends
6. **Webhook Signature Rotation:** Periodic secret key rotation
7. **Rate Limiting:** Prevent abuse of payment endpoints
8. **Audit Logging:** Detailed logging for compliance

---

## References

- [MoMo Developer Portal](https://dev.momo.vn)
- [MoMo Sandbox Documentation](https://dev.momo.vn/sandbox)
- [HMAC-SHA256 Algorithm](https://en.wikipedia.org/wiki/HMAC)
- [REST API Best Practices](https://restfulapi.net/)
- [Spring Boot Security](https://spring.io/guides/gs/securing-web/)
- [JPA Best Practices](https://thoughts.dev/2024/01/jpa-best-practices/)

---

**Version:** 1.0
**Last Updated:** 2025-01-15
**Author:** AutoWashPro Development Team
**Status:** Production Ready
