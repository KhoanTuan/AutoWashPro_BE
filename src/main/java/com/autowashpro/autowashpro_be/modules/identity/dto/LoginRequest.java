package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request đăng nhập nhân viên nội bộ")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Tên đăng nhập nhân viên", example = "admin")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Mật khẩu", example = "Admin@123")
    private String password;
}
