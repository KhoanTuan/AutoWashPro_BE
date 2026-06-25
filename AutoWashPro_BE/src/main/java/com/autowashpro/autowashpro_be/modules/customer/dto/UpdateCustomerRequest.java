package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 15)
    private String phoneNumber;

    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(max = 20)
    private String licensePlate;

    @Pattern(regexp = "SEDAN|SUV|TRUCK")
    @Schema(example = "SEDAN")
    private String carType;

    @Pattern(regexp = "ACTIVE|INACTIVE")
    private String status;
}
