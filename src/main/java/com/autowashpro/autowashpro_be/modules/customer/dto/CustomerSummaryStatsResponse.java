package com.autowashpro.autowashpro_be.modules.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerSummaryStatsResponse {
    private long totalCustomers;
    private long activeCustomers;
    private long vipMembers;
    private long newRegistrationsLast30Days;
    private long churnRiskCount;
}
