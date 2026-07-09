package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionKpiSummaryResponse {
    private BigDecimal totalPromoValueIssued; // Card 1: Giá trị ưu đãi đã phát
    private int activeCampaignsCount;         // Card 2: Chiến dịch kích hoạt
    private int totalVouchersClaimed;         // Card 3: Voucher khách đã lấy
    private double marketingRoi;               // Card 4: Hiệu quả ROI tiếp thị (ví dụ 3.2)
    private double redemptionRate;             // Card 4 subtext: Hiệu suất dùng voucher (%)
}
