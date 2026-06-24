package com.autowashpro.autowashpro_be.modules.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCustomerStatusRequest {
    @NotBlank
    @Pattern(regexp = "ACTIVE|INACTIVE")
    private String status;
}
