package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerLoginRequest {
    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String password;
}
