package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerResetPasswordTokenRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    private String confirmPassword;
}
