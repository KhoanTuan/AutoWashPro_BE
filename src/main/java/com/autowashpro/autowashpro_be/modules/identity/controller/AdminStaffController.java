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

@RestController
@RequestMapping("/api/v1/admin/staffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_03_ADMIN_STAFF, description = "Quản trị nhân sự — trang `/admin/staff`")
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(operationId = "03-01-stats", summary = "[READ] Thống kê Staff & Performance")
    public ResponseEntity<StaffSummaryStatsResponse> stats() {
        return ResponseEntity.ok(staffService.getSummaryStats());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(
            operationId = "03-02-list",
            summary = "[READ] Danh sách nhân viên (phân trang)",
            description = "Query: `status` (ACTIVE|INACTIVE), `keyword`, `page`, `size`"
    )
    public ResponseEntity<PageResponse<StaffResponse>> list(
            @Parameter(description = "ACTIVE hoặc INACTIVE") @RequestParam(required = false) String status,
            @Parameter(description = "Tìm theo tên, username, email, SĐT") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(staffService.listStaff(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(operationId = "03-03-detail", summary = "[READ] Chi tiết nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy staffId")
    })
    public ResponseEntity<StaffResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            operationId = "03-04-create",
            summary = "[CREATE] Tạo tài khoản nhân viên",
            description = "Admin provisioning — trả `temporaryPassword`. Mật khẩu tạm: `Welcome@2026`"
    )
    public ResponseEntity<CreateStaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.provisionStaff(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(operationId = "03-05-update", summary = "[UPDATE] Cập nhật thông tin nhân viên")
    public ResponseEntity<StaffResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(operationId = "03-06-update-account-status", summary = "[UPDATE] Khóa / mở khóa tài khoản (ACTIVE ↔ INACTIVE)")
    public ResponseEntity<StaffResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/{id}/work-status")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(operationId = "03-07-update-work-status", summary = "[UPDATE] Trạng thái ca (IDLE / BUSY / ON_BREAK / OFF)")
    public ResponseEntity<StaffResponse> updateWorkStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffWorkStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateWorkStatus(id, request.getWorkStatus()));
    }

    @PutMapping("/{staffId}/roles")
    @PreAuthorize("hasAuthority('ASSIGN_ROLE')")
    @Operation(
            operationId = "03-08-assign-roles",
            summary = "[UPDATE] Gán vai trò cho nhân viên",
            description = "Ghi đè toàn bộ `roleIds`. Lấy role từ tag **06 - Roles** → `GET /api/v1/roles`"
    )
    public ResponseEntity<StaffResponse> assignRoles(
            @PathVariable Long staffId,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        return ResponseEntity.ok(staffService.assignRoles(staffId, request));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            operationId = "03-09-reset-password",
            summary = "[ACTION] Admin reset mật khẩu nhân viên",
            description = "Reset về `AutoWash@2026`, bật `requirePasswordChange`"
    )
    public ResponseEntity<CreateStaffResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.resetPassword(id));
    }
}
