package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogResponse {
    private Long serviceId;
    private String serviceCode;
    private String serviceName;
    private ServiceType serviceType;
    private BigDecimal price;
    private Integer durationMinutes;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
