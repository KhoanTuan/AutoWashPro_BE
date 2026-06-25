package com.autowashpro.autowashpro_be.modules.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutRequest {

    @NotNull
    private Long bookingId;

    private Long customerPromotionId;

    @DecimalMin("0.00")
    private BigDecimal cashAmount;

    @DecimalMin("0.00")
    private BigDecimal momoAmount;

    private String notes;
}

