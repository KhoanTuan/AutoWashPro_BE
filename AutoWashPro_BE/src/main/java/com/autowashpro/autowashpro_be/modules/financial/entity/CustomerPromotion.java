package com.autowashpro.autowashpro_be.modules.financial.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_promotion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPromotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_promotion_id")
    private Long customerPromotionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerPromotionStatus status = CustomerPromotionStatus.AVAILABLE;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;
}
