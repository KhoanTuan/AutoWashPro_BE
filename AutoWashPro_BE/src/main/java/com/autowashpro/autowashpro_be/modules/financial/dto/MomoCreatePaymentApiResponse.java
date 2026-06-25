package com.autowashpro.autowashpro_be.modules.financial.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoCreatePaymentApiResponse {

    private String partnerCode;
    private String requestId;
    private String orderId;
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
}
