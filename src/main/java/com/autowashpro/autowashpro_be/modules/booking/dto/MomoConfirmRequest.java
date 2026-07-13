package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoConfirmRequest {
    private String partnerCode;
    private String requestId;
    private String orderId;
    private String requestType; // "capture" or "cancel"
    private String lang;
    private Long amount;
    private String description;
    private String signature;
}
