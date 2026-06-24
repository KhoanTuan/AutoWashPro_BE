package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request đổi mật khẩu nhân viên")
public class ChangePasswordRequest {

    @NotBlank
    @Schema(description = "Mật khẩu hiện tại (hoặc mật khẩu tạm từ Admin)", example = "Welcome@2026")
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "Mật khẩu mới (tối thiểu 6 ký tự)", example = "MySecurePass@123")
    private String newPassword;
}
