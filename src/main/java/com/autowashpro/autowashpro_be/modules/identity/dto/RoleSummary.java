package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Tóm tắt vai trò — dùng trong danh sách staff")
public class RoleSummary {

    @Schema(description = "ID vai trò", example = "2")
    private Integer roleId;

    @Schema(description = "Tên vai trò", example = "ROLE_CASHIER")
    private String roleName;
}
