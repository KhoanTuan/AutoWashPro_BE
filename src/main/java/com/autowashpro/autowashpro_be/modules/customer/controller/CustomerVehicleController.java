package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.VehicleResponse;
import com.autowashpro.autowashpro_be.modules.customer.service.VehicleService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Vehicle Management", description = "Quản lý Garage xe (biển số, dòng xe) cho App Khách hàng")
public class CustomerVehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách xe trong Garage cá nhân")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(vehicleService.getVehiclesByCustomerId(principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Thêm xe mới vào Garage cá nhân")
    public ResponseEntity<VehicleResponse> addVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VehicleRequest request) {
        requireAuthenticated(principal);
        VehicleResponse response = vehicleService.addVehicle(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin xe trong Garage")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long vehicleId,
            @Valid @RequestBody VehicleRequest request) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(vehicleService.updateVehicle(principal.getId(), vehicleId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa xe khỏi Garage cá nhân")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long vehicleId) {
        requireAuthenticated(principal);
        vehicleService.deleteVehicle(principal.getId(), vehicleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/set-default")
    @Operation(summary = "Đặt xe làm mặc định")
    public ResponseEntity<Void> setDefaultVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long vehicleId) {
        requireAuthenticated(principal);
        vehicleService.setDefaultVehicle(principal.getId(), vehicleId);
        return ResponseEntity.ok().build();
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Vui lòng đăng nhập để sử dụng chức năng quản lý xe!");
        }
    }
}
