package com.autowashpro.autowashpro_be.modules.customer.dto;

import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltySettingsResponse {
    private LoyaltyConfig config;
    private List<LoyaltyTierResponse> tiers;
}
