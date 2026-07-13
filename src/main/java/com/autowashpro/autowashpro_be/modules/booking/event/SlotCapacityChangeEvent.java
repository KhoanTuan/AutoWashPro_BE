package com.autowashpro.autowashpro_be.modules.booking.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDate;

@Getter
public class SlotCapacityChangeEvent extends ApplicationEvent {
    private final LocalDate date;
    private final Long slotId;

    public SlotCapacityChangeEvent(Object source, LocalDate date, Long slotId) {
        super(source);
        this.date = date;
        this.slotId = slotId;
    }
}
