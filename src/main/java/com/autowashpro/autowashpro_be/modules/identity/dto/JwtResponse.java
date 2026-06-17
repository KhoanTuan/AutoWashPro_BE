package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Response sau khi đăng nhập / đổi mật khẩu staff")
public class JwtResponse {

    @Schema(description = "JWT access token — lưu localStorage, gắn header Authorization", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Loại token", example = "Bearer")
    private String tokenType;

    @Schema(description = "ID nhân viên", example = "1")
    private Long staffId;

    @Schema(description = "Username đăng nhập", example = "admin")
    private String username;

    @Schema(description = "Họ tên nhân viên", example = "System Administrator")
    private String fullName;

    @Schema(description = "Danh sách role (ROLE_ADMIN, ROLE_CASHIER...)", example = "[\"ROLE_ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Danh sách mã quyền vi mô — dùng cho PermissionGuard trên FE", example = "[\"MANAGE_STAFF\", \"CONFIG_RBAC_MATRIX\"]")
    private List<String> permissions;

    @Schema(description = "true = FE phải chặn sidebar và buộc đổi mật khẩu ngay", example = "false")
    private Boolean forceChangePassword;
}
