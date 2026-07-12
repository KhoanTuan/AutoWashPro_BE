package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for checkout operations.
 * Returns payment gateway URL and transaction details to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {

    /**
     * Payment transaction reference ID in our system
     */
    @JsonProperty("transactionId")
    private String transactionId;

    /**
     * Booking ID associated with this payment
     */
    @JsonProperty("bookingId")
    private Long bookingId;

    /**
     * Payment amount in VND
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Payment method used (e.g., "MOMO")
     */
    @JsonProperty("paymentMethod")
    private String paymentMethod;

    /**
     * URL where customer should be redirected to complete payment (MoMo gateway)
     */
    @JsonProperty("paymentUrl")
    private String paymentUrl;

    /**
     * Transaction status: "PENDING", "PROCESSING", "SUCCESS", "FAILED", etc.
     */
    @JsonProperty("status")
    private String status;

    /**
     * Message describing the checkout result
     */
    @JsonProperty("message")
    private String message;

    /**
     * MoMo request ID for tracking
     */
    @JsonProperty("momoRequestId")
    private String momoRequestId;

    /**
     * MoMo order ID for tracking
     */
    @JsonProperty("momoOrderId")
    private String momoOrderId;
}
