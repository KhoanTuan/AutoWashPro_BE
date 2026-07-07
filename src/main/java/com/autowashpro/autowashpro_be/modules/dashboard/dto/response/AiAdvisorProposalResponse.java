package com.autowashpro.autowashpro_be.modules.dashboard.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về từ Trợ lý AI (External AI API) phân tích điểm mạnh/yếu trên Dashboard Command Center,
 * đồng thời đề xuất cấu hình chiến dịch Voucher Loyalty Win-back (E2E-3) cho Admin bấm nút 1-Click Apply.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAdvisorProposalResponse {

    private String status;           // SUCCESS / ERROR
    private String generatedAt;
    private String summaryAnalysis;  // Tóm tắt phân tích (VD: "Phát hiện khung 13h-15h vắng khách...")
    private List<ActionableProposal> actionableProposals;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionableProposal {
        private String proposalId;             // VD: "PROP-WINBACK-01"
        private String title;                  // VD: "🔥 Kích hoạt Chiến dịch Voucher Loyalty Win-back (E2E-3)"
        private String reason;                 // Lý do đề xuất (Dựa theo các khung giờ isHighRisk = true)
        private SuggestedVoucherConfig suggestedVoucherConfig;
        private Integer estimatedTargetAudience; // Số lượng khách hàng mục tiêu ước tính (VD: 85)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuggestedVoucherConfig {
        private String code;                   // VD: "WINBACK20"
        private String name;                   // VD: "Giảm 20% cho Khung Giờ Trưa (13h-15h)"
        private String discountType;           // FIXED, PERCENT, FREE_WASH
        private BigDecimal value;              // VD: 20 (20%)
        private Integer minTier;               // VD: 1 (Gold trở lên)
        private Integer minRecencyDays;        // VD: 30 (Khách đã vắng mặt >= 30 ngày)
        private Integer costPoints;            // VD: 0 (Miễn phí Claiming)
    }
}
