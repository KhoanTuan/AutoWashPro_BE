package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionSource;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVoucherResponse {
    private Long id;
    private String voucherCode;
    private String title;
    private String description;
    private DiscountType discountType;
    private BigDecimal value;
    private LocalDateTime issuedAt;
    private LocalDateTime expiryDate;
    private CustomerPromotionStatus status;
    private CustomerPromotionSource source;
    private boolean isExpired;
    private String applicableServiceCode;
    private String applicableDays;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
}
