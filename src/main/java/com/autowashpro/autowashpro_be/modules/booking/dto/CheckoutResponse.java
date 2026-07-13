package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for checkout operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("bookingId")
    private Long bookingId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("paymentMethod")
    private String paymentMethod;

    @JsonProperty("paymentUrl")
    private String paymentUrl;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("momoRequestId")
    private String momoRequestId;

    @JsonProperty("momoOrderId")
    private String momoOrderId;
}
