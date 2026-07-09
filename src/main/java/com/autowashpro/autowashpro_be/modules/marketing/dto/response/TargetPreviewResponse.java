package com.autowashpro.autowashpro_be.modules.marketing.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetPreviewResponse {
    private long estimatedCustomerCount;
    private String message;
}
