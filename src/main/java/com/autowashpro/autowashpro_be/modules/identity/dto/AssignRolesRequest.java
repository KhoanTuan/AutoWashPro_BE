package com.autowashpro.autowashpro_be.modules.identity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRolesRequest {
    @NotEmpty(message = "At least one role is required")
    private List<Integer> roleIds;
}
