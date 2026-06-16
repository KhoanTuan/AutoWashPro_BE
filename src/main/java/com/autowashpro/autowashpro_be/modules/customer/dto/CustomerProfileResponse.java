package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerProfileResponse {
    private Long customerId;
    private String phoneNumber;
    private String fullName;
    private String tierName;
    private Integer visitCount;
    private BigDecimal totalSpending;
    private Integer loyaltyPoints;
}
