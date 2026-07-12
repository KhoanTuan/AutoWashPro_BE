package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.util.HmacSHA256Util;
import com.autowashpro.autowashpro_be.config.MomoProperties;
import com.autowashpro.autowashpro_be.modules.booking.dto.*;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.PaymentTransactionRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service layer for MoMo payment gateway integration.
 * Handles payment request creation, signature generation, and IPN callback processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private final MomoProperties momoProperties;
    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Initiates MoMo payment checkout for a booking.
     * 
     * @param bookingId Booking ID to pay for
     * @param customerId Customer ID initiating the payment
     * @return CheckoutResponse containing MoMo payment gateway URL
     */
    @Transactional
    public CheckoutResponse checkoutWithMoMo(Long bookingId, Long customerId) {
        // Fetch and validate booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Unauthorized: Booking does not belong to this customer");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }

        // Generate unique identifiers
        String requestId = generateRequestId(booking.getBookingId());
        String orderId = generateOrderId(booking);

        try {
            // Create MoMo payment request
            MomoPaymentRequest request = buildMomoPaymentRequest(booking, requestId, orderId);

            // Store transaction record with PENDING status
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .booking(booking)
                    .paymentGateway("MOMO")
                    .momoRequestId(requestId)
                    .momoOrderId(orderId)
                    .amount(booking.getFinalAmount())
                    .status("PENDING")
                    .requestPayload(objectMapper.writeValueAsString(request))
                    .build();
            paymentTransactionRepository.save(transaction);

            // Send request to MoMo endpoint
            MomoPaymentResponse momoResponse = sendPaymentRequest(request);

            // Update transaction with MoMo response
            transaction.setMomoTransId(momoResponse.getTransId() != null ? momoResponse.getTransId().toString() : null);
            transaction.setResultCode(momoResponse.getResultCode());
            transaction.setResultMessage(momoResponse.getResultMessage());
            transaction.setResponsePayload(objectMapper.writeValueAsString(momoResponse));

            if (momoResponse.getResultCode() == 0) {
                transaction.setStatus("PROCESSING");
                log.info("MoMo payment initiated successfully for booking: {} with requestId: {}", bookingId, requestId);
            } else {
                transaction.setStatus("FAILED");
                transaction.setErrorDetails("MoMo returned result code: " + momoResponse.getResultCode() + 
                        ", message: " + momoResponse.getResultMessage());
                log.warn("MoMo payment failed for booking: {} - {}", bookingId, momoResponse.getResultMessage());
            }
            paymentTransactionRepository.save(transaction);

            // Build response
            return CheckoutResponse.builder()
                    .transactionId(String.valueOf(transaction.getTransactionId()))
                    .bookingId(bookingId)
                    .amount(booking.getFinalAmount())
                    .paymentMethod("MOMO")
                    .paymentUrl(momoResponse.getPayUrl())
                    .status(transaction.getStatus())
                    .message(momoResponse.getResultMessage())
                    .momoRequestId(requestId)
                    .momoOrderId(orderId)
                    .build();

        } catch (Exception e) {
            log.error("Error processing MoMo checkout for booking: {}", bookingId, e);
            throw new BadRequestException("Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Processes MoMo IPN (Instant Payment Notification) callback from MoMo servers.
     * Verifies signature and updates booking/customer state based on payment result.
     * 
     * @param callback IPN callback data from MoMo
     * @return Callback response status
     */
    @Transactional
    public Map<String, Object> processIpnCallback(MomoIpnCallbackRequest callback) {
        String requestId = callback.getRequestId();
        log.info("Processing MoMo IPN callback for requestId: {}", requestId);

        try {
            // Verify callback signature
            if (!verifyCallbackSignature(callback)) {
                log.warn("Invalid MoMo callback signature for requestId: {}", requestId);
                return Map.of("statusCode", 1, "message", "Invalid signature");
            }

            // Find transaction by request ID
            PaymentTransaction transaction = paymentTransactionRepository.findByMomoRequestId(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for requestId: " + requestId));

            // Store callback payload
            try {
                transaction.setCallbackPayload(objectMapper.writeValueAsString(callback));
            } catch (Exception e) {
                log.warn("Could not serialize callback payload: {}", e.getMessage());
            }

            // Process based on result code
            if (callback.getResultCode() == 0) {
                // Payment successful
                processSuccessfulPayment(transaction, callback);
            } else {
                // Payment failed or cancelled
                processFailedPayment(transaction, callback);
            }

            paymentTransactionRepository.save(transaction);

            return Map.of(
                    "statusCode", 0,
                    "message", "Success",
                    "data", Map.of(
                            "requestId", requestId,
                            "orderId", callback.getOrderId(),
                            "transId", callback.getTransId()
                    )
            );

        } catch (Exception e) {
            log.error("Error processing MoMo IPN callback for requestId: {}", requestId, e);
            return Map.of("statusCode", 1, "message", "Error processing callback: " + e.getMessage());
        }
    }

    /**
     * Handles successful payment: updates booking payment status, loyalty points, and tier.
     */
    private void processSuccessfulPayment(PaymentTransaction transaction, MomoIpnCallbackRequest callback) {
        Booking booking = transaction.getBooking();
        Customer customer = booking.getCustomer();

        // Update transaction
        transaction.setStatus("SUCCESS");
        transaction.setResultCode(callback.getResultCode());
        transaction.setResultMessage(callback.getResultMessage());

        // Update booking payment status
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        log.info("Booking payment marked as PAID for bookingId: {}, transId: {}", 
                booking.getBookingId(), callback.getTransId());

        // Process loyalty points
        processLoyaltyPoints(customer, booking);

        // Check and update tier if eligible
        checkAndUpdateTier(customer);
    }

    /**
     * Handles failed payment: updates transaction status without modifying booking.
     */
    private void processFailedPayment(PaymentTransaction transaction, MomoIpnCallbackRequest callback) {
        transaction.setStatus("FAILED");
        transaction.setResultCode(callback.getResultCode());
        transaction.setResultMessage(callback.getResultMessage());
        transaction.setErrorDetails("MoMo callback result code: " + callback.getResultCode() + 
                ", message: " + callback.getResultMessage());

        log.warn("Payment failed for bookingId: {}, resultCode: {}, message: {}", 
                transaction.getBooking().getBookingId(),
                callback.getResultCode(), 
                callback.getResultMessage());
    }

    /**
     * Computes and updates customer loyalty points based on booking payment.
     * Points calculation: (final_amount / 1000) * tier_multiplier
     */
    private void processLoyaltyPoints(Customer customer, Booking booking) {
        try {
            BigDecimal amount = booking.getFinalAmount();
            BigDecimal pointsEarned = amount.divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP)
                    .multiply(customer.getTier().getTierMultiplier());
            
            int pointsToAdd = pointsEarned.intValue();
            int newLoyaltyPoints = customer.getLoyaltyPoints() + pointsToAdd;
            BigDecimal newTotalSpending = customer.getTotalSpending().add(amount);

            customer.setLoyaltyPoints(newLoyaltyPoints);
            customer.setTotalSpending(newTotalSpending);

            customerRepository.save(customer);

            log.info("Loyalty points processed for customerId: {}, pointsEarned: {}, totalPoints: {}, totalSpending: {}", 
                    customer.getCustomerId(), pointsToAdd, newLoyaltyPoints, newTotalSpending);

        } catch (Exception e) {
            log.error("Error processing loyalty points for customerId: {}", customer.getCustomerId(), e);
        }
    }

    /**
     * Checks if customer is eligible for tier upgrade based on cumulative spending.
     * Updates customer tier if spending threshold is met.
     */
    private void checkAndUpdateTier(Customer customer) {
        try {
            // Tier progression logic
            // Assuming tier names are: BRONZE, SILVER, GOLD, PLATINUM
            // Based on totalSpending thresholds
            String currentTierName = customer.getTier().getTierName();
            BigDecimal spending = customer.getTotalSpending();

            log.info("Checking tier upgrade for customerId: {}, currentTier: {}, totalSpending: {}", 
                    customer.getCustomerId(), currentTierName, spending);

            // This is placeholder logic - adjust based on your actual tier thresholds
            // You may need to create a tier lookup service for this
            
        } catch (Exception e) {
            log.error("Error checking tier upgrade for customerId: {}", customer.getCustomerId(), e);
        }
    }

    /**
     * Builds the complete MoMo payment request with signature.
     */
    private MomoPaymentRequest buildMomoPaymentRequest(Booking booking, String requestId, String orderId) {
        long amount = booking.getFinalAmount().multiply(BigDecimal.valueOf(1)).longValue();

        // Build request payload for signature
        String rawData = "accessKey=" + momoProperties.getAccessKey() +
                "&amount=" + amount +
                "&extraData=" +
                "&ipnUrl=" + momoProperties.getIpnUrl() +
                "&lang=" + momoProperties.getLang() +
                "&orderId=" + orderId +
                "&orderInfo=" + "Thanh toán đơn rửa xe " + booking.getBookingCode() +
                "&partnerCode=" + momoProperties.getPartnerCode() +
                "&redirectUrl=" + momoProperties.getRedirectUrl() +
                "&requestId=" + requestId +
                "&requestType=" + momoProperties.getRequestType();

        String signature = HmacSHA256Util.generateSignature(rawData, momoProperties.getSecretKey());

        return MomoPaymentRequest.builder()
                .partnerCode(momoProperties.getPartnerCode())
                .partnerName("AutoWashPro")
                .accessKey(momoProperties.getAccessKey())
                .requestId(requestId)
                .amount(amount)
                .orderId(orderId)
                .orderInfo("Thanh toán đơn rửa xe " + booking.getBookingCode())
                .redirectUrl(momoProperties.getRedirectUrl())
                .ipnUrl(momoProperties.getIpnUrl())
                .requestType(momoProperties.getRequestType())
                .signature(signature)
                .lang(momoProperties.getLang())
                .extraData("")
                .build();
    }

    /**
     * Sends payment request to MoMo Sandbox endpoint.
     */
    private MomoPaymentResponse sendPaymentRequest(MomoPaymentRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MomoPaymentRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Sending MoMo payment request to: {}", momoProperties.getEndpoint());

            ResponseEntity<MomoPaymentResponse> response = restTemplate.postForEntity(
                    momoProperties.getEndpoint(),
                    entity,
                    MomoPaymentResponse.class
            );

            MomoPaymentResponse paymentResponse = response.getBody();
            if (paymentResponse == null) {
                throw new BadRequestException("Empty response from MoMo API");
            }

            log.debug("MoMo response - ResultCode: {}, Message: {}", 
                    paymentResponse.getResultCode(), paymentResponse.getResultMessage());

            return paymentResponse;

        } catch (Exception e) {
            log.error("Error sending payment request to MoMo", e);
            throw new BadRequestException("Failed to communicate with payment gateway: " + e.getMessage());
        }
    }

    /**
     * Verifies the signature of IPN callback from MoMo.
     */
    private boolean verifyCallbackSignature(MomoIpnCallbackRequest callback) {
        try {
            String rawData = "accessKey=" + callback.getAccessKey() +
                    "&amount=" + callback.getAmount() +
                    "&extraData=" + (callback.getExtraData() != null ? callback.getExtraData() : "") +
                    "&orderId=" + callback.getOrderId() +
                    "&orderInfo=" + callback.getOrderInfo() +
                    "&orderType=" + (callback.getOrderType() != null ? callback.getOrderType() : "") +
                    "&partnerCode=" + callback.getPartnerCode() +
                    "&requestId=" + callback.getRequestId() +
                    "&responseTime=" + callback.getResponseTime() +
                    "&resultCode=" + callback.getResultCode() +
                    "&resultMessage=" + callback.getResultMessage() +
                    "&transId=" + callback.getTransId();

            boolean isValid = HmacSHA256Util.verifySignature(rawData, momoProperties.getSecretKey(), callback.getSignature());
            
            if (!isValid) {
                log.warn("Invalid callback signature for orderId: {}", callback.getOrderId());
            }
            
            return isValid;

        } catch (Exception e) {
            log.error("Error verifying callback signature", e);
            return false;
        }
    }

    /**
     * Generates unique request ID for MoMo (timestamp-based).
     */
    private String generateRequestId(Long bookingId) {
        return "REQ-" + bookingId + "-" + System.currentTimeMillis();
    }

    /**
     * Generates unique order ID for MoMo (booking code-based).
     */
    private String generateOrderId(Booking booking) {
        return booking.getBookingCode() + "-" + Instant.now().getEpochSecond();
    }
}
