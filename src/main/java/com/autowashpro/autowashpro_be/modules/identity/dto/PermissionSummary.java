package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Mã quyền vi mô — dùng cho ma trận RBAC")
public class PermissionSummary {

    @Schema(description = "ID quyền", example = "5")
    private Integer permissionId;

    @Schema(description = "Mã quyền — dùng cho PermissionGuard trên FE", example = "CASHIER_CHECKIN")
    private String permissionCode;
}
