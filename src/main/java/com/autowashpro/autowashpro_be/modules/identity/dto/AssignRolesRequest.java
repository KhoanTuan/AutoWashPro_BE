package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request gán role cho nhân viên — ghi đè toàn bộ roles cũ")
public class AssignRolesRequest {

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Danh sách roleId — lấy từ GET /api/v1/roles", example = "[2, 3]")
    private List<Integer> roleIds;
}
