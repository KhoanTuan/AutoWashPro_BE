package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau đăng ký/đăng nhập khách hàng")
public class CustomerAuthResponse {

    @Schema(description = "JWT access token — lưu localStorage", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Loại token", example = "Bearer")
    private String tokenType;

    @Schema(description = "ID khách hàng", example = "1")
    private Long customerId;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Họ tên", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "Hạng thành viên hiện tại", example = "REGULAR")
    private String tierName;

    @Schema(description = "Điểm tích lũy", example = "0")
    private Integer loyaltyPoints;
}
