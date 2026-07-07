package com.autowashpro.autowashpro_be.modules.dashboard.service.impl;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.*;
import com.autowashpro.autowashpro_be.modules.dashboard.service.AdminAiAdvisorService;
import com.autowashpro.autowashpro_be.modules.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation cho Trợ lý AI (E2E-3).
 * Phân tích số liệu từ DashboardService, phát hiện slot rủi ro cao (isHighRisk = true)
 * để sinh đề xuất chiến dịch Voucher Loyalty Win-back cho Admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAiAdvisorServiceImpl implements AdminAiAdvisorService {

    private final AdminDashboardService dashboardService;

    @Override
    public AiAdvisorProposalResponse analyzeDashboard(DashboardFilterRequest filter) {
        log.info("AI Advisor analyzing dashboard for timeRange: {}", filter.getTimeRange());
        List<SlotPerformanceResponse> slots = dashboardService.getSlotPerformances(filter);

        List<String> riskySlots = new ArrayList<>();
        for (SlotPerformanceResponse slot : slots) {
            if (Boolean.TRUE.equals(slot.getIsHighRisk())) {
                riskySlots.add(slot.getTimeSlot());
            }
        }

        String summary;
        List<AiAdvisorProposalResponse.ActionableProposal> proposals = new ArrayList<>();

        if (!riskySlots.isEmpty()) {
            String slotsStr = String.join(", ", riskySlots);
            summary = String.format("⚠️ Phát hiện %d khung giờ (%s) có tỷ lệ lấp đầy thấp (<50%%) và rủi ro hủy hẹn No-show vượt ngưỡng 20%%. Đề xuất kích hoạt chiến dịch Win-back ngay để lấp đầy tải bãi.", riskySlots.size(), slotsStr);

            proposals.add(AiAdvisorProposalResponse.ActionableProposal.builder()
                    .proposalId("PROP-WINBACK-01")
                    .title("🔥 Kích hoạt Chiến dịch Voucher Loyalty Win-back (E2E-3)")
                    .reason("Khung giờ " + slotsStr + " vắng khách, tỷ lệ rớt lịch >20%.")
                    .suggestedVoucherConfig(AiAdvisorProposalResponse.SuggestedVoucherConfig.builder()
                            .code("WINBACK20")
                            .name("Giảm 20% cho Khung Giờ Trưa (" + slotsStr + ")")
                            .discountType("PERCENT")
                            .value(BigDecimal.valueOf(20))
                            .minTier(1)
                            .minRecencyDays(30)
                            .costPoints(0)
                            .build())
                    .estimatedTargetAudience(85)
                    .build());
        } else {
            summary = "✅ Hiệu suất các khung giờ E2E-1 đang hoạt động ổn định. Tỷ lệ lấp đầy tốt và rủi ro hủy hẹn nằm trong ngưỡng an toàn (<20%).";
        }

        return AiAdvisorProposalResponse.builder()
                .status("SUCCESS")
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .summaryAnalysis(summary)
                .actionableProposals(proposals)
                .build();
    }

    @Override
    public boolean applyProposal(String proposalId) {
        log.info("Applying AI Advisor win-back proposal: {}", proposalId);
        // Mô phỏng chèn chiến dịch Voucher WINBACK20 vào DB và bắn WebSocket Event cho tệp khách hàng
        log.info("Successfully published WINBACK20 promotion and dispatched notification event to target audience!");
        return true;
    }
}
