package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.ServiceCatalogService;
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
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "03 - Admin Service Catalog", description = "Quản lý danh mục Gói dịch vụ & Dịch vụ cộng thêm (Add-on) — trang `/admin/services-slots`")
public class AdminServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.VIEW_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    @Operation(summary = "[READ] Danh sách gói dịch vụ", description = "Lấy toàn bộ danh sách gói dịch vụ rửa xe và dịch vụ cộng thêm.")
    public ResponseEntity<List<ServiceCatalogResponse>> getAllServices(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(serviceCatalogService.getAllServices(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[CREATE] Tạo gói dịch vụ mới", description = "Thêm gói rửa hoặc dịch vụ bổ sung mới vào hệ thống DB.")
    public ResponseEntity<ServiceCatalogResponse> createService(@Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.createService(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[UPDATE] Cập nhật thông tin gói dịch vụ", description = "Sửa tên, giá tiền, thời lượng rửa hoặc mô tả gói dịch vụ.")
    public ResponseEntity<ServiceCatalogResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "[UPDATE] Bật / Tắt trạng thái hoạt động gói dịch vụ", description = "Đổi trạng thái Kích hoạt/Tạm dừng của gói dịch vụ.")
    public ResponseEntity<ServiceCatalogResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICES + "') or hasRole('ADMIN')")
    @Operation(summary = "[DELETE] Xóa vĩnh viễn gói dịch vụ", description = "Xóa vĩnh viễn gói dịch vụ khỏi hệ thống DB.")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceCatalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
