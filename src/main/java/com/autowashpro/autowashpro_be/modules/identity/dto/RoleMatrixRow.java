package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Một hàng trong ma trận RBAC — role + permissionIds đã gán")
public class RoleMatrixRow {

    private Integer roleId;
    private String roleName;
    private String description;
    private String displayName;
    private Boolean isSystem;
    private Boolean permissionEditable;
    private Boolean deletable;
    private Integer staffCount;
    private List<Integer> permissionIds;
}
