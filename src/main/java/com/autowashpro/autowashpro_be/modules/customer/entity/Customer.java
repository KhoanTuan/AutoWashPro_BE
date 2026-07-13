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

    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private CustomerAuthProvider authProvider = CustomerAuthProvider.PHONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Integer visitCount = 0;

    @Column(name = "total_spending", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalSpending = BigDecimal.ZERO;

    @Column(name = "tier_spending", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal tierSpending = BigDecimal.ZERO;

    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private LoyaltyTier tier;

    @Column(name = "last_completed_booking_at")
    private LocalDateTime lastCompletedBookingAt;
}
