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

@RestController
@RequestMapping("/api/v1/admin/staffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "03 - Admin Staff", description = "Quản trị nhân sự — trang `/internal/admin/staff`")
public class AdminStaffController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Tạo tài khoản nhân viên mới (Admin provisioning)",
            description = """
                    **Chỉ ROLE_ADMIN** — không có đăng ký công khai cho staff.

                    - Backend tự sinh mật khẩu tạm: `Welcome@2026`
                    - Set `requirePasswordChange = true` → nhân viên phải đổi mật khẩu khi login lần đầu
                    - Gửi email thông báo (dev: xem log console)
                    - Response trả `temporaryPassword` để Admin copy (chỉ lúc tạo)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo nhân viên thành công"),
            @ApiResponse(responseCode = "400", description = "Username hoặc email đã tồn tại"),
            @ApiResponse(responseCode = "403", description = "Không phải ROLE_ADMIN")
    })
    public ResponseEntity<CreateStaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.provisionStaff(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(
            summary = "Danh sách nhân viên (phân trang)",
            description = """
                    **Frontend:** Bảng quản lý nhân sự.

                    - Lọc theo `status`: ACTIVE | INACTIVE
                    - Tìm theo `keyword` trong username hoặc fullName
                    - Phân trang: `page` (0-based), `size`
                    """
    )
    public ResponseEntity<PageResponse<StaffResponse>> list(
            @Parameter(description = "Lọc trạng thái: ACTIVE hoặc INACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Tìm kiếm theo tên hoặc username")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(staffService.listStaff(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(summary = "Chi tiết một nhân viên", description = "Lấy thông tin staff theo `staffId`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tìm thấy nhân viên"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy staffId")
    })
    public ResponseEntity<StaffResponse> getById(
            @Parameter(description = "ID nhân viên", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    @Operation(
            summary = "Khóa / mở khóa tài khoản nhân viên",
            description = "Đổi trạng thái ACTIVE ↔ INACTIVE. Nhân viên INACTIVE không thể login."
    )
    public ResponseEntity<StaffResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStatus(id, request.getStatus()));
    }

    @PutMapping("/{staffId}/roles")
    @PreAuthorize("hasAuthority('ASSIGN_ROLE')")
    @Operation(
            summary = "Gán vai trò cho nhân viên",
            description = """
                    Ghi đè toàn bộ roles của nhân viên bằng danh sách `roleIds` mới.

                    - Lấy danh sách role từ `GET /api/v1/roles`
                    - Nhân viên thừa hưởng permissions theo role đã gán
                    """
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
            summary = "Admin reset mật khẩu nhân viên",
            description = """
                    **Luồng quên mật khẩu nội bộ** — nhân viên liên hệ Admin offline.

                    - Reset về mật khẩu mặc định: `AutoWash@2026`
                    - Set `requirePasswordChange = true`
                    - Nhân viên login lại sẽ bị buộc đổi mật khẩu (`forceChangePassword = true`)
                    """
    )
    public ResponseEntity<CreateStaffResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.resetPassword(id));
    }
}
