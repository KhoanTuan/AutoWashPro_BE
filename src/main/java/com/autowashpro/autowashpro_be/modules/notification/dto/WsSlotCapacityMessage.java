package com.autowashpro.autowashpro_be.modules.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsSlotCapacityMessage {
    private String type;
    private String date;
    private Long timeSlotId;
    private int availableCapacity;
    private boolean isFull;
}
