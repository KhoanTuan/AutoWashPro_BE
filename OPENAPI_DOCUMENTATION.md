# MoMo Payment Integration - OpenAPI/Swagger Documentation

## Swagger Configuration

The following endpoints are documented and available through Swagger UI:

**Access Swagger UI:** `http://localhost:8080/swagger-ui.html`

**Access OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

---

## API Endpoints

### 1. Checkout Endpoint

#### POST /api/v1/customer/bookings/checkout

**Summary:** Initiate MoMo payment for a booking

**Description:** 
Creates a payment transaction and generates a MoMo payment gateway URL where the customer can complete the payment. Returns the payment URL which should be opened in a browser.

**Security:** 
- Requires JWT authentication with CUSTOMER role
- Validates booking ownership
- Validates payment method

**Tags:** Payment, Booking

**Request Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "bookingId": 5,
  "paymentMethod": "MOMO",
  "notes": "Optional notes"
}
```

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| bookingId | Long | Yes | ID of the booking to pay for |
| paymentMethod | String | Yes | Payment method (currently only "MOMO" supported) |
| notes | String | No | Optional customer notes |

**Response (Success - 200 OK):**
```json
{
  "transactionId": "1001",
  "bookingId": 5,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/confirm?requestId=REQ-5-1705123456789&accessKey=...",
  "status": "PROCESSING",
  "message": "Successful",
  "momoRequestId": "REQ-5-1705123456789",
  "momoOrderId": "NV-250115-0001-1705123456"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| transactionId | String | Unique transaction reference in our system |
| bookingId | Long | The booking being paid for |
| amount | BigDecimal | Payment amount in Vietnamese Dong (VND) |
| paymentMethod | String | Payment method used (MOMO) |
| paymentUrl | String | **IMPORTANT:** Direct customer to this URL to complete payment |
| status | String | Payment transaction status (PENDING, PROCESSING, SUCCESS, FAILED) |
| message | String | Response message describing the result |
| momoRequestId | String | MoMo request tracking ID |
| momoOrderId | String | MoMo order tracking ID |

**Response (Error - 400 Bad Request):**
```json
{
  "statusCode": 400,
  "message": "Payment processing failed: Booking already paid",
  "data": null
}
```

**Response (Error - 401 Unauthorized):**
```json
{
  "statusCode": 401,
  "message": "Unauthorized"
}
```

**Response (Error - 404 Not Found):**
```json
{
  "statusCode": 404,
  "message": "Booking not found with id: 5"
}
```

**HTTP Status Codes:**

| Code | Meaning |
|------|---------|
| 200 | Successful checkout initiation |
| 400 | Invalid request (validation error) |
| 401 | Not authenticated or invalid token |
| 403 | Forbidden (insufficient permissions) |
| 404 | Booking not found |
| 500 | Server error |

**Example cURL Request:**
```bash
curl -X POST http://localhost:8080/api/v1/customer/bookings/checkout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 5,
    "paymentMethod": "MOMO",
    "notes": "Test payment"
  }'
```

**Example JavaScript/Fetch Request:**
```javascript
const response = await fetch('/api/v1/customer/bookings/checkout', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    bookingId: 5,
    paymentMethod: 'MOMO',
    notes: 'Optional notes'
  })
});

const data = await response.json();

if (response.ok) {
  // Redirect to payment gateway
  window.location.href = data.paymentUrl;
} else {
  console.error('Payment error:', data.message);
}
```

**Frontend Implementation Flow:**
```
1. User clicks "Pay with MoMo" button
2. Call POST /api/v1/customer/bookings/checkout
3. Receive paymentUrl in response
4. Redirect to paymentUrl: window.location.href = paymentUrl
5. Customer completes payment in MoMo interface
6. MoMo redirects back to MOMO_REDIRECT_URL
7. (Optional) Poll booking status to verify payment
```

---

### 2. IPN Callback Endpoint

#### POST /api/v1/callback/momo/ipn

**Summary:** MoMo Instant Payment Notification webhook

**Description:** 
Webhook endpoint called by MoMo servers to notify about payment completion. This is NOT called directly by frontend; it's called by MoMo backend servers. Automatically processes successful payments and updates booking state.

**Security:** 
- No authentication required (called by MoMo)
- Signature verification using HMAC-SHA256
- Rejects tampered or invalid requests

**Tags:** Payment, Webhook

**Request Headers:**
```
Content-Type: application/json
(No Authorization required - MoMo calls this endpoint)
```

**Request Body (From MoMo):**
```json
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
  "signature": "gO1qBi46UXyM1bQjTXaWIvV...",
  "extraData": ""
}
```

**Request Fields:**

| Field | Type | Description |
|-------|------|-------------|
| partnerCode | String | MoMo partner code |
| accessKey | String | MoMo access key |
| requestId | String | Matches original checkout request ID |
| amount | Long | Amount in VND |
| orderId | String | Matches original checkout order ID |
| orderInfo | String | Order description |
| orderType | String | Type of order (momo_wallet) |
| resultCode | Integer | **0 = Success**, others = failure |
| resultMessage | String | Result description |
| transId | Long | MoMo transaction reference ID |
| responseTime | Long | Unix timestamp in milliseconds |
| paymentOption | String | Payment method used |
| signature | String | HMAC-SHA256 signature for verification |
| extraData | String | Additional data (if provided in request) |

**Response (Success - 200 OK):**
```json
{
  "statusCode": 0,
  "message": "Success",
  "data": {
    "requestId": "REQ-5-1705123456789",
    "orderId": "NV-250115-0001-1705123456",
    "transId": 20250115123456789
  }
}
```

**Response (Failure - 200 OK with error status):**
```json
{
  "statusCode": 1,
  "message": "Invalid signature"
}
```

**Response Status Codes:**

| statusCode | Meaning |
|-----------|---------|
| 0 | Successfully processed |
| 1 | Error (invalid signature, transaction not found, etc.) |

**Processing Logic (by Backend):**
```
1. Receive IPN callback from MoMo
2. Verify signature using HMAC-SHA256
3. If signature invalid:
   → Return statusCode 1, message "Invalid signature"
4. If signature valid:
   a. Find PaymentTransaction by requestId
   b. Check resultCode:
      - If 0 (Success):
        ✓ Update PaymentTransaction.status = SUCCESS
        ✓ Update Booking.paymentStatus = PAID
        ✓ Update Booking.status = COMPLETED
        ✓ Calculate and award loyalty points:
          points = (amount / 1000) * tier_multiplier
        ✓ Update Customer.loyaltyPoints += points
        ✓ Update Customer.totalSpending += amount
      - If != 0 (Failure):
        ✓ Update PaymentTransaction.status = FAILED
        ✓ Do NOT update booking
        ✓ Do NOT award points
   c. Return statusCode 0, message "Success"
```

**Important Notes:**
- **Always return 200 OK**, even on processing errors
- **Webhook is idempotent:** Processing same callback twice is safe
- **MoMo will retry** if we don't return 200 OK
- **No customer authentication** required (webhook from MoMo servers)
- **Signature verification is critical** for security

**Result Code Reference:**

| resultCode | Meaning |
|-----------|---------|
| 0 | Transaction successful |
| 1001 | Transaction denied/cancelled by user |
| 1002 | Transaction denied (insufficient balance) |
| 1003 | Transaction timeout |
| 1004 | Service unavailable |
| 1111 | Maintenance |

---
### 3. Admin Retention Simulation

#### POST /api/v1/admin/loyalty/run-simulation

**Summary:** Run a one-click loyalty retention simulation and report the retention impact.

**Description:**
This admin endpoint executes the retention simulation job. It expires loyalty points for long-inactive customers, downgrades VIP customers who have not booked in 6+ months to `REGULAR`, and marks customers as `INACTIVE` after 12+ months of inactivity.

**Security:**
- Requires JWT authentication
- Requires permissions: `MANAGE_LOYALTY_CONFIG`, `ROLE_ADMIN`, or `ROLE_MANAGER`

**Tags:** Admin Loyalty, Retention

**Request Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body:**
- None

**Response (Success - 200 OK):**
```json
{
  "status": "success",
  "expiredPoints": 1200,
  "downgradedUsers": 8,
  "deactivatedUsers": 5
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| status | String | Result status, typically `success` |
| expiredPoints | Integer | Total loyalty points expired by the simulation |
| downgradedUsers | Integer | Number of VIP users downgraded to REGULAR |
| deactivatedUsers | Integer | Number of users marked as INACTIVE |

**Response (Error - 401 Unauthorized):**
```json
{
  "statusCode": 401,
  "message": "Unauthorized"
}
```

**Response (Error - 403 Forbidden):**
```json
{
  "statusCode": 403,
  "message": "Forbidden"
}
```

**Response (Error - 500 Internal Server Error):**
```json
{
  "statusCode": 500,
  "message": "Retention simulation failed"
}
```

**HTTP Status Codes:**

| Code | Meaning |
|------|---------|
| 200 | Simulation completed successfully |
| 401 | Not authenticated |
| 403 | Authenticated but not authorized |
| 500 | Server error during simulation |

---
## Data Models

### CheckoutRequest

```json
{
  "bookingId": 5,
  "paymentMethod": "MOMO",
  "notes": "Optional notes"
}
```

### CheckoutResponse

```json
{
  "transactionId": "1001",
  "bookingId": 5,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/confirm?...",
  "status": "PROCESSING",
  "message": "Successful",
  "momoRequestId": "REQ-5-1705123456789",
  "momoOrderId": "NV-250115-0001-1705123456"
}
```

### MomoIpnCallbackRequest

```json
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
  "signature": "gO1qBi46UXyM1bQjTXaWIvV...",
  "extraData": ""
}
```

---

## Error Responses

### Common Error Responses

#### 400 Bad Request - Invalid Payment Method
```json
{
  "statusCode": 400,
  "message": "Hiện tại chỉ hỗ trợ thanh toán qua MoMo",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

#### 400 Bad Request - Booking Already Paid
```json
{
  "statusCode": 400,
  "message": "Booking already paid",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

#### 400 Bad Request - Unauthorized Access
```json
{
  "statusCode": 400,
  "message": "Unauthorized: Booking does not belong to this customer",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

#### 401 Unauthorized - Not Authenticated
```json
{
  "statusCode": 401,
  "message": "Vui lòng đăng nhập để thực hiện thanh toán!",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

#### 404 Not Found - Booking Not Found
```json
{
  "statusCode": 404,
  "message": "Booking not found with id: 5",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

#### 500 Internal Server Error - MoMo API Error
```json
{
  "statusCode": 500,
  "message": "Payment processing failed: Failed to communicate with payment gateway",
  "data": null,
  "timestamp": "2025-01-15T10:30:45.123Z"
}
```

---

## Authentication

### Getting JWT Token

Before calling checkout endpoint, get JWT token:

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "phoneNumber": "0987654321",
  "password": "password123"
}
```

**Response:**
```json
{
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "customer": {
      "customerId": 1,
      "fullName": "Nguyen Van A",
      "phoneNumber": "0987654321",
      "loyaltyPoints": 1000
    }
  }
}
```

### Using JWT Token

Include in all authenticated requests:
```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Rate Limiting

To prevent abuse:
- **Checkout endpoint:** Max 10 requests per minute per user
- **IPN callback:** No rate limit (called by MoMo)

---

## Best Practices

### For Frontend Developers

1. **Always redirect to paymentUrl:**
   ```javascript
   if (checkoutResponse.paymentUrl) {
       window.location.href = checkoutResponse.paymentUrl;
   }
   ```

2. **Handle timeout gracefully:**
   ```javascript
   const timeoutId = setTimeout(() => {
       alert('Payment gateway is taking longer than expected');
   }, 30000); // 30 seconds
   ```

3. **Store transaction ID for reference:**
   ```javascript
   localStorage.setItem('lastTransactionId', response.transactionId);
   ```

4. **Poll booking status after return:**
   ```javascript
   // After customer returns from MoMo
   setInterval(async () => {
       const booking = await fetch(`/api/v1/customer/bookings/${bookingId}`);
       if (booking.paymentStatus === 'PAID') {
           // Payment confirmed
       }
   }, 2000);
   ```

### For Backend Developers

1. **Always verify IPN signatures:**
   ```java
   if (!verifySignature(callback)) {
       return Map.of("statusCode", 1, "message", "Invalid");
   }
   ```

2. **Make IPN processing idempotent:**
   ```java
   if (transaction.getStatus().equals("SUCCESS")) {
       // Already processed, skip
       return success;
   }
   ```

3. **Log all transactions for audit:**
   ```java
   transaction.setRequestPayload(requestJson);
   transaction.setResponsePayload(responseJson);
   transaction.setCallbackPayload(callbackJson);
   ```

4. **Use @Transactional for atomicity:**
   ```java
   @Transactional
   public void processIpnCallback(MomoIpnCallbackRequest callback) {
       // All DB updates are atomic
   }
   ```

---

## Troubleshooting

### Issue: paymentUrl is null
**Possible Causes:**
- MoMo API returned error (resultCode != 0)
- Network error communicating with MoMo

**Solution:**
- Check `status` and `message` fields in response
- Check application logs for detailed error
- Verify MoMo credentials are correct

### Issue: IPN callback not received
**Possible Causes:**
- ipnUrl not publicly accessible
- Firewall blocking MoMo servers
- Network timeout

**Solution:**
- Use ngrok to expose localhost for development
- Whitelist MoMo server IPs
- Check application logs for connection attempts
- Increase timeout if production is slow

### Issue: Signature verification fails
**Possible Causes:**
- Secret key incorrect
- Raw data order wrong
- Data tampering

**Solution:**
- Verify MOMO_SECRET_KEY is correct
- Check raw data construction order
- Use signature generation utility (don't build manually)

---

## Testing Guide

### Using Postman

1. **Create Collection:** "MoMo Payment"

2. **Add Pre-request Script** (for all requests):
```javascript
const token = pm.environment.get('auth_token');
if (token) {
    pm.request.headers.add({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}
```

3. **Add Test Scripts** (for verification):
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has paymentUrl", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.paymentUrl).to.be.a('string');
});
```

### Using cURL
```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"0987654321","password":"pass123"}' \
  | jq -r '.data.accessToken')

# 2. Checkout
curl -X POST http://localhost:8080/api/v1/customer/bookings/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookingId":5,"paymentMethod":"MOMO"}'
```

---

## Support & Documentation

- **Full Integration Guide:** See `MOMO_INTEGRATION_GUIDE.md`
- **Quick Start:** See `QUICKSTART.md`
- **Implementation Details:** See `IMPLEMENTATION_NOTES.md`
- **Test Cases:** See `TEST_CASES.md`
- **MoMo Dev Portal:** https://dev.momo.vn

---

**Version:** 1.0
**Last Updated:** 2025-01-15
**Status:** Complete and Ready for Integration
