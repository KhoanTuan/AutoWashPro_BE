package com.autowashpro.autowashpro_be.modules.booking.event;

import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
@JsonIgnoreProperties({"source"})
public class BookingEvent extends ApplicationEvent {
    @JsonIgnore
    private final Booking booking;
    private final Long bookingId;
    private final String bookingCode;
    private final BookingEventAction action;
    private final String customTitle;
    private final String customContent;

    public BookingEvent(Object source, Booking booking, BookingEventAction action) {
        super(source);
        this.booking = booking;
        this.bookingId = booking != null ? booking.getBookingId() : null;
        this.bookingCode = booking != null ? booking.getBookingCode() : null;
        this.action = action;
        this.customTitle = null;
        this.customContent = null;
    }

    public BookingEvent(Object source, Booking booking, BookingEventAction action, String customTitle, String customContent) {
        super(source);
        this.booking = booking;
        this.bookingId = booking != null ? booking.getBookingId() : null;
        this.bookingCode = booking != null ? booking.getBookingCode() : null;
        this.action = action;
        this.customTitle = customTitle;
        this.customContent = customContent;
    }
}
