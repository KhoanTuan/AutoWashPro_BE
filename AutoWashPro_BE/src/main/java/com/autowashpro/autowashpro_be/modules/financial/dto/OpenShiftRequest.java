package com.autowashpro.autowashpro_be.modules.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenShiftRequest {

    @DecimalMin("0.00")
    private BigDecimal openingBalance;
}
