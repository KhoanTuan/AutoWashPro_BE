package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response sau khi gửi OTP đăng ký")
public class RegisterOtpSentResponse {

    @Schema(description = "Thông báo cho người dùng", example = "OTP sent to your phone number")
    private String message;

    @Schema(description = "SĐT nhận OTP", example = "0901234567")
    private String phoneNumber;

    @Schema(description = "Thời gian OTP còn hiệu lực (giây)", example = "120")
    private int expiresInSeconds;
}
