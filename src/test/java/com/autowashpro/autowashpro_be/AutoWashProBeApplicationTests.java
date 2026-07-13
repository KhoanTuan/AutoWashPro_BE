package com.autowashpro.autowashpro_be;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.CustomerFeedbackCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.FeedbackResolveRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.PromotionCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerFeedbackRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.PromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminFeedbackService;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminPromotionService;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerFeedbackService;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerRewardService;
import com.autowashpro.autowashpro_be.modules.notification.entity.Notification;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationRecipientType;
import com.autowashpro.autowashpro_be.modules.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutoWashProBeApplicationTests {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AdminPromotionService adminPromotionService;
    @Autowired
    private CustomerFeedbackService customerFeedbackService;
    @Autowired
    private AdminFeedbackService adminFeedbackService;
    @Autowired
    private CustomerRewardService customerRewardService;
    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private CustomerFeedbackRepository customerFeedbackRepository;
    @Autowired
    private CustomerPromotionRepository customerPromotionRepository;

    @Test
    void testBidirectionalNotifications() {
        System.out.println("====== START INTEGRATION TESTING FOR BI-DIRECTIONAL NOTIFICATIONS ======");

        // 1. Lấy dữ liệu mẫu
        Customer customer = customerRepository.findByPhoneNumber("0902000001")
                .orElseThrow(() -> new AssertionError("Seeded customer not found"));
        TimeSlot slot = timeSlotRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("No time slots available"));

        // Lấy số lượng thông báo ban đầu
        long initialStaffNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
        long initialCusNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientType() == NotificationRecipientType.CUSTOMER && customer.getCustomerId().equals(n.getRecipientId())).count();

        System.out.println("Initial notifications - Staff: " + initialStaffNotifs + ", Customer: " + initialCusNotifs);

        Long createdBookingId = null;
        Long createdPromoId = null;
        Long createdFeedbackId = null;
        int originalPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

        try {
            // ==========================================
            // TEST FLOW 1: TẠO ĐƠN ĐẶT LỊCH (CREATE BOOKING)
            // ==========================================
            CreateBookingRequest bookingReq = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(5)) // Ngày ở tương lai xa để không trùng
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L)
                    .notes("Test booking notification")
                    .build();

            BookingResponse bookingRes = bookingService.createBooking(bookingReq, customer);
            assertNotNull(bookingRes);
            createdBookingId = bookingRes.getBookingId();

            // Chờ một chút để event listener chạy và commit transaction của nó
            Thread.sleep(200);

            long staffNotifsAfterCreate = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
            long cusNotifsAfterCreate = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.CUSTOMER && customer.getCustomerId().equals(n.getRecipientId())).count();

            System.out.println("After Create Booking - Staff Notifs: " + staffNotifsAfterCreate + ", Customer Notifs: " + cusNotifsAfterCreate);
            assertTrue(staffNotifsAfterCreate > initialStaffNotifs, "Staff notification count should increase");
            assertTrue(cusNotifsAfterCreate > initialCusNotifs, "Customer notification count should increase");

            // ==========================================
            // TEST FLOW 2: THANH TOÁN HOÀN TẤT (CHECKOUT)
            // ==========================================
            BookingResponse checkoutRes = bookingService.completeCheckout(createdBookingId, "cash");
            assertNotNull(checkoutRes);
            assertEquals(BookingStatus.COMPLETED, checkoutRes.getStatus());

            Thread.sleep(200);

            long staffNotifsAfterCheckout = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
            long cusNotifsAfterCheckout = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.CUSTOMER && customer.getCustomerId().equals(n.getRecipientId())).count();

            System.out.println("After Checkout - Staff Notifs: " + staffNotifsAfterCheckout + ", Customer Notifs: " + cusNotifsAfterCheckout);
            assertTrue(staffNotifsAfterCheckout > staffNotifsAfterCreate, "Staff notification count should increase after checkout");
            assertTrue(cusNotifsAfterCheckout > cusNotifsAfterCreate, "Customer notification count should increase after checkout");

            // ==========================================
            // TEST FLOW 3: KHỞI TẠO KHUYẾN MÃI MỚI (NEW PROMOTION)
            // ==========================================
            String testPromoCode = "TESTPROMO" + System.currentTimeMillis() % 1000;
            PromotionCreateRequest promoReq = new PromotionCreateRequest();
            promoReq.setCode(testPromoCode);
            promoReq.setName("Khuyến mãi đặc biệt Test");
            promoReq.setDescription("Giảm giá 10% cho tất cả dịch vụ");
            promoReq.setDiscountType(DiscountType.PERCENTAGE);
            promoReq.setValue(BigDecimal.valueOf(10));
            promoReq.setCostPoints(50);
            promoReq.setMinTier("Member");
            promoReq.setStartDate(LocalDateTime.now());
            promoReq.setEndDate(LocalDateTime.now().plusDays(10));
            promoReq.setTotalBudget(100);

            PromotionResponse promoRes = adminPromotionService.createPromotion(promoReq);
            assertNotNull(promoRes);
            createdPromoId = promoRes.getId();

            Thread.sleep(200);

            long staffNotifsAfterPromo = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
            System.out.println("After New Promotion - Staff Notifs: " + staffNotifsAfterPromo);
            assertTrue(staffNotifsAfterPromo > staffNotifsAfterCheckout, "Staff notification count should increase after promotion creation");

            // ==========================================
            // TEST FLOW 4: KHÁCH HÀNG FEEDBACK & ADMIN GIẢI QUYẾT
            // ==========================================
            CustomerFeedbackCreateRequest feedbackReq = new CustomerFeedbackCreateRequest();
            feedbackReq.setBookingCode(checkoutRes.getBookingCode());
            feedbackReq.setRatingStars(5);
            feedbackReq.setComment("Dịch vụ tuyệt vời!");
            
            FeedbackResponse feedbackRes = customerFeedbackService.createFeedback(customer.getCustomerId(), feedbackReq);
            assertNotNull(feedbackRes);
            createdFeedbackId = feedbackRes.getId();

            Thread.sleep(200);

            long staffNotifsAfterFeedback = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
            System.out.println("After Customer Feedback - Staff Notifs: " + staffNotifsAfterFeedback);
            assertTrue(staffNotifsAfterFeedback > staffNotifsAfterPromo, "Staff notification count should increase after feedback creation");

            // Admin Resolve/Reply Feedback
            FeedbackResolveRequest resolveReq = new FeedbackResolveRequest();
            resolveReq.setResolutionNotes("Cảm ơn quý khách, chúng tôi ghi nhận đóng góp!");
            resolveReq.setGrantCompensationVoucher(false);

            FeedbackResponse resolvedRes = adminFeedbackService.resolveFeedback(createdFeedbackId, resolveReq);
            assertNotNull(resolvedRes);

            Thread.sleep(200);

            long cusNotifsAfterResolve = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.CUSTOMER && customer.getCustomerId().equals(n.getRecipientId())).count();
            System.out.println("After Admin Resolve Feedback - Customer Notifs: " + cusNotifsAfterResolve);
            assertTrue(cusNotifsAfterResolve > cusNotifsAfterCheckout, "Customer notification count should increase after feedback resolution");

            // ==========================================
            // TEST FLOW 5: KHÁCH HÀNG CLAIM/ĐỔI VOUCHER
            // ==========================================
            customer.setLoyaltyPoints(1000);
            customerRepository.save(customer);

            Promotion promotion = promotionRepository.findById(createdPromoId).orElseThrow();
            promotion.setStatus(PromotionStatus.ACTIVE);
            promotionRepository.save(promotion);

            assertNotNull(customerRewardService.exchangePoints(customer.getCustomerId(), createdPromoId));

            Thread.sleep(200);

            long staffNotifsAfterExchange = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.ALL_STAFF).count();
            long cusNotifsAfterExchange = notificationRepository.findAll().stream()
                    .filter(n -> n.getRecipientType() == NotificationRecipientType.CUSTOMER && customer.getCustomerId().equals(n.getRecipientId())).count();

            System.out.println("After Voucher Exchange - Staff Notifs: " + staffNotifsAfterExchange + ", Customer Notifs: " + cusNotifsAfterExchange);
            assertTrue(staffNotifsAfterExchange > staffNotifsAfterFeedback, "Staff notification count should increase after voucher exchange");
            assertTrue(cusNotifsAfterExchange > cusNotifsAfterResolve, "Customer notification count should increase after voucher exchange");

            System.out.println("====== INTEGRATION TESTING FOR BI-DIRECTIONAL NOTIFICATIONS COMPLETED SUCCESSFULLY ======");

        } catch (Exception e) {
            fail("Test threw an exception: " + e.getMessage(), e);
        } finally {
            // Dọn dẹp dữ liệu để không ảnh hưởng database thực tế
            System.out.println("Cleaning up test data...");
            try {
                if (createdFeedbackId != null) {
                    customerFeedbackRepository.deleteById(createdFeedbackId);
                }
                if (createdPromoId != null) {
                    customerPromotionRepository.deleteAll(customerPromotionRepository.findByCustomerCustomerId(customer.getCustomerId()));
                    promotionRepository.deleteById(createdPromoId);
                }
                if (createdBookingId != null) {
                    bookingRepository.deleteById(createdBookingId);
                }
                customer.setLoyaltyPoints(originalPoints);
                customerRepository.save(customer);

                // Xóa tất cả các thông báo phát sinh trong bài test để giữ DB sạch
                List<Notification> allNotifs = notificationRepository.findAll();
                for (Notification n : allNotifs) {
                    if (n.getNotificationId() > 7) { // Các thông báo seeded có ID <= 7
                        notificationRepository.delete(n);
                    }
                }
                System.out.println("Cleanup completed.");
            } catch (Exception ex) {
                System.err.println("Error cleaning up test data: " + ex.getMessage());
            }
        }
    }
}
