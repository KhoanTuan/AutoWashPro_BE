package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoPosCallbackRequest {
    private String partnerCode;
    private String accessKey;
    private Long amount;
    private String partnerRefId; // This is the billId/orderId we generated
    private String partnerTransId;
    private String transType;
    private String momoTransId;
    private Integer status; // 0 for success
    private String message;
    private Long responseTime;
    private String storeId;
    private String signature;
}
