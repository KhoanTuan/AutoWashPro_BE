package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.Invoice;
import com.autowashpro.autowashpro_be.modules.financial.entity.InvoicePaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    Optional<Invoice> findByBookingBookingId(Long bookingId);

    List<Invoice> findByPaymentStatus(InvoicePaymentStatus paymentStatus);

    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :from AND i.createdAt < :to AND i.paymentStatus = :status")
    List<Invoice> findPaidInvoicesBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("status") InvoicePaymentStatus status);
}
