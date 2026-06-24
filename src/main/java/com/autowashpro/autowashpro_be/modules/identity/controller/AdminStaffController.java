package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.service.StaffService;
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

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_03_ADMIN_STAFF;
import static com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog.CREATE_UPDATE_STAFF;
import static com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog.DELETE_STAFF;
import static com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog.READ_STAFF;

/**
 * Tầng presentation — chỉ nhận HTTP request, validate (@Valid), phân quyền (@PreAuthorize),
 * gọi {@link StaffService} và trả ResponseEntity. Không chứa business logic hay truy vấn DB.
 *
 * <p>Permission matrix (Phase A):
 * <ul>
 *   <li>{@code READ_STAFF} — Admin, Manager</li>
 *   <li>{@code CREATE_UPDATE_STAFF} — Admin, Manager</li>
 *   <li>{@code DELETE_STAFF} — Admin only: soft/hard delete + restore</li>
 *   <li>{@code ASSIGN_ROLE} — Admin, Manager</li>
 *   <li>Technician / Cashier — không có quyền staff admin</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/staffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_03_ADMIN_STAFF, description = "Quản trị nhân sự — trang `/admin/staff`")
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('" + READ_STAFF + "')")
    @Operation(operationId = "03-01-stats", summary = "[READ] Thống kê Staff & Performance")
    public ResponseEntity<StaffSummaryStatsResponse> stats() {
        return ResponseEntity.ok(staffService.getSummaryStats());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + READ_STAFF + "')")
    @Operation(
            operationId = "03-02-list",
            summary = "[READ] Danh sách nhân viên (phân trang)",
            description = "Query: `status` (ACTIVE|INACTIVE|PENDING_ACTIVATION), `keyword`, `includeDeleted`, `page`, `size`. Cần `READ_STAFF`."
    )
    public ResponseEntity<PageResponse<StaffResponse>> list(
            @Parameter(description = "ACTIVE, INACTIVE hoặc PENDING_ACTIVATION") @RequestParam(required = false) String status,
            @Parameter(description = "Tìm theo tên, username, email, SĐT") @RequestParam(required = false) String keyword,
            @Parameter(description = "true = gồm cả nhân viên đã soft delete (Admin)") @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(staffService.listStaff(status, keyword, includeDeleted, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + READ_STAFF + "')")
    @Operation(operationId = "03-03-detail", summary = "[READ] Chi tiết nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy staffId")
    })
    public ResponseEntity<StaffResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + CREATE_UPDATE_STAFF + "')")
    @Operation(
            operationId = "03-04-create",
            summary = "[CREATE] Tạo tài khoản nhân viên",
            description = """
                    Cần `CREATE_UPDATE_STAFF`. Admin hoặc Manager tạo tài khoản.
                    Nhân viên ở `PENDING_ACTIVATION` cho đến khi xác thực email.
                    Manager không được gán `ROLE_ADMIN` / `ROLE_MANAGER`.
                    """
    )
    public ResponseEntity<CreateStaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + CREATE_UPDATE_STAFF + "')")
    @Operation(operationId = "03-05-update", summary = "[UPDATE] Cập nhật thông tin nhân viên")
    public ResponseEntity<StaffResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + CREATE_UPDATE_STAFF + "')")
    @Operation(operationId = "03-06-update-account-status", summary = "[UPDATE] Khóa / mở khóa tài khoản (ACTIVE ↔ INACTIVE)")
    public ResponseEntity<StaffResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/work-status")
    @PreAuthorize("hasAuthority('" + CREATE_UPDATE_STAFF + "')")
    @Operation(operationId = "03-07-update-work-status", summary = "[UPDATE] Trạng thái ca (IDLE / BUSY / ON_BREAK / OFF)")
    public ResponseEntity<StaffResponse> updateWorkStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffWorkStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateWorkStatus(id, request));
    }

    @PutMapping("/{staffId}/roles")
    @PreAuthorize("hasAuthority('ASSIGN_ROLE')")
    @Operation(
            operationId = "03-08-assign-roles",
            summary = "[UPDATE] Gán vai trò cho nhân viên",
            description = "Cần `ASSIGN_ROLE`. Ghi đè toàn bộ `roleIds`."
    )
    public ResponseEntity<StaffResponse> assignRoles(
            @PathVariable Long staffId,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        return ResponseEntity.ok(staffService.assignRoles(staffId, request));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('" + CREATE_UPDATE_STAFF + "')")
    @Operation(
            operationId = "03-09-reset-password",
            summary = "[ACTION] Reset mật khẩu nhân viên",
            description = "Cần `CREATE_UPDATE_STAFF`. Reset về `AutoWash@2026`, bật `requirePasswordChange`."
    )
    public ResponseEntity<CreateStaffResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + DELETE_STAFF + "')")
    @Operation(
            operationId = "03-10-delete",
            summary = "[DELETE] Xóa nhân viên (soft/hard)",
            description = """
                    Cần `DELETE_STAFF` — chỉ Admin.
                    - Mặc định soft delete: gắn `deletedAt`, khóa tài khoản, giải phóng username/email.
                    - `hardDelete=true`: xóa vĩnh viễn khỏi DB.
                    - Không xóa chính mình hoặc admin cuối cùng.
                    """
    )
    public ResponseEntity<DeleteStaffResponse> deleteStaff(
            @PathVariable Long id,
            @RequestBody(required = false) DeleteStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.deleteStaff(id, request));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('" + DELETE_STAFF + "')")
    @Operation(
            operationId = "03-11-restore",
            summary = "[ACTION] Khôi phục nhân viên đã soft delete",
            description = "Cần `DELETE_STAFF`. Khôi phục username/email gốc, đặt `INACTIVE` — Admin bật lại ACTIVE thủ công."
    )
    public ResponseEntity<StaffResponse> restoreStaff(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.restoreStaff(id));
    }
}
