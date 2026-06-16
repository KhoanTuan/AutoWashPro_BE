package com.autowashpro.autowashpro_be.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateStaffRequest {
    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    private List<Integer> roleIds;
}
