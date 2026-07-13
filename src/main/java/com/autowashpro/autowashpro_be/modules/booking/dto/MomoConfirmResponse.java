package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoConfirmResponse {
    private String partnerCode;
    private String requestId;
    private String orderId;
    private Integer resultCode;
    private String message;
}
