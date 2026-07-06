package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.booking.entity.GarageClosure;
import com.autowashpro.autowashpro_be.modules.booking.repository.GarageClosureRepository;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/closures")
@RequiredArgsConstructor
@Tag(name = "Admin Garage Closures", description = "Quản lý Lịch Nghỉ Lễ / Ngoại Lệ của xưởng")
public class AdminGarageClosureController {

    private final GarageClosureRepository garageClosureRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOT_CONFIG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Lấy danh sách các ngày xưởng nghỉ lễ")
    public ResponseEntity<List<GarageClosure>> getAllClosures() {
        return ResponseEntity.ok(garageClosureRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOT_CONFIG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Thêm ngày nghỉ lễ mới")
    public ResponseEntity<GarageClosure> createClosure(@RequestBody CreateClosureRequest request) {
        if (request.getClosureDate() == null) {
            throw new BadRequestException("Closure date must not be null");
        }
        if (garageClosureRepository.existsByClosureDate(request.getClosureDate())) {
            throw new BadRequestException("Date " + request.getClosureDate() + " is already marked as closed");
        }
        GarageClosure closure = GarageClosure.builder()
                .closureDate(request.getClosureDate())
                .reason(request.getReason())
                .isFullDay(request.getIsFullDay() != null ? request.getIsFullDay() : true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(garageClosureRepository.save(closure));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.MANAGE_SLOT_CONFIG + "') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Xóa lịch nghỉ lễ (mở cửa trở lại)")
    public ResponseEntity<Void> deleteClosure(@PathVariable Long id) {
        if (!garageClosureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Garage closure not found with id: " + id);
        }
        garageClosureRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateClosureRequest {
        private LocalDate closureDate;
        private String reason;
        private Boolean isFullDay;
    }
}
