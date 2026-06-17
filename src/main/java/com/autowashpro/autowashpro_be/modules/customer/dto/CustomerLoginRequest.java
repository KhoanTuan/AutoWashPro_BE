package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request đăng nhập khách hàng")
public class CustomerLoginRequest {

    @NotBlank
    @Schema(description = "Số điện thoại đã đăng ký", example = "0901234567")
    private String phoneNumber;

    @NotBlank
    @Schema(description = "Mật khẩu", example = "Pass@123")
    private String password;
}
