package com.autowashpro.autowashpro_be.modules.financial.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uk_financial_ledger_date", columnNames = "ledger_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialLedger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "ledger_date", nullable = false)
    private LocalDate ledgerDate;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_cash", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "total_momo", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalMomo = BigDecimal.ZERO;

    @Column(name = "total_expenses", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    @Column(name = "closing_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LedgerStatus status = LedgerStatus.OPEN;

    @Column(name = "sealed_at")
    private LocalDateTime sealedAt;

    @Column(name = "summary_notes", length = 1000)
    private String summaryNotes;
}
