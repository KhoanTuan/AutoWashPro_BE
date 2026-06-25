package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau khi Admin tạo/reset tài khoản nhân viên")
public class CreateStaffResponse {

    @Schema(description = "Thông tin nhân viên vừa tạo/cập nhật")
    private StaffResponse staff;

    @Schema(description = "Mật khẩu tạm thời — chỉ hiện lúc tạo/reset (Welcome@2026 hoặc AutoWash@2026)", example = "Welcome@2026")
    private String temporaryPassword;

    @Schema(description = "Thông báo kết quả", example = "Staff account created. Temporary password sent to cashier@autowashpro.com")
    private String message;
}
