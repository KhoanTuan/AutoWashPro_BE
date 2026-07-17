package com.autowashpro.autowashpro_be.modules.booking.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class SlotCapacityChangeEvent {
    private final LocalDate date;
    private final Long slotId;
}
