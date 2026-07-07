package com.autowashpro.autowashpro_be.modules.dashboard.dto.response;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO tổng hợp số liệu cho 4 Thẻ KPI trên cùng của màn hình Command Center Dashboard.
 * Dữ liệu được tính toán động từ các bảng gốc theo chu kỳ timeRange được chọn.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKpiSummaryResponse {

    private String timeRange;

    // --- THẺ 1: ĐẶT LỊCH TRONG KỲ ---
    private Long totalBookings;
    private Double bookingsGrowthPercentage; // % tăng trưởng so với kỳ trước
    private Double onTimeRateE2E1;           // % đúng hẹn theo E2E-1

    // --- THẺ 2: DOANH THU THỰC THU (PAID) ---
    private BigDecimal actualRevenuePaid;
    private Double revenueGrowthPercentage;  // % tăng trưởng so với kỳ trước

    // --- THẺ 3: ĐIỂM TÍCH / ĐỔI (LOYALTY) ---
    private Long loyaltyPointsNet;           // Điểm thực nhận
    private Long pointsIssued;               // Điểm đã phát
    private Long pointsRedeemed;             // Điểm đã đổi voucher

    // --- THẺ 4: HIỆU SUẤT SLOT LẤP ĐẦY E2E-1 ---
    private Double slotOccupancyRate;        // % lấp đầy slot bãi
    private String peakForecastLabel;        // Dự báo đỉnh (VD: "Các ngày lễ (Cao)")
}
