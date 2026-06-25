package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.modules.financial.entity.FinancialLedger;
import com.autowashpro.autowashpro_be.modules.financial.entity.InvoicePaymentStatus;
import com.autowashpro.autowashpro_be.modules.financial.entity.LedgerStatus;
import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentMethod;
import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentTransaction;
import com.autowashpro.autowashpro_be.modules.financial.entity.PaymentTxStatus;
import com.autowashpro.autowashpro_be.modules.financial.repository.FinancialLedgerRepository;
import com.autowashpro.autowashpro_be.modules.financial.repository.InvoiceRepository;
import com.autowashpro.autowashpro_be.modules.financial.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialLedgerService {

    private final FinancialLedgerRepository financialLedgerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AiAnalyticsService aiAnalyticsService;

    @Scheduled(cron = "0 59 23 * * *")
    @Transactional
    public void sealDailyLedgerAtMidnight() {
        LocalDate today = LocalDate.now();
        log.info("Sealing financial ledger for {}", today);
        sealLedgerForDate(today);
    }

    @Transactional
    public FinancialLedger sealLedgerForDate(LocalDate ledgerDate) {
        FinancialLedger ledger = financialLedgerRepository.findByLedgerDate(ledgerDate)
                .orElseGet(() -> FinancialLedger.builder()
                        .ledgerDate(ledgerDate)
                        .status(LedgerStatus.OPEN)
                        .build());

        if (ledger.getStatus() == LedgerStatus.SEALED) {
            return ledger;
        }

        LocalDateTime dayStart = ledgerDate.atStartOfDay();
        LocalDateTime dayEnd = ledgerDate.atTime(LocalTime.MAX);

        BigDecimal totalCash = sumSuccessfulPayments(dayStart, dayEnd, PaymentMethod.CASH);
        BigDecimal totalMomo = sumSuccessfulPayments(dayStart, dayEnd, PaymentMethod.MOMO);
        BigDecimal totalRevenue = totalCash.add(totalMomo);

        BigDecimal openingBalance = financialLedgerRepository.findTopByOrderByLedgerDateDesc()
                .filter(prev -> prev.getLedgerDate().isBefore(ledgerDate))
                .map(FinancialLedger::getClosingBalance)
                .orElse(BigDecimal.ZERO);

        ledger.setOpeningBalance(openingBalance);
        ledger.setTotalCash(totalCash);
        ledger.setTotalMomo(totalMomo);
        ledger.setTotalRevenue(totalRevenue);
        ledger.setClosingBalance(openingBalance.add(totalRevenue).subtract(ledger.getTotalExpenses()));
        ledger.setStatus(LedgerStatus.SEALED);
        ledger.setSealedAt(LocalDateTime.now());
        ledger.setSummaryNotes(buildSummary(totalCash, totalMomo, totalRevenue));

        financialLedgerRepository.save(ledger);

        long paidInvoices = invoiceRepository.findPaidInvoicesBetween(
                dayStart, dayEnd, InvoicePaymentStatus.PAID
        ).size();
        log.info("Ledger sealed for {} — revenue={}, paidInvoices={}", ledgerDate, totalRevenue, paidInvoices);

        aiAnalyticsService.generateRecommendationsAsync(ledger);
        return ledger;
    }

    private BigDecimal sumSuccessfulPayments(LocalDateTime from, LocalDateTime to, PaymentMethod method) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll().stream()
                .filter(tx -> tx.getStatus() == PaymentTxStatus.SUCCESS)
                .filter(tx -> tx.getPaymentMethod() == method)
                .filter(tx -> !tx.getCreatedAt().isBefore(from) && !tx.getCreatedAt().isAfter(to))
                .toList();

        return transactions.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildSummary(BigDecimal cash, BigDecimal momo, BigDecimal revenue) {
        return String.format("Daily seal — cash: %s VND, MoMo: %s VND, total revenue: %s VND",
                cash.toPlainString(), momo.toPlainString(), revenue.toPlainString());
    }
}
