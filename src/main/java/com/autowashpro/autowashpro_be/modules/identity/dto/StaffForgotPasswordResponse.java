package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau khi gửi email quên mật khẩu staff")
public class StaffForgotPasswordResponse {

    @Schema(description = "Thông báo chung (không lộ email có tồn tại hay không)")
    private String message;

    @Schema(description = "GMAIL hoặc MOCK")
    private String mailMode;

    @Schema(description = "MOCK mode: URL reset để test không cần mở email")
    private String devActionUrl;
}
