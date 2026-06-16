package com.autowashpro.autowashpro_be.modules.identity.controller;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    public ResponseEntity<PageResponse<StaffResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(staffService.listStaff(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    public ResponseEntity<StaffResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_STAFF')")
    public ResponseEntity<StaffResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStaffStatusRequest request
    ) {
        return ResponseEntity.ok(staffService.updateStatus(id, request.getStatus()));
    }

    @PutMapping("/{staffId}/roles")
    @PreAuthorize("hasAuthority('ASSIGN_ROLE')")
    public ResponseEntity<StaffResponse> assignRoles(
            @PathVariable Long staffId,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        return ResponseEntity.ok(staffService.assignRoles(staffId, request));
    }
}
