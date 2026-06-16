package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.modules.identity.dto.PermissionSummary;
import com.autowashpro.autowashpro_be.modules.identity.dto.RoleResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.UpdateRolePermissionsRequest;
import com.autowashpro.autowashpro_be.modules.identity.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    public ResponseEntity<List<PermissionSummary>> getPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('CONFIG_RBAC_MATRIX')")
    public ResponseEntity<RoleResponse> updatePermissions(
            @PathVariable Integer roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(roleService.updateRolePermissions(roleId, request));
    }
}
