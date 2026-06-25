package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Thống kê tổng quan trang Staff & Performance")
public class StaffSummaryStatsResponse {

    @Schema(description = "Tổng nhân viên ACTIVE", example = "24")
    private long totalActiveStaff;

    @Schema(description = "Hiệu suất trung bình (%)", example = "94.2")
    private double avgEfficiency;

    @Schema(description = "Điểm đánh giá trung bình /5", example = "4.85")
    private double teamScore;

    @Schema(description = "Số nhân viên đang nghỉ giữa ca (ON_BREAK)", example = "4")
    private long onBreakNow;

    @Schema(description = "Số nhân viên OFF-duty", example = "3")
    private long offDutyNow;
}
