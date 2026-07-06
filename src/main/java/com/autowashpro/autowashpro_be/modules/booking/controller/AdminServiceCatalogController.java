package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.ServiceCatalogResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.ServiceCatalogService;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
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
public class AdminServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICE_CATALOG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<ServiceCatalogResponse>> getAllServices(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(serviceCatalogService.getAllServices(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICE_CATALOG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ServiceCatalogResponse> createService(@Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.createService(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICE_CATALOG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ServiceCatalogResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SERVICE_CATALOG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ServiceCatalogResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.toggleStatus(id));
    }
}
