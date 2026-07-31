package com.autowashpro.autowashpro_be.modules.marketing.service;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.DirectGrantRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.PromotionCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.TargetPreviewRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.TargetPreviewResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionKpiSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminPromotionService {
    PromotionResponse createPromotion(PromotionCreateRequest request);
    PromotionResponse updatePromotion(Long id, PromotionCreateRequest request);
    Page<PromotionResponse> getPromotions(PromotionStatus status, String keyword, Pageable pageable);
    PromotionResponse getPromotionById(Long id);
    PromotionResponse updateStatus(Long id, PromotionStatus status);
    void deletePromotion(Long id);
    TargetPreviewResponse previewTarget(TargetPreviewRequest request);
    List<String> grantDirect(DirectGrantRequest request);
    PromotionKpiSummaryResponse getKpiSummary();
}
