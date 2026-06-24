package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.common.dto.MessageResponse;
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
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX') or hasAuthority('ASSIGN_ROLE') or hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(
            operationId = "06-01-list-roles",
            summary = "[READ] Danh sách Role (+ permissions, staffCount)",
            description = "Dùng cho dropdown gán role hoặc trang quản trị roles."
    )
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX') or hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(operationId = "06-02-detail-role", summary = "[READ] Chi tiết Role kèm permissions")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Integer roleId) {
        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(
            operationId = "06-03-create-role",
            summary = "[CREATE] Tạo custom role",
            description = """
                    `roleName` phải theo format `ROLE_*` (vd. `ROLE_SUPERVISOR`).
                    Không được trùng tên system role. Có thể gán `permissionIds` ngay khi tạo.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Tên trùng hoặc permission không hợp lệ")
    })
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(
            operationId = "06-04-update-role",
            summary = "[UPDATE] Cập nhật mô tả role",
            description = "Chỉ đổi `description` — không đổi `roleName`."
    )
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('MANAGE_ROLE')")
    @Tag(name = TAG_06_ROLES)
    @Operation(
            operationId = "06-05-delete-role",
            summary = "[DELETE] Xóa custom role",
            description = """
                    - `ROLE_ADMIN` và system roles **không xóa được**.
                    - Custom role phải không còn staff nào được gán.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "System role / còn staff / ROLE_ADMIN")
    })
    public ResponseEntity<MessageResponse> deleteRole(@PathVariable Integer roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.ok(MessageResponse.of("Role deleted successfully"));
    }

    // ── Tag 07: Permission Matrix ───────────────────────────────────

    @GetMapping("/rbac/matrix")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Tag(name = TAG_07_PERMISSION_MATRIX)
    @Operation(
            operationId = "07-00-rbac-matrix",
            summary = "[READ] Ma trận RBAC đầy đủ (permissions + roles)",
            description = """
                    Trả về dữ liệu cho UI ma trận checkbox.
                    Mỗi role có `permissionEditable`, `deletable` để FE disable nút phù hợp.
                    `ROLE_ADMIN`: permissionEditable=false, deletable=false.
                    """
    )
    public ResponseEntity<RoleMatrixResponse> getRbacMatrix(
            @Parameter(description = "Bao gồm permission Flow 2 chưa bật")
            @RequestParam(defaultValue = "false") boolean includeDisabled
    ) {
        return ResponseEntity.ok(roleService.getRbacMatrix(includeDisabled));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Tag(name = TAG_07_PERMISSION_MATRIX)
    @Operation(
            operationId = "07-01-list-permissions",
            summary = "[READ] Danh sách Permission",
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
            description = """
                    Body: `{ "permissionIds": [1, 2, 5] }` — chỉ permission `enabled=true`.
                    `ROLE_ADMIN` **không cho sửa** (system-managed).
                    System role khác phải giữ ít nhất 1 permission.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "ROLE_ADMIN locked / permission không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "roleId không tồn tại")
    })
    public ResponseEntity<RoleResponse> updatePermissions(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRolePermissions(roleId, request));
    }
}
