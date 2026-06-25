package com.autowashpro.autowashpro_be.modules.financial.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_closure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftClosure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_closure_id")
    private Long shiftClosureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private Staff cashier;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "expected_balance", precision = 14, scale = 2)
    private BigDecimal expectedBalance;

    @Column(name = "actual_balance", precision = 14, scale = 2)
    private BigDecimal actualBalance;

    @Column(name = "variance", precision = 14, scale = 2)
    private BigDecimal variance;

    @Column(name = "total_cash", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "total_momo", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalMomo = BigDecimal.ZERO;

    @Column(name = "total_revenue", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShiftClosureStatus status = ShiftClosureStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
