package com.autowashpro.autowashpro_be.modules.financial.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 50)
    private String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private Staff cashier;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vehicle_surcharge", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal vehicleSurcharge = BigDecimal.ZERO;

    @Column(name = "promotion_discount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal promotionDiscount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid_cash", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal amountPaidCash = BigDecimal.ZERO;

    @Column(name = "amount_paid_momo", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal amountPaidMomo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private InvoicePaymentStatus paymentStatus = InvoicePaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_payment_status", nullable = false, length = 20)
    @Builder.Default
    private SplitPaymentStatus splitPaymentStatus = SplitPaymentStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.DRAFT;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    public BigDecimal getAmountPaidTotal() {
        return amountPaidCash.add(amountPaidMomo);
    }

    public BigDecimal getRemainingBalance() {
        return totalAmount.subtract(getAmountPaidTotal());
    }

    public boolean isFullyPaid() {
        return getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0;
    }
}
