package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRewardShopResponse {
    private Long id;
    private String code;
    private String title;
    private String description;
    private DiscountType discountType;
    private BigDecimal value;
    private Integer pointsCost;
    private String minTier;
    private LocalDateTime endDate;
    private boolean isUnlocked;
    private boolean isGrayscale;
    private String unlockTooltip;
    private String applicableServiceCode;
    private String applicableDays;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
}
