# MoMo Payment Gateway Integration - Configuration Guide

## Overview
This document provides comprehensive instructions for configuring and using the MoMo Sandbox payment gateway integration in the AutoWashPro Backend.

## 1. Sandbox Credentials Setup

Before starting, you need to obtain MoMo Sandbox credentials from [MoMo Developer Portal](https://dev.momo.vn).

### Steps to Get Sandbox Credentials:
1. Visit https://dev.momo.vn/dashboard
2. Register a new partner account or log in
3. Create a Sandbox environment
4. Note the following credentials:
   - `Partner Code` (Mã đối tác)
   - `Access Key` (Khóa truy cập)
   - `Secret Key` (Khóa bí mật)

## 2. Environment Configuration

### 2.1 Development Environment (.env or system variables)

Set these environment variables on your local machine:

```bash
# MoMo Sandbox Configuration
export MOMO_PARTNER_CODE=MOMO12345
export MOMO_ACCESS_KEY=F8635FE50A0FE6588
export MOMO_SECRET_KEY=NFcpIQIChnh0szvg6Z9zesZDZURAVESz
export MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
export MOMO_REDIRECT_URL=http://localhost:3000/payment-success
export MOMO_IPN_URL=http://localhost:8080/api/v1/callback/momo/ipn
```

### 2.2 Production Environment

For production deployment, ensure:
- Update credentials with production partner code and keys
- Set `MOMO_ENDPOINT` to production URL
- Use proper domain for `MOMO_REDIRECT_URL` (your frontend domain)
- Use proper domain for `MOMO_IPN_URL` (your backend domain)

Example production config:
```yaml
MOMO_PARTNER_CODE: YOUR_PRODUCTION_PARTNER_CODE
MOMO_ACCESS_KEY: YOUR_PRODUCTION_ACCESS_KEY
MOMO_SECRET_KEY: YOUR_PRODUCTION_SECRET_KEY
MOMO_ENDPOINT: https://payment.momo.vn/v2/gateway/api/create
MOMO_REDIRECT_URL: https://autowashpro.com/payment-success
MOMO_IPN_URL: https://api.autowashpro.com/api/v1/callback/momo/ipn
```

## 3. Architecture Overview

### Payment Flow:
```
Frontend (Customer) 
    ↓
POST /api/v1/customer/bookings/checkout (with MOMO payment method)
    ↓
PaymentController → MomoPaymentService
    ↓
Create MomoPaymentRequest (with signature)
    ↓
Send POST to MoMo Sandbox Endpoint
    ↓
MoMo returns payUrl
    ↓
Frontend redirects customer to payUrl
    ↓
Customer completes payment on MoMo platform
    ↓
MoMo POST IPN callback to: /api/v1/callback/momo/ipn
    ↓
Verify signature → Update Booking → Process Loyalty Points
    ↓
Respond with statusCode 0 (success) to MoMo
```

## 4. API Endpoints

### 4.1 Checkout Endpoint
**URL:** `POST /api/v1/customer/bookings/checkout`

**Authentication:** Required (Customer role)

**Request Body:**
```json
{
  "bookingId": 123,
  "paymentMethod": "MOMO",
  "notes": "Optional payment notes"
}
```

**Response (Success):**
```json
{
  "transactionId": "1001",
  "bookingId": 123,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/confirm?...",
  "status": "PROCESSING",
  "message": "Successful",
  "momoRequestId": "REQ-123-1705123456789",
  "momoOrderId": "NV-250115-0001-1705123456"
}
```

**Response (Error):**
```json
{
  "statusCode": 400,
  "message": "Payment processing failed: Booking already paid",
  "data": null
}
```

### 4.2 IPN Callback Endpoint
**URL:** `POST /api/v1/callback/momo/ipn`

**Authentication:** NOT required (called by MoMo servers)

**Callback Request (from MoMo):**
```json
{
  "partnerCode": "MOMO12345",
  "accessKey": "F8635FE50A0FE6588",
  "requestId": "REQ-123-1705123456789",
  "amount": 250000,
  "orderId": "NV-250115-0001-1705123456",
  "orderInfo": "Thanh toán đơn rửa xe NV-250115-0001",
  "orderType": "momo_wallet",
  "resultCode": 0,
  "resultMessage": "Success.",
  "transId": 20250115123456789,
  "responseTime": 1705123500000,
  "paymentOption": "webApp",
  "signature": "Base64EncodedHmacSHA256Signature",
  "extraData": ""
}
```

**Callback Response (to MoMo):**
- **Success:** `{"statusCode": 0, "message": "Success"}`
- **Failure:** `{"statusCode": 1, "message": "Error processing callback: ..."}`

## 5. Security Features

### 5.1 Signature Generation
All requests to MoMo are signed using HMAC-SHA256:

```
Raw Data = "accessKey=...&amount=...&extraData=...&ipnUrl=...&lang=...&orderId=...&orderInfo=...&partnerCode=...&redirectUrl=...&requestId=...&requestType=..."
Signature = Base64(HMAC-SHA256(Raw Data, Secret Key))
```

### 5.2 Signature Verification
IPN callbacks are verified using the same HMAC-SHA256 process to ensure authenticity.

### 5.3 Implementation Details
- **Utility Class:** `HmacSHA256Util`
- **Methods:** 
  - `generateSignature(message, secretKey)` - Generate signature
  - `verifySignature(message, secretKey, providedSignature)` - Verify signature

## 6. Database Schema

### PaymentTransaction Table
```sql
CREATE TABLE payment_transaction (
  transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL REFERENCES booking(booking_id),
  payment_gateway VARCHAR(50) NOT NULL,
  momo_trans_id VARCHAR(100),
  momo_request_id VARCHAR(100),
  momo_order_id VARCHAR(100),
  amount DECIMAL(12, 2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  result_code INT,
  result_message TEXT,
  request_payload TEXT,
  response_payload TEXT,
  callback_payload TEXT,
  error_details TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

## 7. Loyalty Points Processing

After successful payment:
1. Read `resultCode` from IPN callback
2. If `resultCode == 0` (Success):
   - Update Booking: `paymentStatus = PAID`, `status = COMPLETED`
   - Calculate points: `(amount / 1000) * tier_multiplier`
   - Update Customer: `loyaltyPoints += pointsEarned`, `totalSpending += amount`
   - Check tier upgrade eligibility

## 8. Testing Guide

### 8.1 Using MoMo Sandbox Simulator
1. Start payment checkout flow
2. Click "Pay" button to go to MoMo sandbox
3. MoMo provides test credit cards and phone numbers
4. Complete payment in sandbox environment
5. MoMo will POST IPN callback to your `ipnUrl`

### 8.2 Testing Without UI
Use cURL or Postman to test checkout endpoint:

```bash
curl -X POST http://localhost:8080/api/v1/customer/bookings/checkout \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 123,
    "paymentMethod": "MOMO",
    "notes": "Test payment"
  }'
```

### 8.3 Simulating IPN Callback
```bash
curl -X POST http://localhost:8080/api/v1/callback/momo/ipn \
  -H "Content-Type: application/json" \
  -d '{
    "partnerCode": "MOMO12345",
    "accessKey": "F8635FE50A0FE6588",
    "requestId": "REQ-123-1705123456789",
    "amount": 250000,
    "orderId": "NV-250115-0001-1705123456",
    "orderInfo": "Thanh toán đơn rửa xe NV-250115-0001",
    "resultCode": 0,
    "resultMessage": "Success.",
    "transId": 20250115123456789,
    "responseTime": 1705123500000,
    "signature": "YOUR_HMAC_SHA256_SIGNATURE"
  }'
```

## 9. Troubleshooting

### Issue: "Invalid signature"
**Solution:** 
- Verify that `Secret Key` is correct in `application.yaml`
- Check that the raw data string order is exact
- Ensure no extra spaces or special characters

### Issue: "MoMo payment failed - Error code 1001"
**Solution:**
- This means transaction was declined by customer
- Check MoMo sandbox balance or payment method
- Verify amount is >= 1,000 VND

### Issue: "IPN callback not received"
**Solution:**
- Ensure `ipnUrl` is publicly accessible (not localhost)
- Check firewall rules allowing MoMo servers to POST to your IP
- Verify URL is reachable: `curl https://your-domain/api/v1/callback/momo/ipn`
- Check application logs for callback processing errors

### Issue: "Booking payment not updated"
**Solution:**
- Verify IPN callback signature is valid
- Check booking exists and belongs to correct customer
- Review application logs for detailed error messages
- Check payment_transaction table for callback records

## 10. Key Classes and Files

| File | Purpose |
|------|---------|
| `MomoProperties.java` | Configuration properties binding |
| `HmacSHA256Util.java` | Signature generation/verification utility |
| `MomoPaymentRequest.java` | Request DTO to MoMo |
| `MomoPaymentResponse.java` | Response DTO from MoMo |
| `MomoIpnCallbackRequest.java` | IPN callback DTO |
| `CheckoutRequest.java` | Customer checkout request DTO |
| `CheckoutResponse.java` | Checkout response DTO |
| `PaymentTransaction.java` | JPA entity for payment tracking |
| `PaymentTransactionRepository.java` | Database repository |
| `MomoPaymentService.java` | Core payment service logic |
| `PaymentController.java` | REST endpoints |
| `RestTemplateConfig.java` | HTTP client configuration |

## 11. References

- [MoMo Developer Portal](https://dev.momo.vn)
- [MoMo API Documentation](https://momo.vn/en/developers)
- [MoMo Sandbox Test Credentials](https://dev.momo.vn/sandbox)

## 12. Support

For issues or questions:
1. Check application logs in `target/logs/` or console output
2. Review this configuration guide
3. Verify credentials in [MoMo Developer Portal](https://dev.momo.vn)
4. Contact MoMo support for gateway-level issues
