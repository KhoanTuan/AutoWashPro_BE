package com.autowashpro.autowashpro_be.modules.marketing.controller;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.CustomerFeedbackCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerFeedbackService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/feedbacks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "12 - Customer Feedbacks", description = "Gửi đánh giá dịch vụ & chấm sao đơn đặt lịch rửa xe")
public class CustomerFeedbackController {

    private final CustomerFeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "[CREATE] Gửi ý kiến đánh giá & khiếu nại dịch vụ", description = "Khách hàng gửi chấm điểm sao từ 1 đến 5 và bình luận cho đơn hàng.")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId,
            @Valid @RequestBody CustomerFeedbackCreateRequest request) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(feedbackService.createFeedback(id, request));
    }

    @GetMapping("/my-feedbacks")
    @Operation(summary = "[READ] Lịch sử phản hồi của tôi", description = "Lấy danh sách các đánh giá đã gửi và tình trạng xử lý từ Admin.")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedbacks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(id));
    }

    @GetMapping
    @Operation(summary = "[READ] Danh sách phản hồi của tôi (direct path)")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedbacksDirect(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long customerId) {
        Long id = resolveCustomerId(principal, customerId);
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(id));
    }

    private Long resolveCustomerId(UserPrincipal principal, Long paramId) {
        if (paramId != null) return paramId;
        if (principal != null && principal.getId() != null) return principal.getId();
        return 1L;
    }
}
