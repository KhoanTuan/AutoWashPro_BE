package com.autowashpro.autowashpro_be;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.CustomerFeedbackCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.FeedbackResolveRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.PromotionCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionSource;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
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
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private com.autowashpro.autowashpro_be.modules.booking.scheduler.OverdueBookingScheduler overdueBookingScheduler;

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
            BookingResponse checkoutRes = bookingService.completeCheckout(String.valueOf(createdBookingId), "cash");
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
            promoReq.setMaxDiscountAmount(BigDecimal.valueOf(20000));
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

    @Test
    void testNoShowViolationPenalties() {
        System.out.println("====== START INTEGRATION TESTING FOR NO-SHOW PENALTIES ======");
        
        Customer customer = customerRepository.findByPhoneNumber("0902000001")
                .orElseThrow(() -> new AssertionError("Seeded customer not found"));
        TimeSlot slot = timeSlotRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("No time slots available"));

        int originalPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        // Đặt điểm ban đầu của khách hàng về 100 để test trừ điểm dễ dàng
        customer.setLoyaltyPoints(100);
        customerRepository.save(customer);

        // Đảm bảo không có đơn No-Show nào trong 30 ngày qua trước khi test
        List<com.autowashpro.autowashpro_be.modules.booking.entity.Booking> initialNoShows = bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer().getCustomerId().equals(customer.getCustomerId()) && b.getStatus() == BookingStatus.CANCELLED_NO_SHOW)
                .toList();
        bookingRepository.deleteAll(initialNoShows);

        Long bookingId1 = null;
        Long bookingId2 = null;
        Long bookingId3 = null;

        try {
            // ========================================================
            // VI PHẠM LẦN 1: Tạo Booking 1, chuyển về hôm qua để quá hạn
            // ========================================================
            CreateBookingRequest bookingReq1 = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(5)) // Tương lai gần
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L)
                    .notes("Test No-Show 1")
                    .build();

            BookingResponse bookingRes1 = bookingService.createBooking(bookingReq1, customer);
            assertNotNull(bookingRes1);
            bookingId1 = bookingRes1.getBookingId();

            // Set ngày đặt lịch về hôm qua để scheduler đánh dấu quá hạn
            com.autowashpro.autowashpro_be.modules.booking.entity.Booking booking1 = bookingRepository.findById(bookingId1).orElseThrow();
            booking1.setBookingDate(LocalDate.now().minusDays(1));
            bookingRepository.saveAndFlush(booking1);

            // Chạy scheduler để hủy No-Show
            overdueBookingScheduler.cancelOverdueBookings();

            // Verify đơn 1 chuyển sang CANCELLED_NO_SHOW
            com.autowashpro.autowashpro_be.modules.booking.entity.Booking bookingAfter1 = bookingRepository.findById(bookingId1).orElseThrow();
            assertEquals(BookingStatus.CANCELLED_NO_SHOW, bookingAfter1.getStatus());

            // Verify Lần 1: Không trừ điểm Loyalty
            Customer customerAfter1 = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(100, customerAfter1.getLoyaltyPoints(), "First No-Show should not deduct points");

            // ========================================================
            // VI PHẠM LẦN 2: Tạo Booking 2, chuyển về hôm qua để quá hạn
            // ========================================================
            CreateBookingRequest bookingReq2 = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(5))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L)
                    .notes("Test No-Show 2")
                    .build();

            BookingResponse bookingRes2 = bookingService.createBooking(bookingReq2, customer);
            assertNotNull(bookingRes2);
            bookingId2 = bookingRes2.getBookingId();

            com.autowashpro.autowashpro_be.modules.booking.entity.Booking booking2 = bookingRepository.findById(bookingId2).orElseThrow();
            booking2.setBookingDate(LocalDate.now().minusDays(1));
            bookingRepository.saveAndFlush(booking2);

            overdueBookingScheduler.cancelOverdueBookings();

            // Verify Lần 2: Trừ 10 điểm Loyalty (100 - 10 = 90)
            Customer customerAfter2 = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(90, customerAfter2.getLoyaltyPoints(), "Second No-Show should deduct 10 points");

            // ========================================================
            // VI PHẠM LẦN 3: Tạo Booking 3, chuyển về hôm qua để quá hạn
            // ========================================================
            CreateBookingRequest bookingReq3 = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(5))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L)
                    .notes("Test No-Show 3")
                    .build();

            BookingResponse bookingRes3 = bookingService.createBooking(bookingReq3, customer);
            assertNotNull(bookingRes3);
            bookingId3 = bookingRes3.getBookingId();

            com.autowashpro.autowashpro_be.modules.booking.entity.Booking booking3 = bookingRepository.findById(bookingId3).orElseThrow();
            booking3.setBookingDate(LocalDate.now().minusDays(1));
            bookingRepository.saveAndFlush(booking3);

            overdueBookingScheduler.cancelOverdueBookings();

            // Verify Lần 3: Trừ thêm 10 điểm Loyalty (90 - 10 = 80)
            Customer customerAfter3 = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(80, customerAfter3.getLoyaltyPoints(), "Third No-Show should deduct 10 points");

            // ========================================================
            // KIỂM TRA KHÓA ĐẶT LỊCH: Tạo Booking 4 phải báo lỗi bị chặn
            // ========================================================
            CreateBookingRequest bookingReq4 = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(5))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L)
                    .notes("Test No-Show 4 - should fail")
                    .build();

            Exception exception = assertThrows(Exception.class, () -> {
                bookingService.createBooking(bookingReq4, customerAfter3);
            });

            System.out.println("Booking 4 block exception message: " + exception.getMessage());
            assertTrue(exception.getMessage().contains("tạm khóa tính năng đặt lịch online"), 
                    "Should contain ban message");

        } finally {
            // Dọn dẹp dữ liệu test
            System.out.println("Cleaning up No-Show test data...");
            try {
                if (bookingId1 != null) bookingRepository.deleteById(bookingId1);
                if (bookingId2 != null) bookingRepository.deleteById(bookingId2);
                if (bookingId3 != null) bookingRepository.deleteById(bookingId3);
                customer.setLoyaltyPoints(originalPoints);
                customerRepository.save(customer);

                List<Notification> allNotifs = notificationRepository.findAll();
                for (Notification n : allNotifs) {
                    if (n.getNotificationId() > 7) {
                        notificationRepository.delete(n);
                    }
                }
                System.out.println("Cleanup No-Show completed.");
            } catch (Exception ex) {
                System.err.println("Error cleaning up No-Show test data: " + ex.getMessage());
            }
        }
    }

    @Test
    void testMinOrderValueConstraints() {
        System.out.println("====== START INTEGRATION TESTING FOR MIN_ORDER_VALUE CONSTRAINTS ======");
        
        Customer customer = customerRepository.findByPhoneNumber("0902000001")
                .orElseThrow(() -> new AssertionError("Seeded customer not found"));
        TimeSlot slot = timeSlotRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("No time slots available"));

        // Save original status
        CustomerStatus originalStatus = customer.getStatus();
        customer.setStatus(CustomerStatus.ACTIVE); // Ensure active to avoid lockout blocks
        customerRepository.save(customer);

        // 1. Create a promotion with minOrderValue = 50000
        Promotion promo = Promotion.builder()
                .code("MIN_ORDER_TEST")
                .name("Voucher giảm 10k cho đơn từ 50k")
                .discountType(DiscountType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(10000))
                .minOrderValue(BigDecimal.valueOf(50000))
                .costPoints(0)
                .status(PromotionStatus.ACTIVE)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
        Promotion savedPromo = promotionRepository.save(promo);

        // 2. Issue this voucher to the customer (CLAIM source)
        CustomerPromotion claimCp = CustomerPromotion.builder()
                .customer(customer)
                .promotion(savedPromo)
                .voucherCode("VOU-MIN-ORDER-CLAIM")
                .source(CustomerPromotionSource.CLAIM)
                .status(CustomerPromotionStatus.ISSUED)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();
        customerPromotionRepository.save(claimCp);

        // 3. Issue a POINTS_EXCHANGE source voucher to the customer for the same promotion
        CustomerPromotion exchangeCp = CustomerPromotion.builder()
                .customer(customer)
                .promotion(savedPromo)
                .voucherCode("VOU-MIN-ORDER-EXCHANGE")
                .source(CustomerPromotionSource.EXCHANGE)
                .status(CustomerPromotionStatus.ISSUED)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();
        customerPromotionRepository.save(exchangeCp);

        Long bookingId = null;
        try {
            // Test 3.1: Use CLAIM voucher on a standard package (30k) -> Should fail because 30k < 50k
            CreateBookingRequest failReq = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(6))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L) // Standard: 30k
                    .voucherCode("VOU-MIN-ORDER-CLAIM")
                    .build();

            Exception ex = assertThrows(Exception.class, () -> {
                bookingService.createBooking(failReq, customer);
            });
            System.out.println("Exception message for sub-min order: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("chỉ áp dụng cho đơn hàng từ 50000"), "Should contain min order value message");

            // Test 3.2: Use CLAIM voucher on a deluxe package (50k) -> Should succeed
            CreateBookingRequest successReq = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(6))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(2L) // Deluxe: 50k
                    .voucherCode("VOU-MIN-ORDER-CLAIM")
                    .build();

            BookingResponse successRes = bookingService.createBooking(successReq, customer);
            assertNotNull(successRes);
            assertEquals(BigDecimal.valueOf(10000).doubleValue(), successRes.getDiscountAmount().doubleValue(), 0.01);
            assertEquals(BigDecimal.valueOf(40000).doubleValue(), successRes.getFinalAmount().doubleValue(), 0.01);
            bookingId = successRes.getBookingId();

            // Test 3.3: Use EXCHANGE voucher on a standard package (30k) -> Should succeed (bypassed)
            CreateBookingRequest bypassReq = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(7))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda SH 150i")
                    .packageId(1L) // Standard: 30k
                    .voucherCode("VOU-MIN-ORDER-EXCHANGE")
                    .build();

            BookingResponse bypassRes = bookingService.createBooking(bypassReq, customer);
            assertNotNull(bypassRes);
            assertEquals(BigDecimal.valueOf(10000).doubleValue(), bypassRes.getDiscountAmount().doubleValue(), 0.01);
            assertEquals(BigDecimal.valueOf(20000).doubleValue(), bypassRes.getFinalAmount().doubleValue(), 0.01);
            bookingRepository.deleteById(bypassRes.getBookingId());

        } finally {
            // Clean up
            System.out.println("Cleaning up min order test data...");
            if (bookingId != null) {
                try {
                    bookingRepository.deleteById(bookingId);
                } catch (Exception ignored) {}
            }
            try {
                customerPromotionRepository.delete(claimCp);
                customerPromotionRepository.delete(exchangeCp);
                promotionRepository.delete(savedPromo);
                customer.setStatus(originalStatus);
                customerRepository.save(customer);
            } catch (Exception ignored) {}
            System.out.println("Cleanup min order completed.");
        }
    }
}
