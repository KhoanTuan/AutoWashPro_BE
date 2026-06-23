package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerOptionResponse {
    private Long customerId;
    private String fullName;
    private String phoneNumber;
    private String licensePlate;
    private String tierName;
}
