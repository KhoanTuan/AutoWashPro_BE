package com.autowashpro.autowashpro_be.modules.marketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "cost_points", nullable = false)
    @Builder.Default
    private Integer costPoints = 0;

    @Column(name = "min_tier", length = 30)
    @Builder.Default
    private String minTier = "Member";

    @Column(name = "min_recency_days")
    @Builder.Default
    private Integer minRecencyDays = 0;

    @Column(name = "max_claim_per_user")
    @Builder.Default
    private Integer maxClaimPerUser = 1;

    @Column(name = "total_budget")
    @Builder.Default
    private Integer totalBudget = 1000;

    @Column(name = "issued_count")
    @Builder.Default
    private Integer issuedCount = 0;

    @Column(name = "redeemed_count")
    @Builder.Default
    private Integer redeemedCount = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PromotionStatus.ACTIVE;
        if (costPoints == null) costPoints = 0;
        if (minTier == null) minTier = "Member";
        if (minRecencyDays == null) minRecencyDays = 0;
        if (maxClaimPerUser == null) maxClaimPerUser = 1;
        if (issuedCount == null) issuedCount = 0;
        if (redeemedCount == null) redeemedCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
