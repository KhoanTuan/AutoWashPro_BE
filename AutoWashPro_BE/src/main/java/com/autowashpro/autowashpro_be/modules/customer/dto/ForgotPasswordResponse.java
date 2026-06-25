package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau khi gửi OTP quên mật khẩu")
public class ForgotPasswordResponse {

    @Schema(description = "Thông báo cho người dùng", example = "OTP sent to your phone number")
    private String message;

    @Schema(description = "Thời gian OTP còn hiệu lực (giây) — FE hiển thị countdown", example = "120")
    private int expiresInSeconds;
}
