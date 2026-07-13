package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoPosCallbackResponse {
    private Integer status;
    private String message;
    private Long amount;
    private String partnerRefId;
    private String momoTransId;
    private String signature;
}
