package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlotOptionResponse {
    private Integer slotId;
    private String label;
    private String startTime;
    private String endTime;
}
