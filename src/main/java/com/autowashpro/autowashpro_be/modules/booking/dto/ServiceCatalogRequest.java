package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogRequest {

    @NotBlank(message = "Service code must not be empty")
    @Size(max = 50, message = "Service code exceeds 50 characters")
    private String serviceCode;

    @NotBlank(message = "Service name must not be empty")
    @Size(max = 150, message = "Service name exceeds 150 characters")
    private String serviceName;

    @NotNull(message = "Service type must not be null")
    private ServiceType serviceType;

    @NotNull(message = "Price must not be null")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Duration must not be null")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer displayOrder = 0;
}
