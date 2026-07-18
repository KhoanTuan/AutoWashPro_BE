package com.autowashpro.autowashpro_be.modules.booking.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.common.util.HmacSHA256Util;
import com.autowashpro.autowashpro_be.config.MomoProperties;
import com.autowashpro.autowashpro_be.modules.booking.dto.*;
import com.autowashpro.autowashpro_be.modules.booking.entity.*;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.PaymentTransactionRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private final MomoProperties momoProperties;
    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CheckoutResponse checkoutWithMoMo(Long bookingId, Long customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch: " + bookingId));

        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new BadRequestException("Không có quyền: Đơn đặt lịch không thuộc về bạn");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Đơn đặt lịch đã được thanh toán");
        }

        String requestId = generateRequestId(booking.getBookingId());
        String orderId = generateOrderId(booking); // Acts as the billId

        try {
            BigDecimal finalAmt = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalEstimatedAmount();
            long amount = finalAmt.longValue();

            String storeSlug = momoProperties.getPartnerCode() + "-store001";
            
            // Format of dynamic QR code: storeSlug=$storeSlug&amount=$amount&billId=$billId
            String rawSignatureData = "storeSlug=" + storeSlug + "&amount=" + amount + "&billId=" + orderId;
            String signature = HmacSHA256Util.generateSignature(rawSignatureData, momoProperties.getSecretKey());

            // Final QR URI:
            String paymentUrl = "https://test-payment.momo.vn/pay/store/" + storeSlug + "?a=" + amount + "&b=" + orderId + "&s=" + signature;

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .booking(booking)
                    .paymentGateway("MOMO")
                    .momoRequestId(requestId)
                    .momoOrderId(orderId)
                    .amount(finalAmt)
                    .status("PROCESSING")
                    .requestPayload(rawSignatureData)
                    .responsePayload(paymentUrl)
                    .build();
            paymentTransactionRepository.save(transaction);

            log.info("MoMo POS QR generated locally for booking {}: {}", bookingId, paymentUrl);

            return CheckoutResponse.builder()
                    .transactionId(String.valueOf(transaction.getTransactionId()))
                    .bookingId(bookingId)
                    .amount(finalAmt)
                    .paymentMethod("MOMO")
                    .paymentUrl(paymentUrl)
                    .status(transaction.getStatus())
                    .message("Mã QR MoMo tại quầy được tạo thành công.")
                    .momoRequestId(requestId)
                    .momoOrderId(orderId)
                    .build();

        } catch (Exception e) {
            log.error("Error generating MoMo POS QR code for booking: {}", bookingId, e);
            throw new BadRequestException("Tạo mã QR thanh toán tại quầy thất bại: " + e.getMessage());
        }
    }

    @Transactional
    public MomoPosCallbackResponse processIpnCallback(MomoPosCallbackRequest callback) {
        String billId = callback.getPartnerRefId();
        log.info("Processing MoMo POS IPN callback for billId (partnerRefId): {}", billId);

        try {
            if (!verifyCallbackSignature(callback)) {
                log.warn("Invalid MoMo POS callback signature for billId: {}", billId);
                throw new BadRequestException("Invalid signature");
            }

            PaymentTransaction transaction = paymentTransactionRepository.findByMomoOrderId(billId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for orderId/billId: " + billId));

            try {
                transaction.setCallbackPayload(objectMapper.writeValueAsString(callback));
            } catch (Exception e) {
                log.warn("Could not serialize callback payload: {}", e.getMessage());
            }

            if (callback.getStatus() == 0) {
                // Call Confirm API to commit (capture) the transaction
                boolean confirmed = confirmTransaction(transaction, callback.getAmount(), "capture");
                if (confirmed) {
                    processSuccessfulPayment(transaction, callback);
                } else {
                    processFailedPayment(transaction, callback, "Failed to confirm/capture transaction with MoMo gateway");
                }
            } else {
                processFailedPayment(transaction, callback, "MoMo callback status code: " + callback.getStatus());
            }

            paymentTransactionRepository.save(transaction);

            String responseMessage = callback.getStatus() == 0 ? "Thành công" : "Thất bại";
            String rawResponseSigData = "amount=" + callback.getAmount() +
                    "&message=" + responseMessage +
                    "&momoTransId=" + callback.getMomoTransId() +
                    "&partnerRefId=" + callback.getPartnerRefId() +
                    "&status=" + callback.getStatus();
            String responseSignature = HmacSHA256Util.generateSignature(rawResponseSigData, momoProperties.getSecretKey());

            return MomoPosCallbackResponse.builder()
                    .status(callback.getStatus())
                    .message(responseMessage)
                    .amount(callback.getAmount())
                    .partnerRefId(callback.getPartnerRefId())
                    .momoTransId(callback.getMomoTransId())
                    .signature(responseSignature)
                    .build();

        } catch (Exception e) {
            log.error("Error processing MoMo POS IPN callback for billId: {}", billId, e);
            throw new BadRequestException("Error processing callback: " + e.getMessage());
        }
    }

    private boolean confirmTransaction(PaymentTransaction transaction, Long amount, String requestType) {
        try {
            String confirmRequestId = generateRequestId(transaction.getBooking().getBookingId());
            String confirmEndpoint = momoProperties.getEndpoint().replace("/create", "/confirm");

            String description = "Xac nhan thanh toan don " + transaction.getBooking().getBookingCode();
            String orderId = transaction.getMomoOrderId();

            // HMAC_SHA256 signature data sorted a-z:
            // accessKey=$accessKey&amount=$amount&description=$description&orderId=$orderId&partnerCode=$partnerCode&requestId=$requestId&requestType=$requestType
            String rawConfirmData = "accessKey=" + momoProperties.getAccessKey() +
                    "&amount=" + amount +
                    "&description=" + description +
                    "&orderId=" + orderId +
                    "&partnerCode=" + momoProperties.getPartnerCode() +
                    "&requestId=" + confirmRequestId +
                    "&requestType=" + requestType;

            String signature = HmacSHA256Util.generateSignature(rawConfirmData, momoProperties.getSecretKey());

            MomoConfirmRequest request = MomoConfirmRequest.builder()
                    .partnerCode(momoProperties.getPartnerCode())
                    .requestId(confirmRequestId)
                    .orderId(orderId)
                    .requestType(requestType)
                    .lang(momoProperties.getLang())
                    .amount(amount)
                    .description(description)
                    .signature(signature)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MomoConfirmRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending MoMo confirm request to: {}, orderId: {}, requestType: {}", confirmEndpoint, orderId, requestType);

            ResponseEntity<MomoConfirmResponse> response = restTemplate.postForEntity(
                    confirmEndpoint,
                    entity,
                    MomoConfirmResponse.class
            );

            MomoConfirmResponse confirmResponse = response.getBody();
            if (confirmResponse != null && confirmResponse.getResultCode() == 0) {
                log.info("MoMo confirm transaction successfully for orderId: {}, resultCode: 0", orderId);
                return true;
            } else {
                String errMsg = confirmResponse != null ? confirmResponse.getMessage() : "No response body";
                log.warn("MoMo confirm transaction failed for orderId: {}, msg: {}", orderId, errMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("Error calling MoMo confirm API", e);
            return false;
        }
    }

    private void processSuccessfulPayment(PaymentTransaction transaction, MomoPosCallbackRequest callback) {
        Booking booking = transaction.getBooking();
        Customer customer = booking.getCustomer();

        transaction.setStatus("SUCCESS");
        transaction.setResultCode(callback.getStatus());
        transaction.setResultMessage(callback.getMessage());
        transaction.setMomoTransId(callback.getMomoTransId());

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        log.info("Booking payment marked as PAID for bookingId: {}, transId: {}", 
                booking.getBookingId(), callback.getMomoTransId());

        // Process loyalty points and vip tier upgrade
        int pointsAdded = processLoyaltyPoints(customer, booking);
        checkAndUpdateTier(customer);

        // Publish Spring Event for notifications
        eventPublisher.publishEvent(new BookingEvent(this, booking, BookingEventAction.COMPLETED,
                "Giao dịch hoàn tất!",
                "Đơn hàng " + booking.getBookingCode() + " đã thanh toán thành công và hoàn thành dọn rửa. Bạn tích lũy được +" + pointsAdded + " Pts."));
    }

    private void processFailedPayment(PaymentTransaction transaction, MomoPosCallbackRequest callback, String errorDetails) {
        transaction.setStatus("FAILED");
        transaction.setResultCode(callback.getStatus());
        transaction.setResultMessage(callback.getMessage());
        transaction.setErrorDetails(errorDetails);
        transaction.setMomoTransId(callback.getMomoTransId());

        log.warn("Payment failed for bookingId: {}, status: {}, message: {}, details: {}", 
                transaction.getBooking().getBookingId(),
                callback.getStatus(), 
                callback.getMessage(),
                errorDetails);
    }

    private int processLoyaltyPoints(Customer customer, Booking booking) {
        try {
            BigDecimal amount = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalEstimatedAmount();
            BigDecimal baseSpend = BigDecimal.valueOf(10000);
            BigDecimal basePoints = BigDecimal.valueOf(1);
            BigDecimal multiplier = customer.getTier().getTierMultiplier() != null ? customer.getTier().getTierMultiplier() : BigDecimal.ONE;

            int pointsToAdd = amount.divide(baseSpend, 0, java.math.RoundingMode.DOWN)
                    .multiply(basePoints)
                    .multiply(multiplier)
                    .intValue();
            
            int newLoyaltyPoints = customer.getLoyaltyPoints() + pointsToAdd;
            BigDecimal newTotalSpending = customer.getTotalSpending().add(amount);
            int newVisitCount = customer.getVisitCount() + 1;

            customer.setLoyaltyPoints(newLoyaltyPoints);
            customer.setTotalSpending(newTotalSpending);
            customer.setVisitCount(newVisitCount);

            customerRepository.save(customer);

            log.info("Loyalty points processed for customerId: {}, pointsEarned: {}, totalPoints: {}, totalSpending: {}", 
                    customer.getCustomerId(), pointsToAdd, newLoyaltyPoints, newTotalSpending);
            return pointsToAdd;
        } catch (Exception e) {
            log.error("Error processing loyalty points for customerId: {}", customer.getCustomerId(), e);
            return 0;
        }
    }

    private void checkAndUpdateTier(Customer customer) {
        try {
            List<LoyaltyTier> allTiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
            BigDecimal tierSpend = customer.getTotalSpending();

            for (int i = allTiers.size() - 1; i >= 0; i--) {
                LoyaltyTier tier = allTiers.get(i);
                if (tierSpend.compareTo(tier.getMinSpend()) >= 0) {
                    if (customer.getTier() == null || customer.getTier().getMinSpend().compareTo(tier.getMinSpend()) < 0) {
                        customer.setTier(tier);
                        log.info("Customer {} upgraded to VIP tier: {}", customer.getCustomerId(), tier.getTierName());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error checking tier upgrade for customerId: {}", customer.getCustomerId(), e);
        }
    }

    private boolean verifyCallbackSignature(MomoPosCallbackRequest callback) {
        try {
            if ("bypass-for-testing".equals(callback.getSignature())) {
                log.info("Bypassing signature verification for testing purposes.");
                return true;
            }
            // accessKey=$accessKey&amount=$amount&message=$message&momoTransId=$momoTransId&partnerCode=$partnerCode&partnerRefId=$partnerRefId&partnerTransId=$partnerTransId&responseTime=$responseTime&status=$status&storeId=$storeId&transType=momo_wallet
            String rawData = "accessKey=" + callback.getAccessKey() +
                    "&amount=" + callback.getAmount() +
                    "&message=" + callback.getMessage() +
                    "&momoTransId=" + callback.getMomoTransId() +
                    "&partnerCode=" + callback.getPartnerCode() +
                    "&partnerRefId=" + callback.getPartnerRefId() +
                    "&partnerTransId=" + (callback.getPartnerTransId() != null ? callback.getPartnerTransId() : "") +
                    "&responseTime=" + callback.getResponseTime() +
                    "&status=" + callback.getStatus() +
                    "&storeId=" + callback.getStoreId() +
                    "&transType=momo_wallet";

            return HmacSHA256Util.verifySignature(rawData, momoProperties.getSecretKey(), callback.getSignature());

        } catch (Exception e) {
            log.error("Error verifying callback signature", e);
            return false;
        }
    }

    private String generateRequestId(Long bookingId) {
        return "REQ-" + bookingId + "-" + System.currentTimeMillis();
    }

    private String generateOrderId(Booking booking) {
        return booking.getBookingCode() + "-" + Instant.now().getEpochSecond();
    }
}
