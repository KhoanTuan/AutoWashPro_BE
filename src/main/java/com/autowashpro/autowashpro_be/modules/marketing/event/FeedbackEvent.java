package com.autowashpro.autowashpro_be.modules.marketing.event;

import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerFeedback;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
@JsonIgnoreProperties({"source"})
public class FeedbackEvent extends ApplicationEvent {
    @JsonIgnore
    private final CustomerFeedback feedback;
    private final Long feedbackId;
    private final String bookingCode;
    private final String action;

    public FeedbackEvent(Object source, CustomerFeedback feedback, String action) {
        super(source);
        this.feedback = feedback;
        this.feedbackId = feedback != null ? feedback.getId() : null;
        this.bookingCode = feedback != null ? feedback.getBookingId() : null;
        this.action = action;
    }
}
