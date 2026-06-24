package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request bước 2 quên mật khẩu — xác thực OTP và đặt mật khẩu mới")
public class CustomerResetPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Invalid phone number format")
    @Schema(description = "SĐT đã dùng ở bước 1", example = "0901234567")
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    @Schema(description = "Mã OTP 6 số nhận qua SMS (dev: xem log console)", example = "654321")
    private String otp;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "Mật khẩu mới", example = "NewPass@123")
    private String newPassword;
}
