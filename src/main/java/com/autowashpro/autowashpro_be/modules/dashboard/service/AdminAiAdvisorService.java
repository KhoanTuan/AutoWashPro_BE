package com.autowashpro.autowashpro_be.modules.dashboard.service;

import com.autowashpro.autowashpro_be.modules.dashboard.dto.request.DashboardFilterRequest;
import com.autowashpro.autowashpro_be.modules.dashboard.dto.response.AiAdvisorProposalResponse;

/**
 * Service interface cho Trợ lý AI (E2E-3) phân tích Dashboard và đề xuất chiến dịch Win-back.
 */
public interface AdminAiAdvisorService {

    AiAdvisorProposalResponse analyzeDashboard(DashboardFilterRequest filter);

    boolean applyProposal(String proposalId);
}
