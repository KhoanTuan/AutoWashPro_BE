package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for initiating payment checkout for a booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutRequest {

    @NotNull(message = "Booking ID is required")
    @JsonProperty("bookingId")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    @JsonProperty("paymentMethod")
    private String paymentMethod;

    @JsonProperty("voucherCode")
    private String voucherCode;

    @JsonProperty("notes")
    private String notes;
}
