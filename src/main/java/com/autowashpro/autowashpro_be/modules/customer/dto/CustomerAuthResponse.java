package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerAuthResponse {
    private String accessToken;
    private String tokenType;
    private Long customerId;
    private String phoneNumber;
    private String fullName;
    private String tierName;
    private Integer loyaltyPoints;
}
