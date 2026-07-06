package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierResponse;
import com.autowashpro.autowashpro_be.modules.customer.service.LoyaltyService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/loyalty")
@RequiredArgsConstructor
@Tag(name = "Customer Loyalty & VIP Benefits", description = "Chốt chặn Quyền lợi VIP & Giới hạn ngày đặt lịch (Booking Window)")
public class CustomerLoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/tiers")
    @Operation(summary = "Lấy danh sách các hạng thành viên VIP & quyền lợi đặt trước (7, 10, 12, 14 ngày)")
    public ResponseEntity<List<LoyaltyTierResponse>> getAllTiers() {
        return ResponseEntity.ok(loyaltyService.getAllTiers());
    }

    @GetMapping("/my-benefits")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xem trạng thái VIP, tiến độ lên hạng & giới hạn ngày đặt trước của tôi")
    public ResponseEntity<LoyaltyTierResponse> getMyLoyaltyStatus(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để kiểm tra đặc quyền VIP!");
        }
        return ResponseEntity.ok(loyaltyService.getMyLoyaltyStatus(principal.getId()));
    }
}
