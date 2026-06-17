package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Hồ sơ khách hàng — dùng cho customer dashboard")
public class CustomerProfileResponse {

    @Schema(description = "ID khách hàng", example = "1")
    private Long customerId;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Họ tên", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "Hạng thành viên", example = "REGULAR")
    private String tierName;

    @Schema(description = "Số lần ghé trạm", example = "5")
    private Integer visitCount;

    @Schema(description = "Tổng chi tiêu (VND)", example = "1500000.00")
    private BigDecimal totalSpending;

    @Schema(description = "Điểm tích lũy hiện tại", example = "150")
    private Integer loyaltyPoints;
}
