package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerEmailForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}
