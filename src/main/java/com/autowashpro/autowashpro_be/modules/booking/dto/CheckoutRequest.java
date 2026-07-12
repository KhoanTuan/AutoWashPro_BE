package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for initiating payment checkout for a booking.
 * Used to trigger MoMo payment gateway integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutRequest {

    /**
     * Booking ID to process payment for
     */
    @NotNull(message = "Booking ID is required")
    @JsonProperty("bookingId")
    private Long bookingId;

    /**
     * Payment method: "MOMO", "BANK_TRANSFER", "CASH", etc.
     */
    @NotNull(message = "Payment method is required")
    @JsonProperty("paymentMethod")
    private String paymentMethod;

    /**
     * Optional customer notes
     */
    @JsonProperty("notes")
    private String notes;
}
