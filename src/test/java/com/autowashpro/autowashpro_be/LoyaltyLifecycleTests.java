package com.autowashpro.autowashpro_be;

import com.autowashpro.autowashpro_be.modules.booking.dto.BookingResponse;
import com.autowashpro.autowashpro_be.modules.booking.dto.CreateBookingRequest;
import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.booking.repository.TimeSlotRepository;
import com.autowashpro.autowashpro_be.modules.booking.service.BookingService;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyConfigRequest;
import com.autowashpro.autowashpro_be.modules.customer.entity.*;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyConfigRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository;
import com.autowashpro.autowashpro_be.modules.customer.scheduler.LoyaltyScheduler;
import com.autowashpro.autowashpro_be.modules.customer.service.LoyaltyService;
import com.autowashpro.autowashpro_be.modules.notification.entity.Notification;
import com.autowashpro.autowashpro_be.modules.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoyaltyLifecycleTests {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyScheduler loyaltyScheduler;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyTierRepository loyaltyTierRepository;

    @Autowired
    private LoyaltyConfigRepository loyaltyConfigRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testLoyaltyLifecycleAndSimulation() {
        System.out.println("====== START INTEGRATION TESTING FOR E2E-4 LOYALTY LIFECYCLE ======");

        Customer customer = customerRepository.findByPhoneNumber("0902000001")
                .orElseThrow(() -> new AssertionError("Seeded customer not found"));
        TimeSlot slot = timeSlotRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("No time slots available"));
        List<LoyaltyTier> tiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
        assertTrue(tiers.size() >= 4, "Should have at least 4 tiers");
        LoyaltyTier regularTier = tiers.get(0);
        LoyaltyTier silverTier = tiers.get(1);
        LoyaltyTier goldTier = tiers.get(2);
        LoyaltyTier platinumTier = tiers.get(3);

        // Đảm bảo không có point transactions cũ của customer này trong DB trước khi test
        pointTransactionRepository.deleteAll(pointTransactionRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId()));

        // Lưu thông số cũ của customer và config để phục hồi sau test
        int originalPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        BigDecimal originalTierSpend = customer.getTierSpending() != null ? customer.getTierSpending() : BigDecimal.ZERO;
        BigDecimal originalTotalSpend = customer.getTotalSpending() != null ? customer.getTotalSpending() : BigDecimal.ZERO;
        LoyaltyTier originalTier = customer.getTier();
        CustomerStatus originalStatus = customer.getStatus();
        
        LoyaltyConfig originalConfig = loyaltyConfigRepository.getGlobalConfig();
        LoyaltyConfigRequest backupConfig = LoyaltyConfigRequest.builder()
                .basePointRate(originalConfig.getBasePointRate())
                .basePoints(originalConfig.getBasePoints())
                .roundDown(originalConfig.getRoundDown())
                .pointValidityMonths(originalConfig.getPointValidityMonths())
                .inactivityDowngradeMonths(originalConfig.getInactivityDowngradeMonths())
                .inactivityLockoutMonths(originalConfig.getInactivityLockoutMonths())
                .build();

        Long createdBookingId = null;

        try {
            // ==========================================
            // TEST 1: CẤU HÌNH TÍCH ĐIỂM ĐỘNG & CHECKOUT
            // ==========================================
            // Đổi tỷ lệ tích lũy thành: 1 điểm cho mỗi 20,000 VNĐ, có làm tròn xuống
            LoyaltyConfigRequest newConfig = LoyaltyConfigRequest.builder()
                    .basePointRate(BigDecimal.valueOf(20000))
                    .basePoints(1)
                    .roundDown(true)
                    .pointValidityMonths(12)
                    .inactivityDowngradeMonths(6)
                    .inactivityLockoutMonths(12)
                    .build();
            loyaltyService.updateLoyaltyConfig(newConfig);

            // Reset customer points và tier spending để test chính xác
            customer.setLoyaltyPoints(0);
            customer.setTierSpending(BigDecimal.ZERO);
            customer.setTier(regularTier);
            customerRepository.save(customer);

            // Tạo booking trị giá 50,000 VNĐ
            CreateBookingRequest bookingReq = CreateBookingRequest.builder()
                    .bookingDate(LocalDate.now().plusDays(1))
                    .timeSlotId(slot.getSlotId())
                    .licensePlate("51A-12345")
                    .model("Honda Air Blade")
                    .packageId(1L) // Gói 1
                    .notes("Test E2E-4 Point Accumulation")
                    .build();

            BookingResponse bookingRes = bookingService.createBooking(bookingReq, customer);
            createdBookingId = bookingRes.getBookingId();

            // Tiến hành thanh toán hoàn thành booking
            bookingService.completeCheckout(createdBookingId, "cash");

            // Verify số điểm tích lũy: 30,000 / 20,000 = 1.5. Làm tròn xuống là 1 điểm.
            Customer customerAfterCheckout = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(1, customerAfterCheckout.getLoyaltyPoints(), "Should earn exactly 1 point under 20k/point round-down config");

            // Verify PointTransaction được ghi nhận
            List<PointTransaction> txList = pointTransactionRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId());
            assertFalse(txList.isEmpty());
            PointTransaction latestTx = txList.get(0);
            assertEquals(1, latestTx.getPoints());
            assertEquals(PointActivityType.EARNED, latestTx.getActivityType());

            // ==========================================
            // TEST 2: GIẢ LẬP HẠ HẠNG VIP DO VẮNG MẶT
            // ==========================================
            // Nâng hạng khách hàng lên Platinum
            customerAfterCheckout.setTier(platinumTier);
            customerAfterCheckout.setTierSpending(BigDecimal.valueOf(10000000));
            customerRepository.save(customerAfterCheckout);

            // Giả lập vắng mặt 7 tháng (> 6 tháng)
            loyaltyService.simulateSetInactivity(customer.getCustomerId(), 7);

            // Chạy job hạ hạng
            loyaltyScheduler.downgradeInactiveTiers();

            // Xác nhận khách hàng bị hạ xuống Gold (hạng kế dưới Platinum)
            Customer customerAfterDowngrade = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(goldTier.getTierId(), customerAfterDowngrade.getTier().getTierId(), "Should be downgraded to Gold");
            assertEquals(goldTier.getMinSpend(), customerAfterDowngrade.getTierSpending(), "Tier spending should reset to Gold floor");

            // ==========================================
            // TEST 3: GIẢ LẬP THU HỒI ĐIỂM QUÁ HẠN
            // ==========================================
            // Reset điểm khách hàng về 0 trước khi nạp điểm ảo
            customerAfterDowngrade.setLoyaltyPoints(0);
            customerRepository.save(customerAfterDowngrade);

            // Tạo giao dịch tích lũy điểm cách đây 13 tháng (> 12 tháng)
            loyaltyService.simulateSetPointsExpired(customer.getCustomerId(), 13);

            // Chạy job quét thu hồi điểm hết hạn
            loyaltyScheduler.expireExpiredPoints();

            // Xác nhận điểm bị trừ thu hồi về 0
            Customer customerAfterExpiry = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(0, customerAfterExpiry.getLoyaltyPoints(), "Points should be expired and reduced to 0");

            // Verify PointTransaction loại EXPIRY được ghi nhận
            List<PointTransaction> txList2 = pointTransactionRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId());
            PointTransaction latestExpiryTx = txList2.stream()
                    .filter(t -> t.getActivityType() == PointActivityType.EXPIRY)
                    .findFirst()
                    .orElse(null);
            assertNotNull(latestExpiryTx);
            assertEquals(-100, latestExpiryTx.getPoints());

            // ==========================================
            // TEST 4: GIẢ LẬP KHÓA TÀI KHOẢN KHÁCH CŨ
            // ==========================================
            // Giả lập vắng mặt 13 tháng (> 12 tháng)
            loyaltyService.simulateSetInactivity(customer.getCustomerId(), 13);

            // Chạy job khóa tài khoản
            loyaltyScheduler.lockInactiveAccounts();

            // Xác nhận trạng thái tài khoản chuyển sang INACTIVE
            Customer customerAfterLock = customerRepository.findById(customer.getCustomerId()).orElseThrow();
            assertEquals(CustomerStatus.INACTIVE, customerAfterLock.getStatus(), "Customer status should be INACTIVE");

        } finally {
            // Phục hồi nguyên trạng dữ liệu hệ thống
            System.out.println("Cleaning up E2E-4 test data...");
            try {
                if (createdBookingId != null) {
                    bookingRepository.deleteById(createdBookingId);
                }
                
                // Khôi phục cấu hình Loyalty ban đầu
                loyaltyService.updateLoyaltyConfig(backupConfig);

                // Khôi phục trạng thái khách hàng ban đầu
                Customer finalRestore = customerRepository.findById(customer.getCustomerId()).orElseThrow();
                finalRestore.setLoyaltyPoints(originalPoints);
                finalRestore.setTierSpending(originalTierSpend);
                finalRestore.setTotalSpending(originalTotalSpend);
                finalRestore.setTier(originalTier);
                finalRestore.setStatus(originalStatus);
                finalRestore.setLastCompletedBookingAt(null);
                customerRepository.save(finalRestore);

                // Xóa các PointTransaction phát sinh trong bài test
                pointTransactionRepository.deleteAll(pointTransactionRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId()));

                // Xóa các thông báo phát sinh
                List<Notification> allNotifs = notificationRepository.findAll();
                for (Notification n : allNotifs) {
                    if (n.getNotificationId() > 7) {
                        notificationRepository.delete(n);
                    }
                }
                System.out.println("Cleanup E2E-4 completed.");
            } catch (Exception ex) {
                System.err.println("Error cleaning up E2E-4 test data: " + ex.getMessage());
            }
        }
    }
}
