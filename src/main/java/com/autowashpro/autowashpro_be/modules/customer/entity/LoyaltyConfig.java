package com.autowashpro.autowashpro_be.modules.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "loyalty_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyConfig {

    @Id
    @Column(name = "loyalty_config_id")
    private Long loyaltyConfigId;

    @Column(name = "base_point_rate", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal basePointRate = BigDecimal.valueOf(10000);

    @Column(name = "base_points", nullable = false)
    @Builder.Default
    private Integer basePoints = 1;

    @Column(name = "round_down", nullable = false)
    @Builder.Default
    private Boolean roundDown = true;

    @Column(name = "point_validity_months", nullable = false)
    @Builder.Default
    private Integer pointValidityMonths = 12;

    @Column(name = "inactivity_downgrade_months", nullable = false)
    @Builder.Default
    private Integer inactivityDowngradeMonths = 6;

    @Column(name = "inactivity_lockout_months", nullable = false)
    @Builder.Default
    private Integer inactivityLockoutMonths = 12;
}
