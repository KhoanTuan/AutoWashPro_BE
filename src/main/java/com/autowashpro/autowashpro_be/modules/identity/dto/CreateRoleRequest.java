package com.autowashpro.autowashpro_be.modules.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Tạo vai trò mới")
public class CreateRoleRequest {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "ROLE_[A-Z0-9_]+", message = "roleName must match ROLE_* format")
    @Schema(example = "ROLE_SUPERVISOR")
    private String roleName;

    @NotBlank
    @Size(max = 255)
    private String description;

    @Schema(description = "Permission IDs — chỉ phase 1 (enabled) được phép gán lúc tạo")
    private List<Integer> permissionIds;
}
