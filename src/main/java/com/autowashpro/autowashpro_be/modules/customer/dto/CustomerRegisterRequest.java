package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request đăng ký khách hàng mới")
public class CustomerRegisterRequest {

    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Phone number must start with 0 and be 10-11 digits")
    @Schema(description = "Số điện thoại (10-11 số, bắt đầu 0)", example = "0901234567")
    private String phoneNumber;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Họ tên khách hàng", example = "Nguyen Van A")
    private String fullName;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "Mật khẩu (tối thiểu 6 ký tự)", example = "Pass@123")
    private String password;
}
