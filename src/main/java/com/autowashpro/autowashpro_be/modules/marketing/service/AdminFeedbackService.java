package com.autowashpro.autowashpro_be.modules.marketing.service;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.FeedbackResolveRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminFeedbackService {
    Page<FeedbackResponse> getFeedbacks(FeedbackStatus status, Integer ratingLte, Pageable pageable);
    FeedbackResponse resolveFeedback(Long id, FeedbackResolveRequest request);
}
