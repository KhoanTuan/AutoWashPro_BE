package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.common.dto.PageResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.*;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
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

import static com.autowashpro.autowashpro_be.config.OpenApiConfig.TAG_05_ADMIN_BOOKING;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = TAG_05_ADMIN_BOOKING, description = "Flow 1 booking — `/admin/bookings` + Quick Booking modal")
public class AdminBookingController {

    private final BookingService bookingService;

    // ── Catalog & Read ──────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-01-stats", summary = "[READ] Thống kê booking hôm nay")
    public ResponseEntity<BookingSummaryStatsResponse> stats() {
        return ResponseEntity.ok(bookingService.getSummaryStats());
    }

    @GetMapping("/catalog/services")
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-02-catalog-services", summary = "[CATALOG] Danh mục dịch vụ (Quick Booking form)")
    public ResponseEntity<List<ServiceCatalogResponse>> services() {
        return ResponseEntity.ok(bookingService.listServices());
    }

    @GetMapping("/catalog/slots")
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-03-catalog-slots", summary = "[CATALOG] Khung giờ (Quick Booking form)")
    public ResponseEntity<List<SlotOptionResponse>> slots() {
        return ResponseEntity.ok(bookingService.listSlots());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-04-list", summary = "[READ] Danh sách booking (phân trang)")
    public ResponseEntity<PageResponse<BookingResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(bookingService.listBookings(status, date, keyword, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-05-detail", summary = "[READ] Chi tiết booking")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    // ── Create ──────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(
            operationId = "05-06-create",
            summary = "[CREATE] Tạo booking (walk-in / đặt lịch)",
            description = "Trạng thái ban đầu: `PENDING_PAYMENT` + `UNPAID`"
    )
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    // ── Flow 1 actions (theo thứ tự nghiệp vụ) ─────────────────────

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('CASHIER_CHECKIN')")
    @Operation(operationId = "05-07-pay", summary = "[FLOW-1] Checkout — UNPAID → PAID")
    public ResponseEntity<BookingResponse> pay(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.markPaid(id));
    }

    @PatchMapping("/{id}/assign-staff")
    @PreAuthorize("hasAuthority('CREATE_WALK_IN_BOOKING')")
    @Operation(operationId = "05-08-assign-staff", summary = "[FLOW-1] Gán kỹ thuật viên (yêu cầu PAID)")
    public ResponseEntity<BookingResponse> assignStaff(
            @PathVariable Long id,
            @Valid @RequestBody AssignStaffRequest request
    ) {
        return ResponseEntity.ok(bookingService.assignStaff(id, request.getTechnicianId()));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(operationId = "05-09-accept", summary = "[FLOW-1] Accept → CONFIRMED")
    public ResponseEntity<BookingResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.accept(id));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(operationId = "05-10-start", summary = "[FLOW-1] Bắt đầu rửa → PROCESSING")
    public ResponseEntity<BookingResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.startProcessing(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TASK_CHECKLIST')")
    @Operation(operationId = "05-11-complete", summary = "[FLOW-1] Hoàn thành → COMPLETED")
    public ResponseEntity<BookingResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.complete(id));
    }
}
