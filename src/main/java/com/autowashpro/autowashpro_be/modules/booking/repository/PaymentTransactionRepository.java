package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByMomoRequestId(String momoRequestId);
    Optional<PaymentTransaction> findByMomoOrderId(String momoOrderId);
    Optional<PaymentTransaction> findByMomoTransId(String momoTransId);
    Optional<PaymentTransaction> findByBookingBookingId(Long bookingId);
}
