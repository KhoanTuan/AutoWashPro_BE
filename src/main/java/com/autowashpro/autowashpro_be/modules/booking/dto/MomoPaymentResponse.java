package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MomoPaymentResponse {

    /**
     * Partner code from the request
     */
    @JsonProperty("partnerCode")
    private String partnerCode;

    /**
     * Request ID from the request
     */
    @JsonProperty("requestId")
    private String requestId;

    /**
     * Result code from MoMo
     * 0 = Success
     * 1001 = Transaction declined
     * 1002 = Transaction declined (insufficient balance, etc.)
     * 1003 = Transaction timeout
     * 1004 = Service unavailable
     * 1111 = Merchant maintenance
     * etc.
     */
    @JsonProperty("resultCode")
    private Integer resultCode;

    /**
     * Result message describing the result code
     */
    @JsonProperty("resultMessage")
    private String resultMessage;

    /**
     * MoMo order ID for the payment
     */
    @JsonProperty("orderId")
    private String orderId;

    /**
     * MoMo transaction reference number
     */
    @JsonProperty("transId")
    private Long transId;

    /**
     * Payment gateway URL for customer to complete payment
     */
    @JsonProperty("payUrl")
    private String payUrl;

    /**
     * Amount in VND
     */
    @JsonProperty("amount")
    private Long amount;

    /**
     * Response timestamp
     */
    @JsonProperty("responseTime")
    private Long responseTime;

    /**
     * HMAC-SHA256 signature for response validation
     */
    @JsonProperty("signature")
    private String signature;
}
