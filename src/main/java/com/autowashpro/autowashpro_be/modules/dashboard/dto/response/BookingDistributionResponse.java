package com.autowashpro.autowashpro_be.modules.dashboard.dto.response;

import lombok.*;
import java.util.List;

/**
 * DTO chứa dữ liệu cho Biểu đồ Donut: Phân phối trạng thái đơn rửa xe trong chu kỳ lọc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDistributionResponse {

    private Long totalBookings;
    private List<StatusDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusDistribution {
        private String status;       // COMPLETED, PAID, UNPAID, CANCELLED
        private String label;        // "Hoàn thành", "Đã thanh toán", "Chưa thanh toán", "Đã hủy đơn"
        private Long count;          // Số lượng đơn
        private Double percentage;   // Tỷ lệ phần trăm (%)
    }
}
