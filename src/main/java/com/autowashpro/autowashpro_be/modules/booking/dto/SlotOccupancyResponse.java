package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotOccupancyResponse {

    @JsonProperty("slotId")
    private Long slotId;

    @JsonProperty("startTime")
    private LocalTime startTime;

    @JsonProperty("endTime")
    private LocalTime endTime;

    @JsonProperty("maxCapacity")
    private Integer maxCapacity;

    @JsonProperty("bookedCount")
    private Integer bookedCount;

    @JsonProperty("lockedCount")
    private Integer lockedCount;

    @JsonProperty("isActive")
    private Boolean isActive;
}
