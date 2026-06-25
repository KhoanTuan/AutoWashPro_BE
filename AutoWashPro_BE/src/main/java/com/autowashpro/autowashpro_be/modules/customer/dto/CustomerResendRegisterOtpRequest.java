package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request gửi lại OTP đăng ký")
public class CustomerResendRegisterOtpRequest {

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Phone number must start with 0 and be 10-11 digits")
    @Schema(description = "SĐT đang chờ xác nhận OTP", example = "0901234567")
    private String phoneNumber;
}
