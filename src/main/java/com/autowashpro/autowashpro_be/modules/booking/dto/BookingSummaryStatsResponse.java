package com.autowashpro.autowashpro_be.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingSummaryStatsResponse {
    private long todayTotal;
    private long todayWalkIns;
    private long pendingPayment;
    private long inProgress;
}
