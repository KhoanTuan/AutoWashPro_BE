package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.dto.CheckoutRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.CheckoutResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.MomoPosCallbackRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.MomoPosCallbackResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import com.autowashpro.autowashpro_be.modules.booking.service.MomoPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Billing & Payment Management", description = "Các API thanh toán tại quầy và tích hợp cổng MoMo")
public class PaymentController {

    private final BookingService bookingService;
    private final MomoPaymentService momoPaymentService;
    private final BookingRepository bookingRepository;

    /**
      * Admin/Cashier endpoint: Checkout booking at cashier counter.
      * Supports Cash, Bank Transfer, and MoMo QR generation.
      */
    @Operation(summary = "Thanh toán đơn hàng tại quầy", description = "Thu ngân xác nhận thanh toán bằng Tiền mặt / Chuyển khoản (hoàn thành trực tiếp) hoặc tạo giao dịch MoMo Sandbox để hiển thị mã QR quét thanh toán")
    @PostMapping("/admin/bookings/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<CheckoutResponse> adminCheckout(
            @PathVariable("id") Long id,
            @Valid @RequestBody CheckoutRequest request) {

        log.info("Admin initiated checkout for booking {} with method {}", id, request.getPaymentMethod());

        if ("MOMO".equalsIgnoreCase(request.getPaymentMethod())) {
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt lịch với ID: " + id));
            CheckoutResponse response = momoPaymentService.checkoutWithMoMo(id, booking.getCustomer().getCustomerId());
            return ResponseEntity.ok(response);
        } else {
            BookingResponse bookingResponse = bookingService.completeCheckout(id, request.getPaymentMethod());
            CheckoutResponse response = CheckoutResponse.builder()
                    .bookingId(id)
                    .amount(bookingResponse.getFinalAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status("SUCCESS")
                    .message("Thanh toán thành công tại quầy (" + request.getPaymentMethod() + ")")
                    .build();
            return ResponseEntity.ok(response);
        }
    }

    @Operation(summary = "Webhook nhận thông tin thanh toán từ MoMo", description = "Webhook public được MoMo Server gọi để cập nhật trạng thái đơn hàng khi giao dịch thành công/thất bại")
    @PostMapping("/callback/momo/ipn")
    public ResponseEntity<MomoPosCallbackResponse> momoIpnCallback(
            @RequestBody MomoPosCallbackRequest callback) {

        log.info("Received MoMo POS IPN callback for billId (partnerRefId): {}, status: {}", 
                callback.getPartnerRefId(), callback.getStatus());

        try {
            MomoPosCallbackResponse response = momoPaymentService.processIpnCallback(callback);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing MoMo POS IPN callback", e);
            throw new BadRequestException("Error processing callback: " + e.getMessage());
        }
    }
}
