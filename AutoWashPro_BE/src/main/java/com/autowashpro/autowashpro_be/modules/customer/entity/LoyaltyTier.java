package com.autowashpro.autowashpro_be.modules.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_tier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Integer tierId;

    @Column(name = "tier_name", nullable = false, unique = true, length = 20)
    private String tierName;

    @Column(name = "min_spend", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minSpend = BigDecimal.ZERO;

    @Column(name = "tier_multiplier", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal tierMultiplier = BigDecimal.ONE;

    @Column(name = "booking_window_days", nullable = false)
    @Builder.Default
    private Integer bookingWindowDays = 7;
}
