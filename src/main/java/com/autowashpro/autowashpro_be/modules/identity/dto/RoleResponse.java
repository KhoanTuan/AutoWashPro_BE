package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Thông tin vai trò kèm danh sách quyền")
public class RoleResponse {

    @Schema(description = "ID vai trò", example = "2")
    private Integer roleId;

    @Schema(description = "Tên vai trò", example = "ROLE_CASHIER")
    private String roleName;

    @Schema(description = "Mô tả vai trò", example = "Front desk cashier")
    private String description;

    @Schema(description = "Danh sách quyền hiện có của role này")
    private List<PermissionSummary> permissions;
}
