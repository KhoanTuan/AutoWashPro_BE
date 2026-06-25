package com.autowashpro.autowashpro_be.modules.financial.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MomoPaymentResponse {

    private String partnerCode;
    private String requestId;
    private String orderId;
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
}
