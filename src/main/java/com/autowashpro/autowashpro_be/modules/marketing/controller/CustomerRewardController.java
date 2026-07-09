package com.autowashpro.autowashpro_be.modules.marketing.controller;

import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerRewardShopResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerVoucherResponse;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerRewardService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/rewards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "11 - Customer Rewards Shop & Vouchers Wallet", description = "Shop Quà Tặng Đổi Điểm & Ví Voucher cá nhân — trang `/rewards`")
public class CustomerRewardController {

    private final CustomerRewardService rewardService;

    @GetMapping("/shop")
    @Operation(summary = "[READ] Shop Quà Tặng & Ưu đãi", description = "Lấy danh sách quà tặng. Tự động đối chiếu điểm và hạng VIP của khách, gắn cờ khóa FOMO nếu chưa đủ điều kiện.")
    public ResponseEntity<List<CustomerRewardShopResponse>> getRewardShop(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(rewardService.getRewardShop(id));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("isAuthenticated() or #customerId != null")
    @Operation(summary = "[CLAIM] Thu thập voucher miễn phí", description = "Khách hàng lấy voucher miễn phí (costPoints = 0) vào ví.")
    public ResponseEntity<CustomerVoucherResponse> claimFreeVoucher(
            @PathVariable("id") Long promotionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(rewardService.claimFreeVoucher(id, promotionId));
    }

    @PostMapping("/{id}/exchange")
    @PreAuthorize("isAuthenticated() or #customerId != null")
    @Operation(summary = "[EXCHANGE] Đổi voucher bằng điểm Loyalty", description = "Trừ điểm loyalty của khách và cấp voucher vào ví.")
    public ResponseEntity<CustomerVoucherResponse> exchangePoints(
            @PathVariable("id") Long promotionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(rewardService.exchangePoints(id, promotionId));
    }

    @GetMapping("/my-vouchers")
    @Operation(summary = "[WALLET] Ví Voucher của tôi", description = "Lấy danh sách các voucher đang khả dụng/đã dùng trong ví cá nhân.")
    public ResponseEntity<List<CustomerVoucherResponse>> getMyVouchers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false, defaultValue = "ISSUED") String status) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(rewardService.getMyVouchers(id, status));
    }

    private Long resolveCustomerId(UserPrincipal principal, Long paramId) {
        if (paramId != null) return paramId;
        if (principal != null && principal.getId() != null) return principal.getId();
        return 1L; // Mặc định khách hàng Nguyễn Văn An (ID 1) cho demo/testing
    }
}
