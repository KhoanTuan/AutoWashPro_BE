package com.autowashpro.autowashpro_be.modules.notification.listener;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.event.FeedbackEvent;
import com.autowashpro.autowashpro_be.modules.marketing.event.PromotionEvent;
import com.autowashpro.autowashpro_be.modules.marketing.event.VoucherRedemptionEvent;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationRecipientType;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingEventListener {

    private final RealtimeNotificationService notificationService;
    private final CustomerRepository customerRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromotionEvent(PromotionEvent event) {
        log.info("Received promotion event: action={}, code={}", event.getAction(), event.getPromoCode());
        try {
            if ("CREATED".equalsIgnoreCase(event.getAction())) {
                String title = "🎁 Ưu đãi mới: " + event.getPromotion().getName();
                String content = "Nhận ngay voucher " + event.getPromoCode() + " - " + event.getPromotion().getDescription();
                
                // 1. Lưu DB cho tất cả Khách hàng và gửi WebSocket
                List<Customer> customers = customerRepository.findAll();
                for (Customer c : customers) {
                    notificationService.saveNotification(
                            NotificationRecipientType.CUSTOMER,
                            c.getCustomerId(),
                            title,
                            content,
                            NotificationType.NEW_PROMOTION,
                            event.getPromoCode()
                    );
                    
                    notificationService.sendMarketingWsMessage(
                            "/topic/customer/" + c.getCustomerId() + "/notifications",
                            Map.of("type", "NEW_PROMOTION", "code", event.getPromoCode(), "title", title, "content", content)
                    );
                    
                    notificationService.sendMarketingWsMessageToUser(
                            "CUSTOMER:" + c.getCustomerId(),
                            "/queue/notifications",
                            Map.of("type", "NEW_PROMOTION", "code", event.getPromoCode(), "title", title, "content", content)
                    );
                }

                // 2. Lưu DB cho Nhân viên
                notificationService.saveNotification(
                        NotificationRecipientType.ALL_STAFF,
                        null,
                        "📢 Khởi tạo khuyến mãi mới: " + event.getPromoCode(),
                        content,
                        NotificationType.NEW_PROMOTION,
                        event.getPromoCode()
                );
                
                notificationService.sendMarketingWsMessage(
                        "/topic/admin/bookings",
                        Map.of("type", "NEW_PROMOTION", "code", event.getPromoCode(), "title", "📢 Khởi tạo khuyến mãi mới: " + event.getPromoCode(), "content", content)
                );
            }
        } catch (Exception e) {
            log.error("Failed to process promotion event notification", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedbackEvent(FeedbackEvent event) {
        log.info("Received feedback event: action={}, bookingCode={}", event.getAction(), event.getBookingCode());
        try {
            if ("CREATED".equalsIgnoreCase(event.getAction())) {
                // Khách hàng gửi Feedback -> Thông báo cho Staff
                String title = "⭐ Phản hồi mới từ khách hàng!";
                String content = String.format("Khách hàng %s đánh giá %d sao cho đơn %s: %s",
                        event.getFeedback().getCustomer().getFullName(),
                        event.getFeedback().getRatingStars(),
                        event.getBookingCode(),
                        event.getFeedback().getComment() != null ? event.getFeedback().getComment() : "");

                notificationService.saveNotification(
                        NotificationRecipientType.ALL_STAFF,
                        null,
                        title,
                        content,
                        NotificationType.NEW_FEEDBACK,
                        event.getBookingCode()
                );

                notificationService.sendMarketingWsMessage(
                        "/topic/admin/bookings",
                        Map.of("type", "NEW_FEEDBACK", "code", event.getBookingCode(), "title", title, "content", content)
                );
            } else if ("REPLIED".equalsIgnoreCase(event.getAction())) {
                // Admin phản hồi Feedback -> Thông báo cho Khách hàng chính chủ
                Customer c = event.getFeedback().getCustomer();
                String title = "💬 Phản hồi đánh giá của bạn";
                String content = "Cảm ơn bạn đã đóng góp ý kiến. Phản hồi giải quyết: " + 
                        (event.getFeedback().getResolutionNotes() != null ? event.getFeedback().getResolutionNotes() : "");

                notificationService.saveNotification(
                        NotificationRecipientType.CUSTOMER,
                        c.getCustomerId(),
                        title,
                        content,
                        NotificationType.FEEDBACK_REPLIED,
                        event.getBookingCode()
                );

                notificationService.sendMarketingWsMessage(
                        "/topic/customer/" + c.getCustomerId() + "/notifications",
                        Map.of("type", "FEEDBACK_REPLIED", "code", event.getBookingCode(), "title", title, "content", content)
                );

                notificationService.sendMarketingWsMessageToUser(
                        "CUSTOMER:" + c.getCustomerId(),
                        "/queue/notifications",
                        Map.of("type", "FEEDBACK_REPLIED", "code", event.getBookingCode(), "title", title, "content", content)
                );
            }
        } catch (Exception e) {
            log.error("Failed to process feedback event notification", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVoucherRedemptionEvent(VoucherRedemptionEvent event) {
        log.info("Received voucher redemption event: action={}, voucherCode={}", event.getAction(), event.getVoucherCode());
        try {
            Customer c = event.getCustomerPromotion().getCustomer();
            String title = "🎁 Đổi voucher thành công!";
            String content = String.format("Bạn đã nhận thành công mã voucher %s từ ưu đãi %s",
                    event.getVoucherCode(),
                    event.getCustomerPromotion().getPromotion().getName());

            // 1. Lưu DB & WebSocket cho Customer
            notificationService.saveNotification(
                    NotificationRecipientType.CUSTOMER,
                    c.getCustomerId(),
                    title,
                    content,
                    NotificationType.VOUCHER_REDEMPTION,
                    event.getVoucherCode()
            );

            notificationService.sendMarketingWsMessage(
                    "/topic/customer/" + c.getCustomerId() + "/notifications",
                    Map.of("type", "VOUCHER_REDEMPTION", "code", event.getVoucherCode(), "title", title, "content", content)
            );

            notificationService.sendMarketingWsMessageToUser(
                    "CUSTOMER:" + c.getCustomerId(),
                    "/queue/notifications",
                    Map.of("type", "VOUCHER_REDEMPTION", "code", event.getVoucherCode(), "title", title, "content", content)
            );

            // 2. Lưu DB & WebSocket cho Staff
            String staffTitle = "🎁 Khách hàng đổi quà tặng";
            String staffContent = String.format("Khách hàng %s đã đổi thành công voucher %s",
                    c.getFullName(),
                    event.getVoucherCode());

            notificationService.saveNotification(
                    NotificationRecipientType.ALL_STAFF,
                    null,
                    staffTitle,
                    staffContent,
                    NotificationType.VOUCHER_REDEMPTION,
                    event.getVoucherCode()
            );

            notificationService.sendMarketingWsMessage(
                    "/topic/admin/bookings",
                    Map.of("type", "VOUCHER_REDEMPTION", "code", event.getVoucherCode(), "title", staffTitle, "content", staffContent)
            );
        } catch (Exception e) {
            log.error("Failed to process voucher redemption event notification", e);
        }
    }
}
