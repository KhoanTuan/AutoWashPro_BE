package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyConfigRequest {

    @NotNull(message = "Tỷ lệ quy đổi điểm không được trống")
    @DecimalMin(value = "100.0", message = "Tỷ lệ quy đổi tối thiểu là 100 VNĐ")
    private BigDecimal basePointRate;

    @NotNull(message = "Số điểm nhận không được trống")
    @Min(value = 1, message = "Số điểm nhận tối thiểu là 1")
    private Integer basePoints;

    @NotNull(message = "Làm tròn điểm không được trống")
    private Boolean roundDown;

    @NotNull(message = "Hạn dùng điểm không được trống")
    @Min(value = 1, message = "Hạn dùng điểm tối thiểu là 1 tháng")
    private Integer pointValidityMonths;

    @NotNull(message = "Thời gian hạ hạng không được trống")
    @Min(value = 1, message = "Thời gian hạ hạng tối thiểu là 1 tháng")
    private Integer inactivityDowngradeMonths;

    @NotNull(message = "Thời gian khóa tài khoản không được trống")
    @Min(value = 1, message = "Thời gian khóa tối thiểu là 1 tháng")
    private Integer inactivityLockoutMonths;
}
