package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByBookingDate(LocalDate bookingDate);

    long countByBookingDateAndBookingType(LocalDate bookingDate, com.autowashpro.autowashpro_be.modules.booking.entity.BookingType bookingType);

    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.bookingStatus = :status) " +
           "AND (:date IS NULL OR b.bookingDate = :date) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR UPPER(b.vehicle.licensePlate) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Booking> search(@Param("status") BookingStatus status,
                         @Param("date") LocalDate date,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
