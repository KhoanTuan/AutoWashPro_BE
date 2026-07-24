package com.autowashpro.autowashpro_be.modules.marketing.controller;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.DirectGrantRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.PromotionCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.TargetPreviewRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.TargetPreviewResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminPromotionService;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionKpiSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "09 - Admin Promotions & Gifting", description = "Quản lý chiến dịch Khuyến mãi, Quà tặng & Direct Gifting — trang `/admin/customers-loyalty`")
public class AdminPromotionController {

    private final AdminPromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[CREATE] Tạo mới chiến dịch Khuyến mãi/Quà tặng", description = "Tạo chiến dịch mới với luật lệ targeting tier và recency.")
    public ResponseEntity<PromotionResponse> createPromotion(@Valid @RequestBody PromotionCreateRequest request) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "[READ] Danh sách chiến dịch Khuyến mãi", description = "Lọc theo trạng thái và từ khóa, trả về tỷ lệ đổi mã và tình trạng ngân sách.")
    public ResponseEntity<Page<PromotionResponse>> getPromotions(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(promotionService.getPromotions(status, keyword, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "[READ] Chi tiết chiến dịch Khuyến mãi", description = "Lấy thông tin chi tiết của 1 chiến dịch theo ID.")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[UPDATE] Cập nhật trạng thái Kích hoạt / Tạm dừng", description = "Đổi trạng thái chiến dịch (ACTIVE, PAUSED).")
    public ResponseEntity<PromotionResponse> updateStatus(@PathVariable Long id, @RequestParam PromotionStatus status) {
        return ResponseEntity.ok(promotionService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[DELETE] Xóa chiến dịch Khuyến mãi", description = "Chuyển trạng thái chiến dịch thành DELETED.")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/target-preview")
    @PreAuthorize("hasAuthority('GRANT_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[PREVIEW] Audience Preview (Dự kiến tệp đối tượng)", description = "Tính toán thực tế số khách hàng thỏa mãn hạng tối thiểu và số ngày vắng mặt.")
    public ResponseEntity<TargetPreviewResponse> previewTarget(@RequestBody TargetPreviewRequest request) {
        return ResponseEntity.ok(promotionService.previewTarget(request));
    }

    @PostMapping("/grant-direct")
    @PreAuthorize("hasAuthority('GRANT_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[GIFTING] Direct Gifting (Phát hành quà tặng trực tiếp)", description = "Quản lý chủ động phát hành voucher đặc quyền vào ví của 1 hoặc nhóm khách hàng VIP.")
    public ResponseEntity<List<String>> grantDirect(@Valid @RequestBody DirectGrantRequest request) {
        return ResponseEntity.ok(promotionService.grantDirect(request));
    }

    @GetMapping("/kpi-summary")
    @PreAuthorize("hasAuthority('VIEW_PROMOTIONS') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "[READ] Thẻ KPI thông số tổng quan Khuyến mãi", description = "Trả về 4 thẻ KPI động cho trang quản lý Khuyến mãi.")
    public ResponseEntity<PromotionKpiSummaryResponse> getKpiSummary() {
        return ResponseEntity.ok(promotionService.getKpiSummary());
    }
}
