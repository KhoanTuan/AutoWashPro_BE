package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Ma trận RBAC đầy đủ cho UI — permissions (cột) + roles (hàng)")
public class RoleMatrixResponse {

    @Schema(description = "Tất cả permission (cột ma trận)")
    private List<PermissionSummary> permissions;

    @Schema(description = "Tất cả role kèm permissionIds đã gán (hàng ma trận)")
    private List<RoleMatrixRow> roles;
}
