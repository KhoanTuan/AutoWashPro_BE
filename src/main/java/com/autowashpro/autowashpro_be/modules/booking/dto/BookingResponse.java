package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private String bookingCode;
    private String customerName;
    private String customerPhone;
    private String membership;
    private String plate;
    private String slotLabel;
    private String bookingType;
    private String bookingTypeLabel;
    private String serviceName;
    private BigDecimal totalAmount;
    private String bookingStatus;
    private String bookingStatusLabel;
    private String paymentStatus;
    private String paymentStatusLabel;
    private Long technicianId;
    private String technicianName;
    private String notes;
    private LocalDate bookingDate;
    private LocalDateTime createdAt;
    private String action;
}
