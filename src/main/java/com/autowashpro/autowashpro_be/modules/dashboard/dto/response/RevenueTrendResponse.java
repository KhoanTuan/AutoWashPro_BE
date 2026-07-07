package com.autowashpro.autowashpro_be.modules.dashboard.dto.response;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO chứa dữ liệu cho Biểu đồ 1: Xu hướng Vận hành & Cơ cấu Dịch vụ NovaWash.
 * Trục hoành (timeLabel) tự động co giãn độ mịn theo timeRange (Giờ / Thứ / Cụm ngày / Tháng).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueTrendResponse {

    /**
     * Nhãn trục hoành X (VD: "08:00", "Thứ 2", "Ngày 1-5", hoặc "Tháng 1").
     */
    private String timeLabel;

    /**
     * Doanh thu từ gói Rửa Tiêu Chuẩn.
     */
    private BigDecimal standardWashRevenue;

    /**
     * Doanh thu từ gói Combo Nội Thất.
     */
    private BigDecimal interiorComboRevenue;

    /**
     * Doanh thu từ gói Ceramic VIP.
     */
    private BigDecimal ceramicVipRevenue;

    /**
     * Tổng doanh thu của mốc thời gian này.
     */
    private BigDecimal totalRevenue;

    /**
     * Giá trị đơn hàng trung bình (AOV - Average Order Value, tính bằng VND/xe).
     */
    private BigDecimal aov;
}
