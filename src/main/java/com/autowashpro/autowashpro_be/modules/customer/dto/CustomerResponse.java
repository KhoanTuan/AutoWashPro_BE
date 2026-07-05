package com.autowashpro.autowashpro_be.modules.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Thông tin khách hàng cho admin CRM")
public class CustomerResponse {
    private Long customerId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String licensePlate;
    private String model;
    private String status;
    private String statusLabel;
    private String tierName;
    private Integer visitCount;
    private Integer loyaltyPoints;
    private BigDecimal totalSpending;
    private LocalDateTime lastCompletedBookingAt;
    private LocalDateTime createdAt;
}
