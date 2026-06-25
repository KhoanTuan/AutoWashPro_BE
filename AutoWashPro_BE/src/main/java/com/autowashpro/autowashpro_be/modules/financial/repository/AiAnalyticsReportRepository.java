package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.AiAnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiAnalyticsReportRepository extends JpaRepository<AiAnalyticsReport, Long> {

    Optional<AiAnalyticsReport> findByAnalysisDate(LocalDate analysisDate);

    Optional<AiAnalyticsReport> findTopByOrderByAnalysisDateDesc();
}
