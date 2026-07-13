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

    @JsonProperty("partnerCode")
    private String partnerCode;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("resultMessage")
    private String resultMessage;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("transId")
    private Long transId;

    @JsonProperty("payUrl")
    private String payUrl;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("responseTime")
    private Long responseTime;

    @JsonProperty("signature")
    private String signature;
}
