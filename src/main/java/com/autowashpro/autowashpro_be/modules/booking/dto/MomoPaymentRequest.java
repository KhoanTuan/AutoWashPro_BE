package com.autowashpro.autowashpro_be.modules.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MomoPaymentRequest {

    @JsonProperty("partnerCode")
    private String partnerCode;

    @JsonProperty("partnerName")
    private String partnerName;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("orderInfo")
    private String orderInfo;

    @JsonProperty("redirectUrl")
    private String redirectUrl;

    @JsonProperty("ipnUrl")
    private String ipnUrl;

    @JsonProperty("requestType")
    private String requestType;

    @JsonProperty("lang")
    private String lang;

    @JsonProperty("accessKey")
    private String accessKey;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("extraData")
    private String extraData;
}
