package com.autowashpro.autowashpro_be.modules.dashboard.service.impl;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.*;
import com.autowashpro.autowashpro_be.modules.dashboard.repository.DashboardQueryRepository;
import com.autowashpro.autowashpro_be.modules.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Implementation cho AdminDashboardService.
 * Chuyển đổi tham số timeRange thành mốc ngày thực tế và gọi sang Repository tổng hợp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final DashboardQueryRepository queryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardKpiSummaryResponse getKpiSummary(DashboardFilterRequest filter) {
        DateRange range = resolveDateRange(filter);
        log.info("Fetching KPI Summary for range: {} ({} to {})", filter.getTimeRange(), range.from(), range.to());
        return queryRepository.getKpiSummary(range.from(), range.to(), filter.getTimeRange());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueTrendResponse> getRevenueTrends(DashboardFilterRequest filter) {
        DateRange range = resolveDateRange(filter);
        log.info("Fetching Revenue Trends for range: {} ({} to {})", filter.getTimeRange(), range.from(), range.to());
        return queryRepository.getRevenueTrends(range.from(), range.to(), filter.getTimeRange());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDistributionResponse getBookingDistribution(DashboardFilterRequest filter) {
        DateRange range = resolveDateRange(filter);
        log.info("Fetching Booking Distribution for range: {} ({} to {})", filter.getTimeRange(), range.from(), range.to());
        return queryRepository.getBookingDistribution(range.from(), range.to());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotPerformanceResponse> getSlotPerformances(DashboardFilterRequest filter) {
        DateRange range = resolveDateRange(filter);
        log.info("Fetching Slot Performances (E2E-1) for range: {} ({} to {})", filter.getTimeRange(), range.from(), range.to());
        return queryRepository.getSlotPerformances(range.from(), range.to());
    }

    private DateRange resolveDateRange(DashboardFilterRequest filter) {
        String tr = filter.getTimeRange() != null ? filter.getTimeRange().toUpperCase() : "MONTH";
        LocalDate today = LocalDate.now();

        return switch (tr) {
            case "TODAY" -> new DateRange(today, today);
            case "WEEK" -> new DateRange(
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            );
            case "YEAR" -> new DateRange(
                    today.withDayOfYear(1),
                    today.withDayOfYear(today.lengthOfYear())
            );
            case "CUSTOM" -> {
                LocalDate from = filter.getFromDate() != null ? filter.getFromDate() : today.minusDays(30);
                LocalDate to = filter.getToDate() != null ? filter.getToDate() : today;
                yield new DateRange(from, to);
            }
            default -> new DateRange( // MONTH
                    today.withDayOfMonth(1),
                    today.withDayOfMonth(today.lengthOfMonth())
            );
        };
    }

    private record DateRange(LocalDate from, LocalDate to) {}
}
