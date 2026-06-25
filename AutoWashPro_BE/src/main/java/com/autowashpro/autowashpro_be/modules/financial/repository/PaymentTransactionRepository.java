package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentMethod;
import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentTransaction;
import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentTxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    List<PaymentTransaction> findByInvoiceInvoiceId(Long invoiceId);

    List<PaymentTransaction> findByPaymentMethodAndStatus(PaymentMethod paymentMethod, PaymentTxStatus status);
}
