package com.autowashpro.autowashpro_be.modules.operations.entity;

import java.math.BigDecimal;

/**
 * Core wash packages used by the dynamic pricing engine (base prices in VND).
 */
public enum ServicePackage {
    STANDARD_WASH(new BigDecimal("150000")),
    COMPREHENSIVE_CARE(new BigDecimal("350000")),
    PREMIUM_DETAILING(new BigDecimal("850000"));

    private final BigDecimal basePrice;

    ServicePackage(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
