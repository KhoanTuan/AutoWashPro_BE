# MoMo Payment Integration - Test Cases & Scenarios

## Test Case Suite

### 1. Checkout Endpoint Tests

#### TC-001: Successful Payment Initiation
**Objective:** Verify successful MoMo checkout flow

**Preconditions:**
- Customer is authenticated with CUSTOMER role
- Booking exists with ID = 5
- Booking status = PENDING, payment_status = UNPAID
- Booking final_amount = 250,000 VND (>= 1000)
- MoMo sandbox configured and accessible

**Test Steps:**
```
1. POST /api/v1/customer/bookings/checkout
   Authorization: Bearer {valid_jwt_token}
   Content-Type: application/json
   {
     "bookingId": 5,
     "paymentMethod": "MOMO",
     "notes": "Test payment"
   }
```

**Expected Results:**
- Status: 200 OK
- Response contains:
  - transactionId: non-null numeric string
  - paymentUrl: valid MoMo payment URL
  - status: "PROCESSING"
  - momoRequestId: format "REQ-{bookingId}-{timestamp}"
  - momoOrderId: format "{bookingCode}-{timestamp}"
- Payment transaction saved in database
- Booking remains in PENDING status (until IPN callback)

---

#### TC-002: Invalid Payment Method
**Objective:** Reject non-MoMo payment methods

**Preconditions:** Same as TC-001

**Test Steps:**
```
POST /api/v1/customer/bookings/checkout
{
  "bookingId": 5,
  "paymentMethod": "CREDIT_CARD"  // Unsupported
}
```

**Expected Results:**
- Status: 400 Bad Request
- Message: "Hiện tại chỉ hỗ trợ thanh toán qua MoMo"
- No transaction created

---

#### TC-003: Booking Not Found
**Objective:** Verify error handling for non-existent booking

**Test Steps:**
```
POST /api/v1/customer/bookings/checkout
{
  "bookingId": 99999,  // Non-existent
  "paymentMethod": "MOMO"
}
```

**Expected Results:**
- Status: 404 Not Found
- Message: "Booking not found with id: 99999"

---

#### TC-004: Booking Already Paid
**Objective:** Prevent duplicate payment for paid bookings

**Preconditions:**
- Booking exists with payment_status = PAID

**Test Steps:**
```
POST /api/v1/customer/bookings/checkout
{
  "bookingId": 5,
  "paymentMethod": "MOMO"
}
```

**Expected Results:**
- Status: 400 Bad Request
- Message: "Booking already paid"

---

#### TC-005: Unauthorized Access
**Objective:** Verify access control - customer cannot pay others' bookings

**Preconditions:**
- Booking belongs to different customer

**Test Steps:**
```
POST /api/v1/customer/bookings/checkout
Authorization: Bearer {other_customer_token}
{
  "bookingId": 5,  // Belongs to first customer
  "paymentMethod": "MOMO"
}
```

**Expected Results:**
- Status: 400 Bad Request
- Message: "Unauthorized: Booking does not belong to this customer"

---

#### TC-006: No Authentication
**Objective:** Reject unauthenticated requests

**Test Steps:**
```
POST /api/v1/customer/bookings/checkout
{
  "bookingId": 5,
  "paymentMethod": "MOMO"
}
// No Authorization header
```

**Expected Results:**
- Status: 401 Unauthorized

---

### 2. IPN Callback Tests

#### TC-007: Successful Payment Notification
**Objective:** Process successful MoMo payment callback

**Preconditions:**
- Checkout initiated and transaction in PENDING status
- MoMo receives payment from customer
- Payment amount = 250,000 VND

**Test Steps:**
```
POST /api/v1/callback/momo/ipn
Content-Type: application/json
{
  "partnerCode": "MOMO12345",
  "accessKey": "F8635FE50A0FE6588",
  "requestId": "REQ-5-1705123456789",
  "amount": 250000,
  "orderId": "NV-250115-0001-1705123456",
  "orderInfo": "Thanh toán đơn rửa xe NV-250115-0001",
  "orderType": "momo_wallet",
  "resultCode": 0,
  "resultMessage": "Success.",
  "transId": 20250115123456789,
  "responseTime": 1705123500000,
  "paymentOption": "webApp",
  "signature": "VALID_HMAC_SHA256_SIGNATURE",
  "extraData": ""
}
```

**Expected Results:**
- Status: 200 OK
- Response: `{"statusCode": 0, "message": "Success"}`
- PaymentTransaction updated:
  - status: "SUCCESS"
  - resultCode: 0
  - resultMessage: "Success."
  - momoTransId: "20250115123456789"
  - callbackPayload: stored for audit
- Booking updated:
  - payment_status: PAID
  - status: COMPLETED
- Customer updated:
  - loyaltyPoints: increased by (250000 / 1000) * 1.5 = 375
  - totalSpending: increased by 250000

---

#### TC-008: Payment Declined (Result Code != 0)
**Objective:** Handle payment failure notification

**Test Steps:**
```
POST /api/v1/callback/momo/ipn
{
  ...same as TC-007 but...
  "resultCode": 1001,
  "resultMessage": "Transaction declined"
}
```

**Expected Results:**
- Status: 200 OK
- Response: `{"statusCode": 0, "message": "Success"}`
- PaymentTransaction updated:
  - status: "FAILED"
  - resultCode: 1001
  - resultMessage: "Transaction declined"
- Booking remains:
  - payment_status: UNPAID
  - status: PENDING
- Customer loyalty NOT updated

---

#### TC-009: Invalid Signature
**Objective:** Reject tampered IPN callbacks

**Test Steps:**
```
POST /api/v1/callback/momo/ipn
{
  ...same as TC-007 but...
  "signature": "INVALID_OR_TAMPERED_SIGNATURE"
}
```

**Expected Results:**
- Status: 200 OK
- Response: `{"statusCode": 1, "message": "Invalid signature"}`
- PaymentTransaction NOT updated
- Booking remains unchanged
- No loyalty points processed

---

#### TC-010: Transaction Not Found
**Objective:** Handle IPN for non-existent transaction

**Test Steps:**
```
POST /api/v1/callback/momo/ipn
{
  ...
  "requestId": "REQ-99999-NONEXISTENT"
  ...
}
```

**Expected Results:**
- Status: 200 OK
- Response contains error message
- No database updates

---

#### TC-011: Duplicate IPN Callback
**Objective:** Handle MoMo retrying the same notification

**Preconditions:**
- Same callback already processed (transaction status = SUCCESS)

**Test Steps:**
```
POST /api/v1/callback/momo/ipn
// Send same request again
```

**Expected Results:**
- Status: 200 OK
- Transaction found, already SUCCESS
- Booking already PAID and COMPLETED
- Loyalty points already awarded (idempotent)
- No duplicate updates

---

### 3. Signature Tests

#### TC-012: Signature Generation
**Objective:** Verify correct HMAC-SHA256 signature generation

**Test Code:**
```java
@Test
public void testSignatureGeneration() {
    String rawData = "accessKey=F8635FE50A0FE6588" +
                     "&amount=250000" +
                     "&extraData=" +
                     "&ipnUrl=http://localhost:8080/api/v1/callback/momo/ipn" +
                     "&lang=vi" +
                     "&orderId=NV-250115-0001-1705123456" +
                     "&orderInfo=Thanh toán đơn rửa xe NV-250115-0001" +
                     "&partnerCode=MOMO12345" +
                     "&redirectUrl=http://localhost:3000/payment-success" +
                     "&requestId=REQ-5-1705123456789" +
                     "&requestType=captureWallet";
    
    String secretKey = "NFcpIQIChnh0szvg6Z9zesZDZURAVESz";
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    
    assertNotNull(signature);
    assertTrue(signature.length() > 0);
    // Base64 encoded, should contain only alphanumeric, +, /, =
    assertTrue(signature.matches("^[A-Za-z0-9+/=]+$"));
}
```

**Expected Results:**
- Signature generated successfully
- Signature is Base64-encoded string
- Same raw data always generates same signature

---

#### TC-013: Signature Verification
**Objective:** Verify HMAC-SHA256 signature verification

**Test Code:**
```java
@Test
public void testSignatureVerification() {
    String rawData = "...";
    String secretKey = "NFcpIQIChnh0szvg6Z9zesZDZURAVESz";
    
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    boolean valid = HmacSHA256Util.verifySignature(rawData, secretKey, signature);
    
    assertTrue(valid);
}

@Test
public void testSignatureVerificationFailsOnWrongSecret() {
    String rawData = "...";
    String secretKey = "NFcpIQIChnh0szvg6Z9zesZDZURAVESz";
    String wrongSecretKey = "WRONG_SECRET_KEY";
    
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    boolean valid = HmacSHA256Util.verifySignature(rawData, wrongSecretKey, signature);
    
    assertFalse(valid);
}

@Test
public void testSignatureVerificationFailsOnTamperedData() {
    String rawData = "amount=250000&...";
    String secretKey = "NFcpIQIChnh0szvg6Z9zesZDZURAVESz";
    
    String signature = HmacSHA256Util.generateSignature(rawData, secretKey);
    
    // Tamper with data
    String tamperedData = "amount=250001&...";  // Changed amount
    boolean valid = HmacSHA256Util.verifySignature(tamperedData, secretKey, signature);
    
    assertFalse(valid);
}
```

**Expected Results:**
- Valid signature verifies successfully
- Invalid secret key fails verification
- Tampered data fails verification

---

### 4. Database Transaction Tests

#### TC-014: Payment Transaction Creation
**Objective:** Verify PaymentTransaction entity persists correctly

**Preconditions:**
- Checkout endpoint called successfully

**SQL Verification:**
```sql
SELECT * FROM payment_transaction 
WHERE momo_request_id = 'REQ-5-1705123456789';

-- Expected columns populated:
-- transaction_id: auto-generated
-- booking_id: 5
-- payment_gateway: "MOMO"
-- momo_request_id: "REQ-5-1705123456789"
-- momo_order_id: "NV-250115-0001-1705123456"
-- amount: 250000.00
-- status: "PROCESSING"
-- request_payload: JSON string
-- response_payload: JSON string
-- created_at: current timestamp
-- updated_at: current timestamp
```

---

#### TC-015: Booking Status Update
**Objective:** Verify Booking entity updates on successful payment

**SQL Verification (after successful IPN):**
```sql
SELECT booking_id, payment_status, status 
FROM booking 
WHERE booking_id = 5;

-- Expected results:
-- payment_status: PAID
-- status: COMPLETED
```

---

#### TC-016: Customer Loyalty Points Update
**Objective:** Verify Customer loyalty points awarded

**SQL Verification (after successful IPN):**
```sql
SELECT customer_id, loyalty_points, total_spending 
FROM customer 
WHERE customer_id = 1;

-- Expected results (assuming previous points = 1000, spending = 750000):
-- loyalty_points: 1375 (1000 + 375)
-- total_spending: 1000000 (750000 + 250000)
```

---

### 5. Integration Tests

#### TC-017: End-to-End Payment Flow
**Objective:** Complete payment workflow from checkout to IPN

**Steps:**
1. Customer calls checkout endpoint
2. MomoPaymentService creates PaymentTransaction
3. Signature calculated and request sent to MoMo
4. MoMo returns payUrl
5. Frontend redirects customer to payUrl
6. Customer completes payment in MoMo UI
7. MoMo sends IPN callback
8. PaymentController receives and validates signature
9. MomoPaymentService processes success callback
10. Booking marked PAID and COMPLETED
11. Loyalty points awarded

**Verification:**
```
✓ PaymentTransaction status: SUCCESS
✓ Booking payment_status: PAID
✓ Booking status: COMPLETED
✓ Customer loyalty_points increased
✓ Customer total_spending increased
```

---

### 6. Negative Test Scenarios

#### TC-018: MoMo Endpoint Timeout
**Objective:** Handle slow MoMo API response

**Setup:** Mock RestTemplate to simulate 15-second delay

**Expected Results:**
- Timeout after 10 seconds (configured timeout)
- Exception caught and wrapped in BadRequestException
- PaymentTransaction status: FAILED
- User sees error message
- Booking remains UNPAID

---

#### TC-019: MoMo Endpoint Error (500)
**Objective:** Handle MoMo server errors

**Setup:** Mock MoMo endpoint to return 500 Internal Server Error

**Expected Results:**
- Exception caught
- PaymentTransaction created with error details
- User sees error message
- Booking remains UNPAID

---

#### TC-020: Network Error
**Objective:** Handle network connectivity issues

**Setup:** Mock RestTemplate to throw HttpClientErrorException

**Expected Results:**
- Exception caught
- PaymentTransaction status: FAILED
- Error details logged
- User sees error message

---

## Test Data

### Sample Test Booking
```json
{
  "bookingId": 5,
  "bookingCode": "NV-250115-0001",
  "customerId": 1,
  "customerName": "Nguyen Van A",
  "licensePlate": "29-H1 55555",
  "totalEstimatedAmount": 250000,
  "discountAmount": 0,
  "finalAmount": 250000,
  "status": "PENDING",
  "paymentStatus": "UNPAID"
}
```

### Sample Test Customer
```json
{
  "customerId": 1,
  "fullName": "Nguyen Van A",
  "phoneNumber": "0987654321",
  "loyaltyPoints": 1000,
  "totalSpending": 750000,
  "tier": {
    "tierId": 2,
    "tierName": "SILVER",
    "tierMultiplier": 1.5
  }
}
```

### Sample JWT Token
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

---

## Performance Test Scenarios

### Load Test
**Objective:** Verify payment endpoint handles concurrent requests

**Scenario:**
- 100 concurrent checkout requests
- Target: Response time < 5 seconds
- Target: No transaction failures

---

### Stress Test
**Objective:** Identify breaking point

**Scenario:**
- Increase concurrent requests from 100 to 1000
- Monitor: Response times, error rates, database connections

---

## Security Test Scenarios

### CSRF Protection
**Objective:** Verify CSRF token requirement

**Preconditions:** Should already be handled by Spring Security

---

### SQL Injection
**Objective:** Verify parameterized queries prevent SQL injection

**Test:**
```
bookingId: 5; DROP TABLE booking; --
// Should be treated as literal string, not execute
```

---

### Signature Tampering Detection
**Objective:** Verify tampered signatures are rejected

Already covered in TC-009 and TC-013

---

## Regression Test Suite

### Critical Paths
1. Customer checkout with valid booking ✓
2. IPN callback with valid signature ✓
3. Booking status updates on payment ✓
4. Loyalty points calculated correctly ✓
5. Payment transaction audit trail ✓

---

## Test Automation

### Using Postman Collection
```
MoMo Payment Integration Tests
├── Authentication
│   └── Get JWT Token
├── Checkout Scenarios
│   ├── TC-001: Successful Checkout
│   ├── TC-002: Invalid Payment Method
│   ├── TC-003: Booking Not Found
│   ├── TC-004: Already Paid
│   ├── TC-005: Unauthorized Access
│   └── TC-006: No Authentication
├── IPN Callbacks
│   ├── TC-007: Successful Payment
│   ├── TC-008: Payment Declined
│   ├── TC-009: Invalid Signature
│   └── TC-010: Transaction Not Found
└── Database Verification
    ├── Verify PaymentTransaction
    ├── Verify Booking Updates
    └── Verify Customer Rewards
```

---

## Test Execution Checklist

- [ ] Unit tests pass (HmacSHA256Util)
- [ ] Integration tests pass (Controllers, Services)
- [ ] Database tests pass (PaymentTransaction CRUD)
- [ ] IPN callback tests pass
- [ ] Signature verification tests pass
- [ ] Error handling tests pass
- [ ] Security tests pass
- [ ] Performance under expected load
- [ ] Manual end-to-end flow works
- [ ] Logs are detailed and helpful
- [ ] Documentation is complete
- [ ] Code review approved

---

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Test Documentation](https://spring.io/guides/gs/testing-web/)
- [TestContainers for PostgreSQL](https://www.testcontainers.org/)
- [Postman Testing Guide](https://learning.postman.com/docs/testing-your-api/introduction-to-testing-in-postman/)

---

**Version:** 1.0
**Last Updated:** 2025-01-15
**Status:** Complete
