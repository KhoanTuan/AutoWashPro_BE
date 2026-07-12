package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.booking.dto.CheckoutRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.CheckoutResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.MomoIpnCallbackRequest;
import com.autowashpro.autowashpro_be.modules.booking.service.MomoPaymentService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment Controller - Handles payment gateway operations for bookings.
 * Provides endpoints for:
 * 1. MoMo payment checkout
 * 2. MoMo IPN callback webhook
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final MomoPaymentService momoPaymentService;

    /**
     * Customer endpoint: Initiate MoMo payment for a booking.
     * 
     * @param request CheckoutRequest containing bookingId and payment method
     * @param principal Authenticated user principal
     * @return CheckoutResponse with MoMo payment gateway URL
     */
    @PostMapping("/customer/bookings/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện thanh toán!");
        }

        // Validate payment method
        if (!"MOMO".equalsIgnoreCase(request.getPaymentMethod())) {
            throw new BadRequestException("Hiện tại chỉ hỗ trợ thanh toán qua MoMo");
        }

        log.info("Customer {} initiated checkout for booking {}", 
                principal.getId(), request.getBookingId());

        CheckoutResponse response = momoPaymentService.checkoutWithMoMo(
                request.getBookingId(),
                principal.getId()
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Webhook endpoint: Receive and process MoMo IPN (Instant Payment Notification) callbacks.
     * This endpoint is called by MoMo servers when a payment transaction completes.
     * 
     * Security Note: This endpoint is PUBLIC to allow MoMo to call it.
     * However, the signature is verified within the service layer to ensure authenticity.
     * 
     * @param callback MoMo IPN callback payload containing payment result
     * @return Callback acknowledgment with status code (0 = success, 1 = failure)
     */
    @PostMapping("/callback/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpnCallback(
            @RequestBody MomoIpnCallbackRequest callback) {

        log.info("Received MoMo IPN callback for orderId: {}, resultCode: {}", 
                callback.getOrderId(), callback.getResultCode());

        try {
            Map<String, Object> response = momoPaymentService.processIpnCallback(callback);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing MoMo IPN callback", e);
            return ResponseEntity.ok(Map.of(
                    "statusCode", 1,
                    "message", "Error processing callback: " + e.getMessage()
            ));
        }
    }
}
