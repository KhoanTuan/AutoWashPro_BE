package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.TimeSlotService;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/slots")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "04 - Admin Time Slots & Capacity", description = "Quản lý khung giờ hoạt động, công suất rửa xe & đóng mở khung giờ khẩn cấp — trang `/admin/services-slots`")
public class AdminTimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.VIEW_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "[READ] Danh sách khung giờ hoạt động", description = "Lấy toàn bộ danh sách các khung giờ và công suất rửa xe tối đa.")
    public ResponseEntity<List<TimeSlotResponse>> getAllSlots(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(timeSlotService.getAllSlots(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[CREATE] Tạo mới khung giờ hoạt động", description = "Thêm mốc giờ phục vụ mới và thiết lập công suất chứa tối đa.")
    public ResponseEntity<TimeSlotResponse> createSlot(@Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createSlot(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[UPDATE] Cập nhật thông tin khung giờ", description = "Thay đổi mốc giờ bắt đầu/kết thúc và công suất tối đa của khung giờ.")
    public ResponseEntity<TimeSlotResponse> updateSlot(
            @PathVariable Long id,
            @Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.ok(timeSlotService.updateSlot(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[UPDATE] Đóng / Mở hoạt động khung giờ", description = "Đổi trạng thái Kích hoạt/Bảo trì của khung giờ.")
    public ResponseEntity<TimeSlotResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(timeSlotService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN')")
    @Operation(summary = "[DELETE] Xóa vĩnh viễn khung giờ", description = "Xóa vĩnh viễn khung giờ khỏi hệ thống DB.")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        timeSlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
