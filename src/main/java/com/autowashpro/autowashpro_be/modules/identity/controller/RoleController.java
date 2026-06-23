package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
import com.autowashpro.autowashpro_be.common.openapi.ApiHidden;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    // ── Tag 06: Roles CRUD ────────────────────────────────────────────

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX') or hasAuthority('ASSIGN_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-01-list-roles", summary = "[READ] Danh sách Role (+ permissions, staffCount)")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles/{roleId}")
    @ApiHidden
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-02-detail-role", summary = "[READ] Chi tiết Role")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Integer roleId) {
        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @PostMapping("/roles")
    @ApiHidden
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-03-create-role", summary = "[CREATE] Tạo custom role (format ROLE_*)")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    @ApiHidden
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-04-update-role", summary = "[UPDATE] Cập nhật mô tả role")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    @ApiHidden
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-05-delete-role", summary = "[DELETE] Xóa role (chặn system role & staff đang gán)")
    public ResponseEntity<MessageResponse> deleteRole(@PathVariable Integer roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(MessageResponse.of("Role deleted"));
    }

    // ── Tag 07: Permission Matrix ───────────────────────────────────

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Tag(name = TAG_07_PERMISSION_MATRIX)
    @Operation(
            operationId = "07-01-list-permissions",
            summary = "[READ] Danh sách Permission (ma trận RBAC)",
            description = "`includeDisabled=true` → hiển thị cả Flow 2 (sắp có, chưa gán được)"
    )
    public ResponseEntity<List<PermissionSummary>> getPermissions(
            @Parameter(description = "Bao gồm permission Flow 2 chưa bật")
            @RequestParam(defaultValue = "true") boolean includeDisabled
    ) {
        return ResponseEntity.ok(roleService.getAllPermissions(includeDisabled));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Tag(name = TAG_07_PERMISSION_MATRIX)
    @Operation(
            operationId = "07-02-update-role-permissions",
            summary = "[UPDATE] Gán permissions cho Role (ghi đè toàn bộ)",
            description = "Body: `{ \"permissionIds\": [1, 2, 5] }` — chỉ permission `enabled=true`"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "permissionId không hợp lệ hoặc chưa mở (Flow 2)"),
            @ApiResponse(responseCode = "404", description = "roleId không tồn tại")
    })
    public ResponseEntity<RoleResponse> updatePermissions(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRolePermissions(roleId, request));
    }
}
