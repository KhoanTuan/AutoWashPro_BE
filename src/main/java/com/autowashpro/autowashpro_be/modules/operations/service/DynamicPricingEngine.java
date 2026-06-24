package com.autowashpro.autowashpro_be.modules.operations.service;

import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import com.autowashpro.autowashpro_be.modules.operations.dto.DynamicPricingResultDto;
import com.autowashpro.autowashpro_be.modules.operations.entity.ServicePackage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates dynamic surcharges for shop-floor revenue collection.
 * Applies a 1.2x vehicle multiplier for SUV/TRUCK against canonical package base prices.
 */
@Component
public class DynamicPricingEngine {

    private static final BigDecimal SEDAN_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal SUV_TRUCK_MULTIPLIER = new BigDecimal("1.20");

    public DynamicPricingResultDto calculate(ServicePackage servicePackage, CarType carType) {
        BigDecimal basePrice = servicePackage.getBasePrice();
        BigDecimal multiplier = resolveVehicleMultiplier(carType);
        BigDecimal finalizedTotal = basePrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        BigDecimal surcharge = finalizedTotal.subtract(basePrice);

        return DynamicPricingResultDto.builder()
                .servicePackage(servicePackage.name())
                .carType(carType.name())
                .basePrice(basePrice)
                .vehicleMultiplier(multiplier)
                .surchargeAmount(surcharge)
                .finalizedTotalPrice(finalizedTotal)
                .build();
    }

    public DynamicPricingResultDto calculateFromBasePrice(BigDecimal basePrice, CarType carType) {
        BigDecimal multiplier = resolveVehicleMultiplier(carType);
        BigDecimal finalizedTotal = basePrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        BigDecimal surcharge = finalizedTotal.subtract(basePrice);

        return DynamicPricingResultDto.builder()
                .servicePackage("CATALOG_SERVICE")
                .carType(carType.name())
                .basePrice(basePrice)
                .vehicleMultiplier(multiplier)
                .surchargeAmount(surcharge)
                .finalizedTotalPrice(finalizedTotal)
                .build();
    }

    public ServicePackage resolvePackageFromServiceName(String serviceName) {
        if (serviceName == null) {
            return ServicePackage.STANDARD_WASH;
        }
        String normalized = serviceName.toLowerCase();
        if (normalized.contains("premium") || normalized.contains("detail")) {
            return ServicePackage.PREMIUM_DETAILING;
        }
        if (normalized.contains("comprehensive") || normalized.contains("full")) {
            return ServicePackage.COMPREHENSIVE_CARE;
        }
        return ServicePackage.STANDARD_WASH;
    }

    private BigDecimal resolveVehicleMultiplier(CarType carType) {
        return switch (carType) {
            case SUV, TRUCK -> SUV_TRUCK_MULTIPLIER;
            case SEDAN -> SEDAN_MULTIPLIER;
        };
    }
}
