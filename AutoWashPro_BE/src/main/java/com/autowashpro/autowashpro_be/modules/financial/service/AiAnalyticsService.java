package com.autowashpro.autowashpro_be.modules.financial.service;

import com.autowashpro.autowashpro_be.modules.financial.config.GeminiProperties;
import com.autowashpro.autowashpro_be.modules.financial.dto.AiRecommendationResponse;
import com.autowashpro.autowashpro_be.modules.financial.entity.AiAnalyticsReport;
import com.autowashpro.autowashpro_be.modules.financial.entity.FinancialLedger;
import com.autowashpro.autowashpro_be.modules.financial.repository.AiAnalyticsReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalyticsService {

    private final GeminiProperties geminiProperties;
    private final AiAnalyticsReportRepository aiAnalyticsReportRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AiRecommendationResponse getLatestRecommendations() {
        Optional<AiAnalyticsReport> report = aiAnalyticsReportRepository.findTopByOrderByAnalysisDateDesc();
        return report.map(this::toResponse)
                .orElse(AiRecommendationResponse.builder()
                        .analysisDate(LocalDate.now())
                        .summary("No AI analytics report available yet.")
                        .recommendations(List.of())
                        .modelUsed(geminiProperties.getModel())
                        .build());
    }

    @Async
    @Transactional
    public void generateRecommendationsAsync(FinancialLedger ledger) {
        try {
            generateAndPersist(ledger);
        } catch (Exception ex) {
            log.warn("AI analytics generation failed for ledger {}: {}", ledger.getLedgerId(), ex.getMessage());
        }
    }

    @Transactional
    public AiRecommendationResponse generateAndPersist(FinancialLedger ledger) {
        String prompt = buildPrompt(ledger);
        String aiText = callGeminiApi(prompt);
        List<String> recommendations = parseRecommendations(aiText);

        AiAnalyticsReport report = AiAnalyticsReport.builder()
                .ledger(ledger)
                .analysisDate(ledger.getLedgerDate())
                .summaryText(extractSummary(aiText))
                .recommendations(String.join("\n", recommendations))
                .modelUsed(geminiProperties.getModel())
                .build();
        aiAnalyticsReportRepository.save(report);

        return AiRecommendationResponse.builder()
                .analysisDate(report.getAnalysisDate())
                .summary(report.getSummaryText())
                .recommendations(recommendations)
                .modelUsed(report.getModelUsed())
                .build();
    }

    private String buildPrompt(FinancialLedger ledger) {
        return """
                You are a car wash business financial advisor. Analyze the following daily ledger and provide:
                1) A concise executive summary (2-3 sentences)
                2) Exactly 3 actionable management recommendations as bullet points

                Ledger date: %s
                Opening balance: %s VND
                Total cash: %s VND
                Total MoMo: %s VND
                Total revenue: %s VND
                Total expenses: %s VND
                Closing balance: %s VND
                Notes: %s
                """.formatted(
                ledger.getLedgerDate(),
                ledger.getOpeningBalance(),
                ledger.getTotalCash(),
                ledger.getTotalMomo(),
                ledger.getTotalRevenue(),
                ledger.getTotalExpenses(),
                ledger.getClosingBalance(),
                ledger.getSummaryNotes() != null ? ledger.getSummaryNotes() : "N/A"
        );
    }

    private String callGeminiApi(String prompt) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            return fallbackAnalysis(prompt);
        }

        String url = geminiProperties.getEndpoint() + "/" + geminiProperties.getModel()
                + ":generateContent?key=" + geminiProperties.getApiKey();

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        try {
            String response = restTemplate.postForObject(url, body, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText();
            }
        } catch (Exception ex) {
            log.warn("Gemini API call failed, using fallback: {}", ex.getMessage());
        }
        return fallbackAnalysis(prompt);
    }

    private String fallbackAnalysis(String prompt) {
        return """
                Summary: Revenue patterns indicate stable operations. Monitor cash-to-digital payment ratio for optimization.

                Recommendations:
                - Review peak-hour staffing against revenue collected today
                - Promote MoMo split-payment for faster checkout during rush hours
                - Investigate any flagged shift closures before next business day
                """;
    }

    private String extractSummary(String aiText) {
        int recIndex = aiText.toLowerCase().indexOf("recommendation");
        if (recIndex > 0) {
            return aiText.substring(0, recIndex).trim();
        }
        return aiText.length() > 500 ? aiText.substring(0, 500) : aiText;
    }

    private List<String> parseRecommendations(String aiText) {
        List<String> items = new ArrayList<>();
        for (String line : aiText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches("^\\d+\\..*")) {
                items.add(trimmed.replaceFirst("^[-*\\d.\\s]+", "").trim());
            }
        }
        if (items.isEmpty()) {
            items.add("Review daily cash reconciliation reports");
            items.add("Track MoMo vs cash mix for operational efficiency");
            items.add("Schedule rescue promotions for at-risk customer segments");
        }
        return items.stream().limit(5).toList();
    }

    private AiRecommendationResponse toResponse(AiAnalyticsReport report) {
        List<String> recommendations = report.getRecommendations() != null
                ? List.of(report.getRecommendations().split("\n"))
                : List.of();
        return AiRecommendationResponse.builder()
                .analysisDate(report.getAnalysisDate())
                .summary(report.getSummaryText())
                .recommendations(recommendations)
                .modelUsed(report.getModelUsed())
                .build();
    }
}
