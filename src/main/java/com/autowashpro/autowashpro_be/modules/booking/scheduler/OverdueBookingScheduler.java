package com.autowashpro.autowashpro_be.modules.booking.scheduler;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBookingScheduler {

    private final BookingRepository bookingRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final CustomerRepository customerRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Scan every 1 minute for overdue bookings that have passed their slotEndTime.
     * Transitions status to CANCELLED_NO_SHOW, deducts points, and pushes notifications.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelOverdueBookings() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Booking> overdueBookings = bookingRepository.findOverdueBookings(today, now);

        if (!overdueBookings.isEmpty()) {
            log.info("Found {} overdue bookings to cancel as No-Show", overdueBookings.size());

            for (Booking booking : overdueBookings) {
                try {
                    // 1. Chuyển trạng thái đơn và commit đồng bộ xuống DB
                    booking.setStatus(BookingStatus.CANCELLED_NO_SHOW);
                    bookingRepository.saveAndFlush(booking);

                    log.info("Booking {} marked as CANCELLED_NO_SHOW due to slot end time elapsed.", booking.getBookingCode());

                    // 2. Xử lý Voucher: Tịch thu voucher (Không hoàn lại cho khách, chuyển trạng thái sang EXPIRED)
                    if (booking.getVoucherCode() != null && !booking.getVoucherCode().trim().isEmpty()) {
                        customerPromotionRepository.findByCustomerCustomerIdAndVoucherCode(
                                        booking.getCustomer().getCustomerId(), 
                                        booking.getVoucherCode()
                                )
                                .ifPresent(cp -> {
                                    cp.setStatus(CustomerPromotionStatus.EXPIRED);
                                    customerPromotionRepository.save(cp);
                                    log.info("Confiscated voucher {} (status: EXPIRED) for No-Show booking {} of customer {}", 
                                            booking.getVoucherCode(), booking.getBookingCode(), booking.getCustomer().getCustomerId());
                                });
                    }

                    // 3. Tính toán số lần vi phạm No-Show trong 30 ngày qua để áp dụng chế tài phạt
                    LocalDateTime startOf30DaysAgo = LocalDateTime.now().minusDays(30);
                    long noShowCount = bookingRepository.countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(
                            booking.getCustomer().getCustomerId(),
                            Arrays.asList(BookingStatus.CANCELLED_NO_SHOW),
                            startOf30DaysAgo
                    );

                    int pointsToDeduct = 0;
                    String title = "Hủy đơn (No-Show)";
                    String content = "";

                    if (noShowCount == 1) {
                        content = "Lịch hẹn " + booking.getBookingCode() + " bị hủy do trễ hẹn. Lần sau trễ sẽ bị trừ 10 điểm Loyalty.";
                    } else if (noShowCount == 2) {
                        pointsToDeduct = 10;
                        content = "Bị trừ 10 điểm Loyalty do trễ hẹn lần 2. Lần sau trễ sẽ bị khóa đặt lịch 7 ngày.";
                    } else {
                        pointsToDeduct = 10;
                        LocalDateTime banUntil = LocalDateTime.now().plusDays(7);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        content = "Bị trừ 10 điểm và khóa đặt lịch đến " + banUntil.format(formatter) + " do trễ hẹn lần " + noShowCount + ".";
                    }

                    // Thực hiện trừ điểm
                    if (pointsToDeduct > 0) {
                        Customer customer = booking.getCustomer();
                        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
                        customer.setLoyaltyPoints(Math.max(currentPoints - pointsToDeduct, -9999)); // Cho phép âm
                        customerRepository.save(customer);
                        log.info("Deducted {} loyalty points from customer {}. New points: {}", pointsToDeduct, customer.getCustomerId(), customer.getLoyaltyPoints());

                        // Ghi nhận nhật ký điểm phạt No-Show
                        PointTransaction pt = PointTransaction.builder()
                                .customer(customer)
                                .points(-pointsToDeduct)
                                .activityType(PointActivityType.PENALTY)
                                .bookingCode(booking.getBookingCode())
                                .build();
                        pointTransactionRepository.save(pt);
                    }

                    // 4. Phát sự kiện thông báo tương ứng cho khách hàng giải thích chế tài phạt
                    eventPublisher.publishEvent(new BookingEvent(this, booking, BookingEventAction.CANCELLED, title, content));

                } catch (Exception e) {
                    log.error("Error cancelling overdue booking id: " + booking.getBookingId(), e);
                }
            }
        }
    }
}
