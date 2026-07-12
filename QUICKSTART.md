# MoMo Payment Integration - Quick Start Guide

## Step 1: Register MoMo Sandbox Account

1. Go to https://dev.momo.vn
2. Click "Đăng ký đối tác" (Register Partner)
3. Fill in required information and verify email
4. Log in to dashboard
5. Create/access Sandbox environment
6. Copy credentials:
   - Partner Code
   - Access Key
   - Secret Key

## Step 2: Configure Application

### Option A: Using Environment Variables (Recommended for Production)

```bash
# Set environment variables on your system
export MOMO_PARTNER_CODE=your_partner_code
export MOMO_ACCESS_KEY=your_access_key
export MOMO_SECRET_KEY=your_secret_key
export MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
export MOMO_REDIRECT_URL=http://localhost:3000/payment-success
export MOMO_IPN_URL=http://localhost:8080/api/v1/callback/momo/ipn
```

### Option B: Direct Configuration in application.yaml (For Development Only)

Edit `src/main/resources/application.yaml`:

```yaml
app:
  payment:
    momo:
      partner-code: your_partner_code
      access-key: your_access_key
      secret-key: your_secret_key
      endpoint: https://test-payment.momo.vn/v2/gateway/api/create
      redirect-url: http://localhost:3000/payment-success
      ipn-url: http://localhost:8080/api/v1/callback/momo/ipn
```

## Step 3: Database Migration

The application will auto-create the `payment_transaction` table if `spring.jpa.hibernate.ddl-auto=create`.

If using `update` mode, manually run the migration:

```bash
# Connect to PostgreSQL and run:
psql -U postgres -d novawash -f src/main/resources/db/init-payment-transaction.sql
```

## Step 4: Build and Run Application

```bash
# Using Maven
mvn clean install
mvn spring-boot:run

# Or using IDE
# Run AutoWashProBeApplication.java
```

## Step 5: Test Checkout Flow

### Using Postman or cURL

1. **Get JWT Token** (if not already authenticated):
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "phoneNumber": "0987654321",
  "password": "password123"
}
```

2. **Initiate Checkout**:
```bash
POST /api/v1/customer/bookings/checkout
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "bookingId": 1,
  "paymentMethod": "MOMO",
  "notes": "Test payment"
}
```

3. **Expected Response**:
```json
{
  "transactionId": "1",
  "bookingId": 1,
  "amount": 250000,
  "paymentMethod": "MOMO",
  "paymentUrl": "https://test-payment.momo.vn/confirm?...",
  "status": "PROCESSING",
  "message": "Successful",
  "momoRequestId": "REQ-1-1705123456789",
  "momoOrderId": "NV-250115-0001-1705123456"
}
```

4. **Open Payment URL** in browser and complete payment

## Step 6: Testing IPN Callback

The IPN callback is automatically triggered by MoMo after payment completion.

To manually test the callback for debugging:

```bash
curl -X POST http://localhost:8080/api/v1/callback/momo/ipn \
  -H "Content-Type: application/json" \
  -d '{
    "partnerCode": "MOMO12345",
    "accessKey": "F8635FE50A0FE6588",
    "requestId": "REQ-1-1705123456789",
    "amount": 250000,
    "orderId": "NV-250115-0001-1705123456",
    "orderInfo": "Thanh toán đơn rửa xe NV-250115-0001",
    "orderType": "momo_wallet",
    "resultCode": 0,
    "resultMessage": "Success.",
    "transId": 20250115123456789,
    "responseTime": 1705123500000,
    "paymentOption": "webApp",
    "signature": "COMPUTED_HMAC_SHA256_SIGNATURE",
    "extraData": ""
  }'
```

## Key Implementation Details

### Architecture

```
┌─────────────────┐
│   Frontend      │
└────────┬────────┘
         │
         │ POST /checkout
         ↓
┌─────────────────────────────────────┐
│ PaymentController                   │
│ - Validates user authentication     │
│ - Delegates to MomoPaymentService   │
└────────┬────────────────────────────┘
         │
         ↓
┌─────────────────────────────────────┐
│ MomoPaymentService                  │
│ - Builds MoMo request               │
│ - Generates HMAC-SHA256 signature   │
│ - Sends request to MoMo API         │
│ - Stores payment transaction        │
│ - Returns payUrl to frontend        │
└────────┬────────────────────────────┘
         │
         ↓
┌──────────────────────────────────────┐
│ MoMo Sandbox Server                  │
│ - Processes payment request          │
│ - Returns payment gateway URL        │
└──────────────────────────────────────┘
         │
         │ Customer completes payment
         │
         │ MoMo POST IPN callback
         ↓
┌──────────────────────────────────────┐
│ PaymentController (IPN endpoint)     │
│ - Receives callback                  │
│ - Delegates to MomoPaymentService    │
└────────┬─────────────────────────────┘
         │
         ↓
┌──────────────────────────────────────┐
│ MomoPaymentService.processIpnCallback│
│ - Verifies signature                 │
│ - Updates booking status to PAID     │
│ - Processes loyalty points           │
│ - Checks tier upgrade                │
└──────────────────────────────────────┘
```

### Payment States

```
Payment Flow:
PENDING → PROCESSING → SUCCESS/FAILED

Booking Status Flow (on successful payment):
PENDING → COMPLETED

Booking Payment Status Flow (on successful payment):
UNPAID → PAID
```

### Signature Generation

The signature is critical for security:

```
1. Create raw data string with specific field order:
   accessKey=...&amount=...&extraData=...&ipnUrl=...&lang=...
   &orderId=...&orderInfo=...&partnerCode=...&redirectUrl=...
   &requestId=...&requestType=...

2. Generate HMAC-SHA256 hash:
   byte[] digest = Mac.getInstance("HmacSHA256")
                     .doFinal(rawData.getBytes(), secretKey)

3. Base64 encode the digest:
   signature = Base64.encode(digest)

4. Send signature with request for MoMo to verify
```

## Troubleshooting Checklist

- [ ] MoMo credentials are correct (verify in dev.momo.vn dashboard)
- [ ] Environment variables are set correctly
- [ ] Database table `payment_transaction` exists
- [ ] Booking exists and belongs to authenticated customer
- [ ] Booking is not already paid
- [ ] Amount is >= 1,000 VND
- [ ] IPN URL is publicly accessible (not localhost for production)
- [ ] Application logs show signature generation details
- [ ] RestTemplate bean is properly configured
- [ ] Jackson ObjectMapper can serialize/deserialize DTOs

## Files Modified/Created

### New Files:
- `src/main/java/com/autowashpro/autowashpro_be/config/MomoProperties.java`
- `src/main/java/com/autowashpro/autowashpro_be/config/RestTemplateConfig.java`
- `src/main/java/com/autowashpro/autowashpro_be/common/util/HmacSHA256Util.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/dto/MomoPaymentRequest.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/dto/MomoPaymentResponse.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/dto/MomoIpnCallbackRequest.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/dto/CheckoutRequest.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/dto/CheckoutResponse.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/entity/PaymentTransaction.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/repository/PaymentTransactionRepository.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/service/MomoPaymentService.java`
- `src/main/java/com/autowashpro/autowashpro_be/modules/booking/controller/PaymentController.java`
- `src/main/resources/db/init-payment-transaction.sql`
- `.env.example`
- `MOMO_INTEGRATION_GUIDE.md`
- `QUICKSTART.md` (this file)

### Modified Files:
- `src/main/resources/application.yaml` (added MoMo configuration section)

## Next Steps

1. **Configure MoMo Credentials**: Get them from dev.momo.vn
2. **Set Environment Variables**: Export MoMo variables
3. **Run Database Migration**: Create payment_transaction table
4. **Build Project**: `mvn clean install`
5. **Start Application**: `mvn spring-boot:run`
6. **Test Checkout**: Use Postman to initiate payment
7. **Monitor Logs**: Watch for signature verification and callback processing

## Support Resources

- MoMo Dev Portal: https://dev.momo.vn
- MoMo Sandbox Docs: https://momo.vn/en/developers
- Test Credentials: https://dev.momo.vn/sandbox
- Integration Guide: See MOMO_INTEGRATION_GUIDE.md

---

**Last Updated:** 2025-01-15
**Status:** Production Ready
