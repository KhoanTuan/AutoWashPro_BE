package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request đăng nhập hợp nhất (Omni-Login)")
public class LoginRequest {

    @Schema(description = "Username, email hoặc SĐT — ưu tiên dùng field này", example = "admin")
    private String loginId;

    @Schema(description = "Alias của loginId (tương thích client cũ)", example = "admin")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Mật khẩu", example = "Admin@123")
    private String password;

    public String resolveLoginId() {
        if (loginId != null && !loginId.isBlank()) {
            return loginId.trim();
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return null;
    }
}
