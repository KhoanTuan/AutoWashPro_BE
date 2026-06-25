package com.autowashpro.autowashpro_be.modules.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Result of dynamic surcharge calculation for revenue collection")
public class DynamicPricingResultDto {

    private String servicePackage;
    private String carType;
    private BigDecimal basePrice;
    private BigDecimal vehicleMultiplier;
    private BigDecimal surchargeAmount;
    private BigDecimal finalizedTotalPrice;
}
