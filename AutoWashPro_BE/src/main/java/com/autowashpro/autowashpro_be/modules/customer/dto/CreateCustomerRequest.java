package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Admin tạo khách hàng mới (walk-in / provisioning)")
public class CreateCustomerRequest {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotBlank
    @Size(max = 15)
    @Schema(example = "0901234567")
    private String phoneNumber;

    @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "Biển số xe chính", example = "51A-12345")
    private String licensePlate;

    @Pattern(regexp = "SEDAN|SUV|TRUCK", message = "carType must be SEDAN, SUV or TRUCK")
    @Schema(example = "SEDAN")
    private String carType;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "status must be ACTIVE or INACTIVE")
    private String status;
}
