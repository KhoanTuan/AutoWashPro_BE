package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    int countByBookingDateAndTimeSlotSlotIdAndStatusIn(LocalDate bookingDate, Long slotId, Collection<BookingStatus> statuses);
    boolean existsByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(LocalDate bookingDate, String licensePlate, Collection<BookingStatus> statuses);
    int countByCustomerCustomerIdAndBookingDateAndStatusIn(Long customerId, LocalDate bookingDate, Collection<BookingStatus> statuses);
    List<Booking> findAllByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Booking> findByBookingCode(String bookingCode);
    boolean existsByBookingCode(String bookingCode);
    List<Booking> findAllByTimeSlotSlotIdAndBookingDateGreaterThanEqualAndStatusIn(Long slotId, LocalDate fromDate, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.status = 'COMPLETED' AND b.voucherCode IS NOT NULL")
    List<Booking> findCompletedBookingsWithVoucher();
}
