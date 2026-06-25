package com.autowashpro.autowashpro_be.modules.financial.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AiRecommendationResponse {

    private LocalDate analysisDate;
    private String summary;
    private List<String> recommendations;
    private String modelUsed;
}
