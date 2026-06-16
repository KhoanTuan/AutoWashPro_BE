package com.autowashpro.autowashpro_be.modules.identity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRolePermissionsRequest {
    @NotNull
    private List<Integer> permissionIds;
}
