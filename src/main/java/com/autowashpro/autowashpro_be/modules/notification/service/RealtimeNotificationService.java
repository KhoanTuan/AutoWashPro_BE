package com.autowashpro.autowashpro_be.modules.notification.service;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.SlotLockRepository;
import com.autowashpro.autowashpro_be.modules.booking.entity.SlotLock;
import com.autowashpro.autowashpro_be.modules.notification.dto.*;
import com.autowashpro.autowashpro_be.modules.notification.entity.Notification;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationRecipientType;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service("realtimeNotificationService")
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final NotificationRepository notificationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final SlotLockRepository slotLockRepository;
    private final Optional<SimpMessagingTemplate> messagingTemplate;

    private static final List<BookingStatus> ACTIVE_CAPACITY_STATUSES = Arrays.asList(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyNewBooking(Booking booking) {
        String slotTimeStr = formatSlotTime(booking.getTimeSlot());
        String dateStr = booking.getBookingDate().format(DATE_FORMATTER);
        String customerName = booking.getCustomer().getFullName();

        // 1. Lưu DB cho ALL_STAFF (Admin/Manager/Cashier)
        String staffTitle = "🎉 Đơn đặt lịch mới!";
        String staffContent = String.format("Khách %s đặt khung %s ngày %s (Biển số: %s)",
                customerName, slotTimeStr, dateStr, booking.getLicensePlate());
        saveNotification(NotificationRecipientType.ALL_STAFF, null, staffTitle, staffContent,
                NotificationType.NEW_BOOKING, booking.getBookingCode());

        // 2. Lưu DB cho CUSTOMER (Chính chủ)
        String cusTitle = "🎉 Đặt lịch thành công!";
        String cusContent = String.format("Mã đơn %s cho khung giờ %s ngày %s đã được ghi nhận. Vui lòng đến đúng giờ!",
                booking.getBookingCode(), slotTimeStr, dateStr);
        saveNotification(NotificationRecipientType.CUSTOMER, booking.getCustomer().getCustomerId(), cusTitle, cusContent,
                NotificationType.NEW_BOOKING, booking.getBookingCode());

        // 3. Bắn WebSocket Real-time 2 chiều
        sendBookingWsMessage("NEW_BOOKING", booking, staffTitle, staffContent, cusTitle, cusContent);

        // 4. Phát sóng cập nhật số chỗ trống (Slot capacity broadcast)
        broadcastSlotCapacityChange(booking.getBookingDate(), booking.getTimeSlot().getSlotId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyBookingStatusChanged(Booking booking, NotificationType type, String title, String content) {
        // 1. Lưu DB cho CUSTOMER
        saveNotification(NotificationRecipientType.CUSTOMER, booking.getCustomer().getCustomerId(),
                title, content, type, booking.getBookingCode());

        // 2. Lưu DB cho STAFF
        saveNotification(NotificationRecipientType.ALL_STAFF, null,
                "Lịch hẹn " + booking.getBookingCode() + ": " + title, content, type, booking.getBookingCode());

        // 3. Bắn WebSocket 2 chiều
        sendBookingWsMessage(type.name(), booking,
                "Lịch hẹn " + booking.getBookingCode() + ": " + title, content, title, content);

        // 4. Phát sóng số chỗ trống mới
        broadcastSlotCapacityChange(booking.getBookingDate(), booking.getTimeSlot().getSlotId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyGeneral(Long customerId, String title, String content, NotificationType type) {
        // 1. Lưu DB cho CUSTOMER
        saveNotification(NotificationRecipientType.CUSTOMER, customerId, title, content, type, null);
        
        // 2. Bắn WebSocket cho CUSTOMER
        try {
            if (messagingTemplate.isPresent()) {
                WsBookingMessage cusMsg = WsBookingMessage.builder()
                        .type(type.name())
                        .customerId(customerId)
                        .title(title)
                        .content(content)
                        .build();
                messagingTemplate.get().convertAndSend("/topic/customer/" + customerId + "/notifications", (Object) cusMsg);
            }
        } catch (Exception e) {
            log.warn("Failed to send WebSocket general notification: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void broadcastSlotCapacityChange(LocalDate date, Long slotId) {
        try {
            if (messagingTemplate.isPresent()) {
                TimeSlot slot = timeSlotRepository.findById(slotId).orElse(null);
                if (slot != null) {
                    int lockedCount = slotLockRepository.findByLockDateAndTimeSlotSlotId(date, slotId)
                            .map(SlotLock::getLockCount)
                            .orElse(0);
                    int bookedCount = bookingRepository.countByBookingDateAndTimeSlotSlotIdAndStatusIn(
                            date, slotId, ACTIVE_CAPACITY_STATUSES);
                    int availableCapacity = Math.max(0, slot.getMaxCapacity() - bookedCount - lockedCount);
                    boolean isFull = availableCapacity <= 0;

                    WsSlotCapacityMessage payload = WsSlotCapacityMessage.builder()
                            .type("SLOT_CAPACITY_CHANGED")
                            .date(date.toString())
                            .timeSlotId(slotId)
                            .availableCapacity(availableCapacity)
                            .isFull(isFull)
                            .build();

                    messagingTemplate.get().convertAndSend("/topic/public/slots", (Object) payload);
                    log.info("Broadcasted slot capacity change: slotId={}, date={}, avail={}, isFull={}",
                            slotId, date, availableCapacity, isFull);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast slot capacity: {}", e.getMessage());
        }
    }

    private void sendBookingWsMessage(String eventType, Booking booking,
                                      String staffTitle, String staffContent,
                                      String cusTitle, String cusContent) {
        try {
            if (messagingTemplate.isPresent()) {
                String slotTimeStr = formatSlotTime(booking.getTimeSlot());

                // Bắn cho Admin/POS (/topic/admin/bookings)
                WsBookingMessage adminMsg = WsBookingMessage.builder()
                        .type(eventType)
                        .bookingCode(booking.getBookingCode())
                        .customerId(booking.getCustomer().getCustomerId())
                        .customerName(booking.getCustomer().getFullName())
                        .licensePlate(booking.getLicensePlate())
                        .slotTime(slotTimeStr)
                        .date(booking.getBookingDate().toString())
                        .status(booking.getStatus().name())
                        .title(staffTitle)
                        .content(staffContent)
                        .build();
                messagingTemplate.get().convertAndSend("/topic/admin/bookings", (Object) adminMsg);

                // Bắn cho Customer (/topic/customer/{id}/notifications và user destination)
                WsBookingMessage cusMsg = WsBookingMessage.builder()
                        .type(eventType)
                        .bookingCode(booking.getBookingCode())
                        .customerId(booking.getCustomer().getCustomerId())
                        .customerName(booking.getCustomer().getFullName())
                        .licensePlate(booking.getLicensePlate())
                        .slotTime(slotTimeStr)
                        .date(booking.getBookingDate().toString())
                        .status(booking.getStatus().name())
                        .title(cusTitle)
                        .content(cusContent)
                        .build();
                messagingTemplate.get().convertAndSend("/topic/customer/" + booking.getCustomer().getCustomerId() + "/notifications", (Object) cusMsg);
                messagingTemplate.get().convertAndSendToUser("CUSTOMER:" + booking.getCustomer().getCustomerId(), "/queue/notifications", (Object) cusMsg);

                log.info("Sent bi-directional WS notification for booking {}", booking.getBookingCode());
            }
        } catch (Exception e) {
            log.warn("Failed to send WebSocket booking notification: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNotification(NotificationRecipientType recipientType, Long recipientId,
                                  String title, String content, NotificationType type, String refCode) {
        try {
            String safeTitle = title != null && title.length() > 150 ? title.substring(0, 150) : title;
            String safeContent = content != null && content.length() > 180 ? content.substring(0, 180) : content;
            String safeRefCode = refCode != null && refCode.length() > 45 ? refCode.substring(0, 45) : refCode;

            Notification notification = Notification.builder()
                    .recipientType(recipientType)
                    .recipientId(recipientId)
                    .title(safeTitle)
                    .content(safeContent)
                    .type(type)
                    .referenceCode(safeRefCode)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Error saving notification to DB: {}", e.getMessage());
        }
    }

    public void sendMarketingWsMessage(String destination, Object payload) {
        try {
            if (messagingTemplate.isPresent()) {
                messagingTemplate.get().convertAndSend(destination, payload);
            }
        } catch (Exception e) {
            log.warn("Failed to send marketing WebSocket message: {}", e.getMessage());
        }
    }

    public void sendMarketingWsMessageToUser(String username, String destination, Object payload) {
        try {
            if (messagingTemplate.isPresent()) {
                messagingTemplate.get().convertAndSendToUser(username, destination, payload);
            }
        } catch (Exception e) {
            log.warn("Failed to send marketing WebSocket message to user: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCustomerNotifications(Long customerId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findForCustomer(customerId, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getCustomerUnreadCount(Long customerId) {
        return NotificationStatsResponse.builder()
                .unreadCount(notificationRepository.countUnreadForCustomer(customerId))
                .build();
    }

    @Transactional
    public void markAllAsReadForCustomer(Long customerId) {
        notificationRepository.markAllAsReadForCustomer(customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getStaffNotifications(Long staffId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findForStaff(staffId, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStaffUnreadCount(Long staffId) {
        return NotificationStatsResponse.builder()
                .unreadCount(notificationRepository.countUnreadForStaff(staffId))
                .build();
    }

    @Transactional
    public void markAllAsReadForStaff(Long staffId) {
        notificationRepository.markAllAsReadForStaff(staffId);
    }

    private NotificationResponse mapToResponse(Notification entity) {
        return NotificationResponse.builder()
                .notificationId(entity.getNotificationId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .referenceCode(entity.getReferenceCode())
                .isRead(entity.getIsRead())
                .createdAtFormatted(entity.getCreatedAt() != null ? entity.getCreatedAt().format(DATETIME_FORMATTER) : "")
                .build();
    }

    private String formatSlotTime(TimeSlot slot) {
        if (slot == null || slot.getStartTime() == null || slot.getEndTime() == null) return "";
        return slot.getStartTime().format(TIME_FORMATTER) + " - " + slot.getEndTime().format(TIME_FORMATTER);
    }
}
