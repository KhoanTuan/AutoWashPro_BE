package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLoyaltyProfileResponse {
    private Long customerId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Integer loyaltyPoints;
    private BigDecimal totalSpending;
    private BigDecimal tierSpending;
    private String tierName;
    private String nextTierName;
    private BigDecimal nextTierMinSpend;
    private BigDecimal spendNeededForNextTier;
    private Double progressPercentage;
    private Integer bookingWindowDays;
}
