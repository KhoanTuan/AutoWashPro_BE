package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemResponse {
    private Long bookingItemId;
    private Long serviceId;
    private String serviceCodeSnapshot;
    private String serviceNameSnapshot;
    private ServiceType serviceTypeSnapshot;
    private BigDecimal priceSnapshot;
}
