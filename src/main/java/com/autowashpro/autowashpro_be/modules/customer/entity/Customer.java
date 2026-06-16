package com.autowashpro.autowashpro_be.modules.customer.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "phone_number", nullable = false, unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Integer visitCount = 0;

    @Column(name = "total_spending", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalSpending = BigDecimal.ZERO;

    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private LoyaltyTier tier;

    @Column(name = "last_completed_booking_at")
    private LocalDateTime lastCompletedBookingAt;
}
