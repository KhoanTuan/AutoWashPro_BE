package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotResponse {
    private Long slotId;

    @Schema(type = "string", example = "08:00")
    private LocalTime startTime;

    @Schema(type = "string", example = "09:00")
    private LocalTime endTime;

    private Integer maxCapacity;
    private Boolean isActive;
    private Integer displayOrder;
    private String dayOfWeek;
}

