package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request đăng nhập hợp nhất (Omni-Login)")
public class LoginRequest {

    @Schema(
            description = "**Staff only** — username, email hoặc SĐT nhân viên",
            example = "tech01"
    )
    private String loginId;

    @Schema(
            description = "**Staff only** — alias của loginId (tương thích FE gửi `username`)",
            example = "tech01"
    )
    private String username;

    @Schema(
            description = "**Customer only** — số điện thoại đã đăng ký (không dùng loginId cho khách hàng)",
            example = "0902000001"
    )
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Schema(description = "Mật khẩu", example = "Tech@123")
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

    public boolean hasStaffLogin() {
        return resolveLoginId() != null;
    }

    public boolean hasCustomerLogin() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    @AssertTrue(message = "Provide loginId/username for staff OR phoneNumber for customer (not both)")
    @Schema(hidden = true)
    public boolean isLoginIdentifierValid() {
        return hasStaffLogin() ^ hasCustomerLogin();
    }
}
