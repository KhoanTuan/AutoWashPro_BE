package com.autowashpro.autowashpro_be.modules.dashboard.controller;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.AiAdvisorProposalResponse;
import com.autowashpro.autowashpro_be.modules.dashboard.service.AdminAiAdvisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard/ai-advisor")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "08 - Admin Dashboard", description = "Command Center Dashboard & KPI Analytics — trang `/admin/dashboard`")
public class AdminAiAdvisorController {

    private final AdminAiAdvisorService aiAdvisorService;

    @PostMapping("/analyze")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[AI ADVISOR] Nhận lời khuyên & phân tích rủi ro E2E-3", description = "Đóng gói số liệu Dashboard gửi sang Trợ lý AI (External API). Tự động sinh lời khuyên và cấu hình Voucher Win-back khi phát hiện slot rủi ro cao.")
    public ResponseEntity<AiAdvisorProposalResponse> analyzeDashboard(DashboardFilterRequest filter) {
        return ResponseEntity.ok(aiAdvisorService.analyzeDashboard(filter));
    }

    @PostMapping("/apply-proposal/{proposalId}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[AI ADVISOR] 1-Click Apply tạo chiến dịch Voucher Win-back", description = "Bấm 1 nút khởi tạo chiến dịch khuyến mãi theo đề xuất AI và phát thông báo WebSocket/FCM tới tệp khách hàng mục tiêu.")
    public ResponseEntity<Map<String, Object>> applyProposal(@PathVariable("proposalId") String proposalId) {
        boolean success = aiAdvisorService.applyProposal(proposalId);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "proposalId", proposalId,
                "message", "🎉 Đã kích hoạt chiến dịch Voucher Win-back thành công cho tệp khách hàng mục tiêu!"
        ));
    }
}
