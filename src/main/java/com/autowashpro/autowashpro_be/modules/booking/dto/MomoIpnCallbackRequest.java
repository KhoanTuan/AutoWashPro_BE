package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Data class representing the IPN (Instant Payment Notification) callback payload received from MoMo.
 * This is sent by MoMo to notify the backend about payment transaction status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MomoIpnCallbackRequest {

    @JsonProperty("partnerCode")
    private String partnerCode;

    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("orderInfo")
    private String orderInfo;

    @JsonProperty("orderType")
    private String orderType;

    /**
     * Transaction status result code:
     * 0 = Successful payment
     * 1 = Transaction denied
     * 2 = Transaction error
     * Other codes represent various failure scenarios
     */
    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("resultMessage")
    private String resultMessage;

    @JsonProperty("transId")
    private Long transId;

    @JsonProperty("responseTime")
    private Long responseTime;

    @JsonProperty("paymentOption")
    private String paymentOption;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("extraData")
    private String extraData;
}
