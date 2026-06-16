package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegisterRequest {
    @NotBlank
    @Pattern(regexp = "^0\\d{9,10}$", message = "Phone number must start with 0 and be 10-11 digits")
    private String phoneNumber;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
