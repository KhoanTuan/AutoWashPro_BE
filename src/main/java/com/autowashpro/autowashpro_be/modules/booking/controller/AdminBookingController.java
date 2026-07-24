package com.autowashpro.autowashpro_be.modules.booking.controller;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.SlotOccupancyResponse;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Booking Management", description = "Các API quản lý và tiếp tiếp đón tại quầy dành cho thu ngân/admin")
public class AdminBookingController {

    private final BookingService bookingService;

    @Operation(summary = "Tìm kiếm đặt lịch", description = "Tìm kiếm đơn chéo ngày theo query (mã đơn, SĐT, biển số) hoặc lấy danh sách theo ngày nếu query trống")
    @GetMapping("/bookings/search")
    @PreAuthorize("hasAuthority('VIEW_BOOKINGS') or hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<BookingResponse>> searchBookings(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.searchBookingsForAdmin(query, date));
    }

    @Operation(summary = "Lấy danh sách đặt lịch cho Admin/POS", description = "Lấy danh sách đơn hàng theo ngày và/hoặc trạng thái cho giao diện quản lý quầy POS")
    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('VIEW_BOOKINGS') or hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<BookingResponse>> getBookings(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(bookingService.getBookingsForAdmin(date, status));
    }

    @Operation(summary = "Khôi phục check-in trễ", description = "Khôi phục và chuyển trạng thái đơn hàng bị trễ (No-Show) sang IN_PROGRESS nếu slot chưa quá giờ kết thúc và còn chỗ trống")
    @PostMapping("/bookings/{id}/checkin-late")
    @PreAuthorize("hasAuthority('CHECKIN_LATE') or hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<BookingResponse> checkinLate(@PathVariable("id") String id) {
        return ResponseEntity.ok(bookingService.checkinLate(id));
    }

    @Operation(summary = "Giám sát công suất slot trong ngày", description = "Lấy thông tin số lượng đơn đã đặt và số lượng slot đã khóa thủ công theo từng khung giờ trong ngày")
    @GetMapping("/slots/occupancy-monitor")
    @PreAuthorize("hasAuthority('VIEW_BOOKINGS') or hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<SlotOccupancyResponse>> getOccupancyMonitor(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getOccupancyMonitor(date));
    }

    @Operation(summary = "Điều chỉnh khóa slot thủ công (ON/OFF)", description = "Quản trị viên / Quản lý khóa hoặc mở khóa toàn bộ công suất trống còn lại của slot")
    @PostMapping("/slots/{id}/lock")
    @PreAuthorize("hasAuthority('LOCK_SLOT') or hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SlotOccupancyResponse> adjustLock(
            @PathVariable("id") Long slotId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("lock") boolean lock) {
        return ResponseEntity.ok(bookingService.adjustLock(date, slotId, lock));
    }

    @Operation(summary = "Lấy danh sách tất cả các slot đang bị khóa", description = "Lấy tất cả các slot đang bị khóa thủ công")
    @GetMapping("/slots/locks")
    @PreAuthorize("hasAuthority('LOCK_SLOT') or hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<com.autowashpro.autowashpro_be.modules.booking.dto.SlotLockResponse>> getAllSlotLocks() {
        return ResponseEntity.ok(bookingService.getAllSlotLocks());
    }
}
