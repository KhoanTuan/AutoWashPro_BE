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
@RequestMapping("/api/v1/customer/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Notification & Bell Icon", description = "Quản lý thông báo 2 chiều & lịch sử chuông báo cho Khách hàng")
public class CustomerNotificationController {

    private final RealtimeNotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch sử thông báo (cho pop-up chuông 🔔)", description = "Mặc định trả về 20 thông báo mới nhất")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        requireCustomer(principal);
        return ResponseEntity.ok(notificationService.getCustomerNotifications(principal.getId(), limit));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc (để hiển thị huy hiệu đỏ trên chuông)")
    public ResponseEntity<NotificationStatsResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        requireCustomer(principal);
        return ResponseEntity.ok(notificationService.getCustomerUnreadCount(principal.getId()));
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã xem (khi khách nhấn mở danh sách chuông)")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        requireCustomer(principal);
        notificationService.markAllAsReadForCustomer(principal.getId());
        return ResponseEntity.ok().build();
    }

    private void requireCustomer(UserPrincipal principal) {
        if (principal == null || principal.getUserType() != UserPrincipal.UserType.CUSTOMER) {
            throw new BadRequestException("Vui lòng đăng nhập với tài khoản Khách hàng");
        }
    }
}
