package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Create a new appointment or walk-in booking")
public class CreateAppointmentRequest {

    @Schema(description = "walk-in or appt (defaults to appt for customers)", example = "appt")
    private String bookingType;

    @Schema(description = "Staff walk-in: existing customer ID")
    private Long customerId;

    @Schema(description = "Staff walk-in: customer full name")
    private String customerName;

    @Schema(description = "Staff walk-in: customer phone")
    private String phone;

    @Schema(description = "Staff walk-in: customer email")
    private String email;

    @Schema(description = "Staff walk-in: license plate")
    private String plate;

    @Schema(description = "Staff walk-in: vehicle type", example = "SEDAN")
    private String vehicleType;

    @Schema(description = "Customer app: owned vehicle ID")
    private Long vehicleId;

    @NotNull
    @Schema(example = "1")
    private Integer serviceId;

    @NotNull
    @Schema(example = "3")
    private Integer slotId;

    @NotBlank
    @Schema(example = "2026-06-24")
    private String bookingDate;

    private String notes;
}
