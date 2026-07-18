package com.autowashpro.autowashpro_be.modules.customer.controller;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.common.openapi.ApiHidden;
import com.autowashpro.autowashpro_be.modules.customer.dto.*;
import com.autowashpro.autowashpro_be.modules.customer.service.CustomerAdminService;
import com.autowashpro.autowashpro_be.modules.customer.service.LoyaltyService;
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

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_04_ADMIN_CUSTOMER;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_04_ADMIN_CUSTOMER, description = "Quản trị khách hàng — trang `/admin/customers`")
public class AdminCustomerController {

    private final CustomerAdminService customerAdminService;
    private final LoyaltyService loyaltyService;

    @GetMapping("/stats")
    @ApiHidden
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER_PROFILE')")
    @Operation(operationId = "04-01-stats", summary = "[READ] Thống kê khách hàng")
    public ResponseEntity<CustomerSummaryStatsResponse> stats() {
        return ResponseEntity.ok(customerAdminService.getSummaryStats());
    }

    @GetMapping("/options")
    @ApiHidden
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER_PROFILE')")
    @Operation(operationId = "04-02-options", summary = "[READ] Dropdown khách hàng (Quick Booking modal)")
    public ResponseEntity<List<CustomerOptionResponse>> options() {
        return ResponseEntity.ok(customerAdminService.listOptions());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER_PROFILE')")
    @Operation(operationId = "04-03-list", summary = "[READ] Danh sách khách hàng (phân trang + tìm kiếm)")
    public ResponseEntity<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(customerAdminService.listCustomers(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    @ApiHidden
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER_PROFILE')")
    @Operation(operationId = "04-04-detail", summary = "[READ] Chi tiết khách hàng")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerAdminService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_STATUS')")
    @Operation(operationId = "04-05-create", summary = "[CREATE] Tạo khách hàng (admin provisioning)")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAdminService.createCustomer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_STATUS')")
    @Operation(operationId = "04-06-update", summary = "[UPDATE] Cập nhật khách hàng")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(customerAdminService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/status")
    @ApiHidden
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_STATUS')")
    @Operation(operationId = "04-07-update-status", summary = "[UPDATE] Khóa / mở khóa tài khoản (ACTIVE ↔ INACTIVE)")
    public ResponseEntity<CustomerResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerStatusRequest request
    ) {
        return ResponseEntity.ok(customerAdminService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/{id}/points/history")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER_PROFILE')")
    @Operation(summary = "Xem lịch sử tích lũy và sử dụng điểm thưởng của khách hàng (CRM Detail)")
    public ResponseEntity<List<PointTransactionResponse>> getCustomerPointHistory(@PathVariable Long id) {
        return ResponseEntity.ok(loyaltyService.getCustomerPointHistory(id));
    }

    @PostMapping("/{id}/loyalty-profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Cập nhật hoặc khởi tạo thông tin loyalty của khách hàng (Admin/Staff)")
    public ResponseEntity<CustomerResponse> updateLoyaltyProfile(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateLoyaltyRequest request
    ) {
        return ResponseEntity.ok(customerAdminService.updateLoyaltyProfile(id, request));
    }
}
