package com.autowashpro.autowashpro_be.modules.marketing.controller;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.FeedbackResolveRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.FeedbackStatus;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminFeedbackService;
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

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "10 - Admin Customer Feedbacks & CSKH", description = "Quản lý ý kiến đánh giá, khiếu nại chất lượng dịch vụ & đền bù CSKH")
public class AdminFeedbackController {

    private final AdminFeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[READ] Danh sách Ý kiến đánh giá & Khiếu nại", description = "Hỗ trợ lọc theo trạng thái (NEW/RESOLVED) và số sao đánh giá (ví dụ <= 2 sao).")
    public ResponseEntity<Page<FeedbackResponse>> getFeedbacks(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) Integer ratingLte,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getFeedbacks(status, ratingLte, pageable));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[RESOLVE] Xử lý khiếu nại & Gửi voucher đền bù", description = "Ghi nhận lời giải trình khắc phục và hỗ trợ 1-click tự động phát hành Voucher Đền Bù tạ lỗi vào ví khách hàng.")
    public ResponseEntity<FeedbackResponse> resolveFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackResolveRequest request) {
        return ResponseEntity.ok(feedbackService.resolveFeedback(id, request));
    }
}
