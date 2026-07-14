package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyConfigRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltySettingsResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig;
import com.autowashpro.autowashpro_be.modules.customer.service.LoyaltyService;
import com.autowashpro.autowashpro_be.modules.customer.scheduler.LoyaltyScheduler;
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
    private final LoyaltyScheduler loyaltyScheduler;

    @GetMapping("/settings")
    @Operation(summary = "Xem toàn bộ cấu hình gộp (Loyalty Config & Membership Tiers)")
    public ResponseEntity<LoyaltySettingsResponse> getLoyaltySettings() {
        return ResponseEntity.ok(loyaltyService.getLoyaltySettings());
    }

    @PutMapping("/config")
    @Operation(summary = "Cập nhật cấu hình chung Loyalty (Quy đổi điểm, Hạn dùng, Thời gian hạ hạng)")
    public ResponseEntity<LoyaltyConfig> updateLoyaltyConfig(@Valid @RequestBody LoyaltyConfigRequest request) {
        return ResponseEntity.ok(loyaltyService.updateLoyaltyConfig(request));
    }

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

    // ==========================================
    // SIMULATION CENTER FOR EVALUATION BOARD DEMO
    // ==========================================

    @PostMapping("/simulate/set-inactivity")
    @Operation(summary = "Giả lập vắng mặt của khách hàng (Đặt ngày dọn xe cuối cùng về quá khứ)")
    public ResponseEntity<String> simulateSetInactivity(
            @RequestParam Long customerId,
            @RequestParam int months) {
        loyaltyService.simulateSetInactivity(customerId, months);
        return ResponseEntity.ok("Giả lập vắng mặt " + months + " tháng thành công cho khách hàng ID: " + customerId);
    }

    @PostMapping("/simulate/set-points-expired")
    @Operation(summary = "Giả lập tích lũy điểm quá hạn (Tạo giao dịch điểm ảo từ quá khứ)")
    public ResponseEntity<String> simulateSetPointsExpired(
            @RequestParam Long customerId,
            @RequestParam int months) {
        loyaltyService.simulateSetPointsExpired(customerId, months);
        return ResponseEntity.ok("Giả lập tích điểm quá hạn " + months + " tháng thành công cho khách hàng ID: " + customerId);
    }

    @PostMapping("/simulate/run-jobs")
    @Operation(summary = "Ép chạy nóng lập tức cả 3 scheduled jobs rà soát loyalty (Hạ hạng, Hết hạn điểm, Khóa tài khoản)")
    public ResponseEntity<String> simulateRunJobs() {
        logInfo("Manually triggered run-jobs simulation");
        loyaltyScheduler.expireExpiredPoints();
        loyaltyScheduler.downgradeInactiveTiers();
        loyaltyScheduler.lockInactiveAccounts();
        return ResponseEntity.ok("Đã chạy quét rà soát toàn bộ hệ thống Loyalty thành công!");
    }

    private void logInfo(String msg) {
        org.slf4j.LoggerFactory.getLogger(AdminLoyaltyController.class).info(msg);
    }
}
