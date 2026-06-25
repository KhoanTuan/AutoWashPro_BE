package com.autowashpro.autowashpro_be.modules.capacity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
@Schema(description = "Slot capacity snapshot for a given business day")
public class SlotAvailabilityResponse {

    @Schema(example = "1")
    private Integer slotId;

    @Schema(example = "08:00")
    private LocalTime startTime;

    @Schema(example = "09:00")
    private LocalTime endTime;

    @Schema(example = "4")
    private Integer maxCapacity;

    @Schema(description = "Confirmed bookings already occupying this slot on the selected date")
    private Long bookedCount;

    @Schema(description = "Remaining bookable spots")
    private Integer availableSpots;

    @Schema(description = "True when at least one spot remains")
    private Boolean open;
}
