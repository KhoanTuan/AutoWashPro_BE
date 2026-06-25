package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.FinancialLedger;
import com.autowashpro.autowashpro_be.modules.financial.entity.LedgerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, Long> {

    Optional<FinancialLedger> findByLedgerDate(LocalDate ledgerDate);

    List<FinancialLedger> findByStatus(LedgerStatus status);

    Optional<FinancialLedger> findTopByOrderByLedgerDateDesc();
}
