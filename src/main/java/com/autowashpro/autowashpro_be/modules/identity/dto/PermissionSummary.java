package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Mã quyền vi mô — dùng cho ma trận RBAC")
public class PermissionSummary {

    private Integer permissionId;
    private String permissionCode;
    private String description;
    private String moduleGroup;
    private Integer phase;
    private Boolean enabled;
}
