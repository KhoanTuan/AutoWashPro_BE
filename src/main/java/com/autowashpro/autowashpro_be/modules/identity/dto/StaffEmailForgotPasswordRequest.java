package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request quên mật khẩu nhân viên — gửi link reset qua email")
public class StaffEmailForgotPasswordRequest {

    @NotBlank
    @Email
    @Schema(description = "Email đăng ký của nhân viên", example = "tech01@autowashpro.com")
    private String email;
}
