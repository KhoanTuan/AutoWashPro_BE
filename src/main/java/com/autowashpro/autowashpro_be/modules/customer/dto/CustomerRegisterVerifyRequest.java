package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request xác nhận OTP đăng ký khách hàng")
public class CustomerRegisterVerifyRequest {

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Phone number must start with 0 and be 10-11 digits")
    @Schema(description = "SĐT đã dùng ở bước đăng ký", example = "0901234567")
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    @Schema(description = "Mã OTP 6 số nhận qua SMS", example = "654321")
    private String otp;
}
