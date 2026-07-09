package com.autowashpro.autowashpro_be.modules.marketing.service;

import com.autowashpro.autowashpro_be.modules.marketing.dto.request.CustomerFeedbackCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;

import java.util.List;

public interface CustomerFeedbackService {
    FeedbackResponse createFeedback(Long customerId, CustomerFeedbackCreateRequest request);
    List<FeedbackResponse> getMyFeedbacks(Long customerId);
}
