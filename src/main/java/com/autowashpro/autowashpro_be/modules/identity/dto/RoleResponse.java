package com.autowashpro.autowashpro_be.modules.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleResponse {
    private Integer roleId;
    private String roleName;
    private String description;
    private List<PermissionSummary> permissions;
}
