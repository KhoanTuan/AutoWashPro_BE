package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByMomoRequestId(String momoRequestId);
    Optional<PaymentTransaction> findByMomoTransId(String momoTransId);
    Optional<PaymentTransaction> findByBookingBookingId(Long bookingId);
}
