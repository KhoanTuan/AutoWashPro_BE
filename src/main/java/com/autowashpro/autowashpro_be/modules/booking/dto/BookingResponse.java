package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.autowashpro.autowashpro_be.modules.booking.entity.BookingStatus;
import com.autowashpro.autowashpro_be.modules.booking.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long bookingId;
    private String bookingCode;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String licensePlate;
    private String model;
    private LocalDate bookingDate;
    private Long timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalEstimatedAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String notes;
    private List<BookingItemResponse> items;
    private LocalDateTime createdAt;
}
