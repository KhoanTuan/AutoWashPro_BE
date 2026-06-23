package com.autowashpro.autowashpro_be.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotBlank
    @Size(max = 255)
    private String description;
}
