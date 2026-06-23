package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ServiceCatalogResponse {
    private Integer serviceId;
    private String name;
    private String duration;
    private BigDecimal basePrice;
    private Map<String, BigDecimal> pricesByCarType;
}
