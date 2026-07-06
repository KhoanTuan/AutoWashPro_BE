package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật cấu hình hạng thành viên VIP (Admin)")
public class LoyaltyTierRequest {

    @NotNull(message = "Mức chi tiêu tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Mức chi tiêu tối thiểu phải từ 0 trở lên")
    @Schema(description = "Mức chi tiêu tối thiểu để đạt hạng (VND)", example = "1000000.00")
    private BigDecimal minSpend;

    @NotNull(message = "Hệ số tích điểm không được để trống")
    @DecimalMin(value = "1.0", message = "Hệ số tích điểm tối thiểu là 1.0")
    @Schema(description = "Hệ số nhân điểm thưởng khi sử dụng dịch vụ", example = "1.20")
    private BigDecimal tierMultiplier;

    @NotNull(message = "Số ngày đặt lịch trước không được để trống")
    @Min(value = 1, message = "Số ngày đặt lịch trước tối thiểu là 1 ngày")
    @Schema(description = "Giới hạn số ngày được phép đặt lịch trước (booking window)", example = "10")
    private Integer bookingWindowDays;
}
