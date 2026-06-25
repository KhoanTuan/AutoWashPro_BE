package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import com.autowashpro.autowashpro_be.modules.operations.entity.WaitingQueue;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_code", nullable = false, unique = true, length = 50)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 20)
    private BookingType bookingType;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private Staff cashier;

    @Column(name = "base_subtotal", precision = 12, scale = 2)
    private BigDecimal baseSubtotal;

    @Column(name = "vehicle_surcharge", precision = 12, scale = 2)
    private BigDecimal vehicleSurcharge;

    @Column(name = "finalized_total_price", precision = 12, scale = 2)
    private BigDecimal finalizedTotalPrice;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> bookingItems = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskChecklist> taskChecklists = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WaitingQueue waitingQueue;

    /**
     * Returns the collected revenue amount, preferring the finalized total after dynamic surcharges.
     */
    public BigDecimal getCollectedRevenue() {
        if (finalizedTotalPrice != null) {
            return finalizedTotalPrice;
        }
        return bookingItems.stream()
                .map(BookingItem::getActualPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void finalizeRevenue(BigDecimal baseSubtotal, BigDecimal vehicleSurcharge, BigDecimal finalizedTotalPrice) {
        this.baseSubtotal = baseSubtotal;
        this.vehicleSurcharge = vehicleSurcharge;
        this.finalizedTotalPrice = finalizedTotalPrice;
    }
}
