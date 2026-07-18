package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Yêu cầu cập nhật thông tin loyalty của khách hàng (Admin/Staff)")
public class AdminUpdateLoyaltyRequest {

    @NotNull(message = "Loyalty points cannot be null")
    @Min(value = 0, message = "Loyalty points must be greater than or equal to 0")
    @Schema(description = "Số điểm loyalty mới", example = "100")
    private Integer loyaltyPoints;

    @NotNull(message = "Total spending cannot be null")
    @DecimalMin(value = "0.0", message = "Total spending must be greater than or equal to 0.0")
    @Schema(description = "Tổng chi tiêu mới", example = "500000.00")
    private BigDecimal totalSpending;

    @NotBlank(message = "Tier name cannot be blank")
    @Schema(description = "Tên hạng thành viên mới", example = "GOLD")
    private String tierName;
}
