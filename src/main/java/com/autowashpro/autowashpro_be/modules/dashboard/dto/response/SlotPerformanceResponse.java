package com.autowashpro.autowashpro_be.modules.dashboard.dto.response;

import lombok.*;

/**
 * DTO chứa dữ liệu cho Biểu đồ 2: Hiệu suất Khai thác Slot E2E-1 & Cảnh báo Rủi ro Khách hàng.
 * Hiển thị theo các khung giờ (VD: 08:00 đến 19:00) do Admin cấu hình trong E2E-1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotPerformanceResponse {

    /**
     * Mốc giờ E2E-1 (VD: "08:00", "09:00", ... "19:00").
     */
    private String timeSlot;

    /**
     * Tổng công suất tối đa (maxCapacity) được Admin cấu hình trong chu kỳ.
     */
    private Long configuredMaxCapacity;

    /**
     * Số xe thực tế đã đặt thành công vào khung giờ này.
     */
    private Long actualBooked;

    /**
     * Tỷ lệ % lấp đầy slot bãi (Occupancy Rate = actualBooked / configuredMaxCapacity * 100).
     * Cột xanh trên Biểu đồ 2.
     */
    private Double occupancyRate;

    /**
     * Tỷ lệ % hủy hẹn / trễ giờ (No-Show Rate E2E-1).
     * Đường đỏ trên Biểu đồ 2.
     */
    private Double noShowRate;

    /**
     * Cờ cảnh báo rủi ro E2E-3: Bật TRUE khi occupancyRate < 50% VÀ noShowRate > 20%.
     * Khi TRUE, hệ thống kích hoạt AI đề xuất chiến dịch Voucher Loyalty Win-back.
     */
    private Boolean isHighRisk;
}
