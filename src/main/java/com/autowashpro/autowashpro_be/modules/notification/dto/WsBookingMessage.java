package com.autowashpro.autowashpro_be.modules.notification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsBookingMessage {
    private String type;
    private String bookingCode;
    private Long customerId;
    private String customerName;
    private String licensePlate;
    private String slotTime;
    private String date;
    private String status;
    private String title;
    private String content;
}
