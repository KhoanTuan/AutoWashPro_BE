package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal value;
    private Integer costPoints;
    private String minTier;
    private Integer minRecencyDays;
    private Integer maxClaimPerUser;
    private Integer totalBudget;
    private Integer issuedCount;
    private Integer redeemedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PromotionStatus status;
    private String budgetStatus;
    private Double redemptionRate;
    private String applicableServiceCode;
    private String applicableDays;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
}
