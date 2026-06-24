package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request tạo nhân viên — Admin/Manager gửi mật khẩu ban đầu, nhân viên xác thực email để kích hoạt")
public class CreateStaffRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, numbers, dot, underscore and hyphen")
    @Schema(description = "Username viết liền không dấu", example = "tech11")
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Email nhận link kích hoạt tài khoản", example = "tech11@autowashpro.com")
    private String email;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Họ tên đầy đủ", example = "Nguyen Van B")
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Phone number must start with 0 and be 10-11 digits")
    @Schema(description = "Số điện thoại nội bộ", example = "0912345678")
    private String phoneNumber;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "Mật khẩu ban đầu cho nhân viên", example = "Pass@123")
    private String password;

    @NotEmpty
    @Schema(description = "Danh sách roleId — lấy từ GET /api/v1/roles", example = "[3]")
    private List<Integer> roleIds;
}
