package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request bước 1 quên mật khẩu — gửi OTP")
public class CustomerForgotPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Invalid phone number format")
    @Schema(description = "SĐT đã đăng ký tài khoản", example = "0901234567")
    private String phoneNumber;
}
