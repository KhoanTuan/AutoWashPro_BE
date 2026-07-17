package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotLockResponse {
    private Long closureId;
    private LocalDate closureDate;
    private String reason;
    private Boolean isFullDay;
    private Long slotId;
    private String startTime;
}
