package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonProperty("maxCapacity")
    private Integer maxCapacity;

    @JsonProperty("bookedCount")
    private Integer bookedCount;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("isLocked")
    private Boolean isLocked;
}
