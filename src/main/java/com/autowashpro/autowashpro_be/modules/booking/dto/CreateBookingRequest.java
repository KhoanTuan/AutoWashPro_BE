package com.autowashpro.autowashpro_be.modules.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Admin tạo booking walk-in / đặt lịch")
public class CreateBookingRequest {

    @Schema(description = "walk-in hoặc appt", example = "walk-in")
    private String bookingType;

    @Schema(description = "ID khách có sẵn — bỏ trống nếu khách mới")
    private Long customerId;

    @NotBlank
    private String customerName;

    private String phone;
    private String email;

    @NotBlank
    private String plate;

    @Schema(example = "SEDAN")
    private String vehicleType;

    @NotNull
    private Integer serviceId;

    @NotNull
    private Integer slotId;

    @NotBlank
    private String bookingDate;

    private String notes;
}
