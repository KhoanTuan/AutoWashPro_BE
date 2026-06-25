package com.autowashpro.autowashpro_be.modules.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShiftClosureRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal actualBalance;

    private String notes;
}
