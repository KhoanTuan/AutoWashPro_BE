package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request tạo nhân viên mới — Admin không cần gửi password")
public class CreateStaffRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Username viết liền không dấu", example = "newcashier")
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Email nhận thông báo tài khoản", example = "cashier@autowashpro.com")
    private String email;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Họ tên đầy đủ", example = "Nguyen Van B")
    private String fullName;

    @Schema(description = "Danh sách roleId cần gán — lấy từ GET /api/v1/roles", example = "[2]")
    private List<Integer> roleIds;
}
