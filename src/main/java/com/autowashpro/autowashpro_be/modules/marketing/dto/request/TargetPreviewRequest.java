package com.autowashpro.autowashpro_be.modules.marketing.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetPreviewRequest {
    private String minTier;
    private Integer minRecencyDays;
}
