package com.autowashpro.autowashpro_be.modules.notification.listener;

import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.SlotCapacityChangeEvent;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final RealtimeNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingEvent(BookingEvent event) {
        log.info("Received booking event action: {} for booking code: {}", event.getAction(), event.getBooking().getBookingCode());
        try {
            NotificationType type = mapToNotificationType(event.getAction());
            if (type == NotificationType.NEW_BOOKING) {
                notificationService.notifyNewBooking(event.getBooking());
            } else {
                String title = event.getCustomTitle();
                String content = event.getCustomContent();
                
                if (title == null) {
                    title = getGenericTitle(event.getAction());
                }
                if (content == null) {
                    content = getGenericContent(event);
                }

                notificationService.notifyBookingStatusChanged(
                        event.getBooking(),
                        type,
                        title,
                        content
                );
            }
        } catch (Exception e) {
            log.error("Failed to process booking event notification", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSlotCapacityChangeEvent(SlotCapacityChangeEvent event) {
        log.info("Received slot capacity change event for date: {}, slotId: {}", event.getDate(), event.getSlotId());
        try {
            notificationService.broadcastSlotCapacityChange(event.getDate(), event.getSlotId());
        } catch (Exception e) {
            log.error("Failed to broadcast slot capacity change", e);
        }
    }

    private NotificationType mapToNotificationType(com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction action) {
        switch (action) {
            case CREATED:
                return NotificationType.NEW_BOOKING;
            case CANCELLED:
                return NotificationType.BOOKING_CANCELLED;
            case CHECKED_IN:
                return NotificationType.BOOKING_CHECKED_IN;
            case COMPLETED:
                return NotificationType.BOOKING_COMPLETED;
            default:
                return NotificationType.SYSTEM_ALERT;
        }
    }

    private String getGenericTitle(com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction action) {
        switch (action) {
            case CANCELLED:
                return "Lịch hẹn đã bị hủy";
            case CHECKED_IN:
                return "Khách hàng đã check-in";
            case COMPLETED:
                return "Đơn hàng hoàn tất";
            default:
                return "Cập nhật lịch đặt";
        }
    }

    private String getGenericContent(BookingEvent event) {
        return "Lịch hẹn " + event.getBooking().getBookingCode() + " đã chuyển sang trạng thái " + event.getBooking().getStatus();
    }
}
