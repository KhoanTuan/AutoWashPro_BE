package com.autowashpro.autowashpro_be.modules.dashboard.controller;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.*;
import com.autowashpro.autowashpro_be.modules.dashboard.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "08 - Admin Dashboard", description = "Command Center Dashboard & KPI Analytics — trang `/admin/dashboard`")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/kpi-summary")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[READ] Thẻ KPI thông số tổng quan", description = "Trả về 4 thẻ KPI động theo tham số lọc timeRange=TODAY|WEEK|MONTH|YEAR|CUSTOM.")
    public ResponseEntity<DashboardKpiSummaryResponse> getKpiSummary(@ParameterObject DashboardFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getKpiSummary(filter));
    }

    @GetMapping("/revenue-trends")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[READ] Biểu đồ 1: Stacked Bar cơ cấu dịch vụ & đường AOV", description = "Trả về doanh thu phân tầng 3 gói rửa xe kết hợp đường xu hướng giá trị đơn hàng trung bình.")
    public ResponseEntity<List<RevenueTrendResponse>> getRevenueTrends(@ParameterObject DashboardFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getRevenueTrends(filter));
    }

    @GetMapping("/booking-distribution")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[READ] Biểu đồ Donut: Phân phối trạng thái đơn hàng", description = "Trả về tỷ lệ % và số lượng theo 4 trạng thái đặt lịch (Completed, Paid, Unpaid, Cancelled).")
    public ResponseEntity<BookingDistributionResponse> getBookingDistribution(@ParameterObject DashboardFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getBookingDistribution(filter));
    }

    @GetMapping("/slot-performance")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD_STATS')")
    @Operation(summary = "[READ] Biểu đồ 2: Hiệu suất 12 Khung giờ E2E-1 & Cảnh báo rủi ro", description = "Trả về % lấp đầy và % hủy/trễ theo các slot cấu hình E2E-1. Tự động bật cờ isHighRisk khi lấp đầy < 50% và no-show > 20%.")
    public ResponseEntity<List<SlotPerformanceResponse>> getSlotPerformance(@ParameterObject DashboardFilterRequest filter) {
        return ResponseEntity.ok(dashboardService.getSlotPerformances(filter));
    }
}
