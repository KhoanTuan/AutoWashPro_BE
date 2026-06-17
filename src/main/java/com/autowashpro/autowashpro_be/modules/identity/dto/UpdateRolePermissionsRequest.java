package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request cập nhật permissions cho role — gửi toàn bộ permissionIds mới")
public class UpdateRolePermissionsRequest {

    @NotNull
    @Schema(description = "Toàn bộ permissionId được gán cho role (checkbox đã tích trên ma trận RBAC)", example = "[1, 2, 5, 8]")
    private List<Integer> permissionIds;
}
