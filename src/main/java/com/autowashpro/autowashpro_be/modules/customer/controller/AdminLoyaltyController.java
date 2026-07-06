package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierResponse;
import com.autowashpro.autowashpro_be.modules.customer.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loyalty")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGE_LOYALTY_CONFIG', 'ROLE_ADMIN', 'ROLE_MANAGER')")
@Tag(name = "Admin Loyalty Configuration", description = "Cấu hình chính sách Tích điểm & Giới hạn đặt trước (Booking Window) của Hạng thành viên")
public class AdminLoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/tiers")
    @Operation(summary = "Xem toàn bộ danh sách hạng VIP & cấu hình")
    public ResponseEntity<List<LoyaltyTierResponse>> getAllTiers() {
        return ResponseEntity.ok(loyaltyService.getAllTiers());
    }

    @PutMapping("/tiers/{tierId}")
    @Operation(summary = "Cập nhật cấu hình hạng VIP (Booking Window, Mức chi tiêu, Hệ số nhân điểm)")
    public ResponseEntity<LoyaltyTierResponse> updateTierConfig(
            @PathVariable Integer tierId,
            @Valid @RequestBody LoyaltyTierRequest request) {
        return ResponseEntity.ok(loyaltyService.updateTierConfig(tierId, request));
    }
}
