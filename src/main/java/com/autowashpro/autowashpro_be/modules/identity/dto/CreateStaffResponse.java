package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau khi Admin/Manager tạo tài khoản nhân viên")
public class CreateStaffResponse {

    @Schema(description = "Thông tin nhân viên vừa tạo (status = PENDING_ACTIVATION)")
    private StaffResponse staff;

    @Schema(description = "Thông báo kết quả")
    private String message;

    @Schema(description = "Email đã gửi link kích hoạt")
    private String email;

    @Schema(description = "GMAIL hoặc MOCK")
    private String mailMode;

    @Schema(description = "MOCK mode: URL kích hoạt để test không cần mở email")
    private String devActionUrl;

    @Schema(description = "Mật khẩu tạm — chỉ có khi Admin reset mật khẩu")
    private String temporaryPassword;
}
