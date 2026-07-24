package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotRequest;
import com.autowashpro.autowashpro_be.modules.booking.dto.TimeSlotResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.TimeSlotService;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
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
public class AdminTimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.VIEW_SERVICES + "') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    public ResponseEntity<List<TimeSlotResponse>> getAllSlots(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(timeSlotService.getAllSlots(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<TimeSlotResponse> createSlot(@Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createSlot(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<TimeSlotResponse> updateSlot(
            @PathVariable Long id,
            @Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.ok(timeSlotService.updateSlot(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOTS + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<TimeSlotResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(timeSlotService.toggleStatus(id));
    }
}
