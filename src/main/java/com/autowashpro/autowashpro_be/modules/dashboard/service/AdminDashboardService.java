package com.autowashpro.autowashpro_be.modules.dashboard.service;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.*;
import java.util.List;

/**
 * Service interface quản lý nghiệp vụ hiển thị số liệu Command Center Dashboard.
 */
public interface AdminDashboardService {

    DashboardKpiSummaryResponse getKpiSummary(DashboardFilterRequest filter);

    List<RevenueTrendResponse> getRevenueTrends(DashboardFilterRequest filter);

    BookingDistributionResponse getBookingDistribution(DashboardFilterRequest filter);

    List<SlotPerformanceResponse> getSlotPerformances(DashboardFilterRequest filter);
}
