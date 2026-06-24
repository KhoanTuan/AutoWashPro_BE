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

    @Schema(description = "ID nhân viên", example = "3")
    private Long staffId;

    @Schema(description = "Login ID đã dùng để đăng nhập — **chỉ trả về khi userType = STAFF**", example = "tech01")
    private String loginId;

    @Schema(description = "Username nhân viên — **chỉ trả về khi userType = STAFF**", example = "tech01")
    private String username;

    @Schema(description = "Họ tên nhân viên", example = "System Administrator")
    private String fullName;

    @Schema(description = "Danh sách role (ROLE_ADMIN, ROLE_CASHIER...)", example = "[\"ROLE_ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Danh sách mã quyền vi mô — dùng cho PermissionGuard trên FE", example = "[\"MANAGE_STAFF\", \"CONFIG_RBAC_MATRIX\"]")
    private List<String> permissions;

    @Schema(description = "true = FE phải chặn sidebar và buộc đổi mật khẩu ngay", example = "false")
    private Boolean forceChangePassword;

    @Schema(description = "STAFF hoặc CUSTOMER", example = "STAFF")
    private String userType;

    @Schema(description = "Đường dẫn FE sau login — dùng navigate(redirectUrl)", example = "/admin/dashboard")
    private String redirectUrl;

    @Schema(description = "ID khách hàng (chỉ khi userType = CUSTOMER)", example = "1")
    private Long customerId;

    @Schema(description = "SĐT khách hàng (chỉ khi userType = CUSTOMER)", example = "0911207121")
    private String phoneNumber;

    @Schema(description = "Hạng thành viên (chỉ khi userType = CUSTOMER)", example = "REGULAR")
    private String tierName;

    @Schema(description = "Điểm tích lũy (chỉ khi userType = CUSTOMER)", example = "0")
    private Integer loyaltyPoints;
}
