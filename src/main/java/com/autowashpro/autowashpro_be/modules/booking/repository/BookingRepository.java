package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    int countByBookingDateAndTimeSlotSlotIdAndStatusIn(LocalDate bookingDate, Long slotId, Collection<BookingStatus> statuses);
    int countByBookingDateAndStatusIn(LocalDate bookingDate, Collection<BookingStatus> statuses);
    List<Booking> findAllByBookingDateAndStatusIn(LocalDate bookingDate, Collection<BookingStatus> statuses);
    boolean existsByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(LocalDate bookingDate, String licensePlate, Collection<BookingStatus> statuses);
    List<Booking> findAllByBookingDateAndLicensePlateIgnoreCaseAndStatusIn(LocalDate bookingDate, String licensePlate, Collection<BookingStatus> statuses);
    int countByCustomerCustomerIdAndBookingDateAndStatusIn(Long customerId, LocalDate bookingDate, Collection<BookingStatus> statuses);
    boolean existsByCustomerCustomerIdAndBookingDateAndTimeSlotSlotIdAndStatusIn(Long customerId, LocalDate bookingDate, Long slotId, Collection<BookingStatus> statuses);
    List<Booking> findAllByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Booking> findByBookingCode(String bookingCode);
    boolean existsByBookingCode(String bookingCode);
    List<Booking> findAllByBookingDate(LocalDate bookingDate);
    long countByCustomerCustomerIdAndStatusInAndUpdatedAtAfter(Long customerId, Collection<BookingStatus> statuses, java.time.LocalDateTime since);
    Optional<Booking> findFirstByCustomerCustomerIdAndStatusOrderByUpdatedAtDesc(Long customerId, BookingStatus status);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByLicensePlateIgnoreCase(String licensePlate);

    @Query("SELECT b FROM Booking b WHERE " +
           "LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.licensePlate) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Booking> searchBookings(String query);

    List<Booking> findAllByTimeSlotSlotIdAndBookingDateGreaterThanEqualAndStatusIn(Long slotId, LocalDate fromDate, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.status = 'COMPLETED' AND b.voucherCode IS NOT NULL")
    List<Booking> findCompletedBookingsWithVoucher();

    boolean existsByTimeSlotSlotId(Long slotId);

    @Query("SELECT b FROM Booking b WHERE (b.status = com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus.PENDING OR b.status = com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus.CONFIRMED) AND " +
           "(b.bookingDate < :today OR (b.bookingDate = :today AND b.timeSlot.startTime < :cutoffTime))")
    List<Booking> findOverdueBookings(LocalDate today, LocalTime cutoffTime);
}
