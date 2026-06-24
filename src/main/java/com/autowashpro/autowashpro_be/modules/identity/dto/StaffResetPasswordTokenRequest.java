package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request đặt lại mật khẩu nhân viên qua token email")
public class StaffResetPasswordTokenRequest {

    @NotBlank
    @Schema(description = "Token từ link email", example = "abc123...")
    private String token;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "Mật khẩu mới", example = "NewPass@123")
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "Xác nhận mật khẩu mới", example = "NewPass@123")
    private String confirmPassword;
}
