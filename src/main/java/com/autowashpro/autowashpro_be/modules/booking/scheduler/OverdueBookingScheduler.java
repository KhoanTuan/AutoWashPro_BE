package com.autowashpro.autowashpro_be.modules.booking.scheduler;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEvent;
import com.autowashpro.autowashpro_be.modules.booking.event.BookingEventAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBookingScheduler {

    private final BookingRepository bookingRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Scan every 1 minute for overdue bookings that have passed their slotEndTime.
     * Transitions status to CANCELLED_NO_SHOW, releases vouchers, and pushes notifications.
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
                    booking.setStatus(BookingStatus.CANCELLED_NO_SHOW);
                    bookingRepository.save(booking);

                    log.info("Booking {} marked as CANCELLED_NO_SHOW due to slot end time elapsed.", booking.getBookingCode());

                    if (booking.getVoucherCode() != null && !booking.getVoucherCode().trim().isEmpty()) {
                        customerPromotionRepository.findByCustomerCustomerIdAndVoucherCode(
                                        booking.getCustomer().getCustomerId(), 
                                        booking.getVoucherCode()
                                )
                                .ifPresent(cp -> {
                                    cp.setStatus(CustomerPromotionStatus.ISSUED);
                                    customerPromotionRepository.save(cp);
                                    log.info("Released voucher {} back to customer {} for No-Show booking {}", 
                                            booking.getVoucherCode(), booking.getCustomer().getCustomerId(), booking.getBookingCode());
                                });
                    }

                    eventPublisher.publishEvent(new BookingEvent(this, booking, BookingEventAction.CANCELLED,
                            "Lịch hẹn đã bị hủy (No-Show)!",
                            "Lịch hẹn " + booking.getBookingCode() + " đã tự động hủy và giải phóng chỗ do quá giờ kết thúc slot."));

                } catch (Exception e) {
                    log.error("Error cancelling overdue booking id: " + booking.getBookingId(), e);
                }
            }
        }
    }
}
