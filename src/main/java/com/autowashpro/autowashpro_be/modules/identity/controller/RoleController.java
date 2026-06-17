package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.modules.identity.dto.PermissionSummary;
import com.autowashpro.autowashpro_be.modules.identity.dto.RoleResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.UpdateRolePermissionsRequest;
import com.autowashpro.autowashpro_be.modules.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "04 - RBAC Matrix", description = "Ma trận phân quyền — trang `/internal/admin/rbac`")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Operation(
            summary = "Lấy danh sách Role kèm permissions",
            description = """
                    **Frontend:** Cột dọc của ma trận RBAC.

                    - Mỗi role gồm `roleId`, `roleName` (ROLE_ADMIN, ROLE_CASHIER...), danh sách permissions hiện có
                    - Dùng `roleId` khi gán role cho staff hoặc cập nhật permissions
                    """
    )
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Operation(
            summary = "Lấy danh sách Permission (mã quyền vi mô)",
            description = """
                    **Frontend:** Hàng ngang của ma trận RBAC.

                    - Mỗi permission có `permissionId` và `permissionCode` (vd: CASHIER_CHECKIN, TASK_CHECKLIST)
                    - Admin chỉ phân phối permission có sẵn, không tạo mã mới trên UI
                    """
    )
    public ResponseEntity<List<PermissionSummary>> getPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    @Operation(
            summary = "Cập nhật permissions cho một Role",
            description = """
                    **Frontend:** Khi Admin tích checkbox trên ma trận và bấm Lưu.

                    - Gửi toàn bộ `permissionIds` mới (ghi đè, không merge từng cái)
                    - Ví dụ: thêm FORCE_RELEASE_SLOT cho ROLE_CASHIER
                    - Hiệu lực ngay ở phiên login kế tiếp của nhân viên
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công, trả role mới"),
            @ApiResponse(responseCode = "400", description = "permissionId không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "roleId không tồn tại")
    })
    public ResponseEntity<RoleResponse> updatePermissions(
            @Parameter(description = "ID vai trò cần cập nhật", example = "2")
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRolePermissions(roleId, request));
    }
}
