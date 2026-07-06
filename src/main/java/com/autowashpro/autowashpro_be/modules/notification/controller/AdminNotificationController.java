package com.autowashpro.autowashpro_be.modules.notification.controller;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.notification.dto.NotificationResponse;
import com.autowashpro.autowashpro_be.modules.notification.dto.NotificationStatsResponse;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import com.autowashpro.autowashpro_be.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
@Tag(name = "Admin Notification & Bell Icon", description = "Quản lý thông báo thời gian thực & chuông báo trên máy POS cho Staff/Manager")
public class AdminNotificationController {

    private final RealtimeNotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo cho Staff/POS", description = "Mặc định trả về 20 thông báo mới nhất")
    public ResponseEntity<List<NotificationResponse>> getStaffNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        requireStaff(principal);
        return ResponseEntity.ok(notificationService.getStaffNotifications(principal.getId(), limit));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc của quầy POS/Staff")
    public ResponseEntity<NotificationStatsResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        requireStaff(principal);
        return ResponseEntity.ok(notificationService.getStaffUnreadCount(principal.getId()));
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Đánh dấu tất cả thông báo POS là đã xem")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        requireStaff(principal);
        notificationService.markAllAsReadForStaff(principal.getId());
        return ResponseEntity.ok().build();
    }

    private void requireStaff(UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.STAFF) {
            throw new BadRequestException("Vui lòng đăng nhập với tài khoản Nhân viên/Quản lý");
        }
    }
}
