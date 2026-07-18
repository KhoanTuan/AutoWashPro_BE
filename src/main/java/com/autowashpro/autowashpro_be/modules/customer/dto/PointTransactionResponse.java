package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sao kê giao dịch điểm thưởng — dùng cho lịch sử điểm")
public class PointTransactionResponse {

    @Schema(description = "ID giao dịch điểm", example = "101")
    private Long pointTransactionId;

    @Schema(description = "Số điểm biến động (tích hoặc tiêu/trừ)", example = "50")
    private Integer points;

    @Schema(description = "Loại hoạt động điểm", example = "EARNED")
    private String activityType;

    @Schema(description = "Mã booking liên quan", example = "AW-9801")
    private String bookingCode;

    @Schema(description = "Thời gian thực hiện giao dịch", example = "2026-07-18T10:14:00")
    private LocalDateTime createdAt;
}
