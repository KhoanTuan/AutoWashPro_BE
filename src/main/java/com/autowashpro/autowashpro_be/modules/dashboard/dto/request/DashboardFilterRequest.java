package com.autowashpro.autowashpro_be.modules.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO chứa tham số lọc thời gian toàn cục (Universal Time Filter) từ trang Dashboard Command Center.
 * Phục vụ cả 4 thẻ KPI và 3 biểu đồ động.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bộ lọc thời gian Dashboard Command Center")
public class DashboardFilterRequest {

    /**
     * Mốc thời gian lọc toàn cục: TODAY, WEEK, MONTH, YEAR, hoặc CUSTOM.
     * Mặc định là MONTH.
     */
    @Builder.Default
    @Schema(description = "Mốc thời gian lọc toàn cục (TODAY, WEEK, MONTH, YEAR, CUSTOM)", example = "TODAY", allowableValues = {"TODAY", "WEEK", "MONTH", "YEAR", "CUSTOM"})
    private String timeRange = "MONTH";

    /**
     * Ngày bắt đầu (Chỉ áp dụng khi timeRange = CUSTOM).
     */
    @Schema(description = "Ngày bắt đầu (YYYY-MM-DD), áp dụng cho CUSTOM", example = "2026-07-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    /**
     * Ngày kết thúc (Chỉ áp dụng khi timeRange = CUSTOM).
     */
    @Schema(description = "Ngày kết thúc (YYYY-MM-DD), áp dụng cho CUSTOM", example = "2026-07-08")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
}
