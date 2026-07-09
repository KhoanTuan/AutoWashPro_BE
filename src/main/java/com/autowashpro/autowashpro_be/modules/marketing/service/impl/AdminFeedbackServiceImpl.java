package com.autowashpro.autowashpro_be.modules.marketing.service.impl;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.FeedbackResolveRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.FeedbackResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.*;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerFeedbackRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.PromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private final CustomerFeedbackRepository customerFeedbackRepository;
    private final PromotionRepository promotionRepository;
    private final CustomerPromotionRepository customerPromotionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbacks(FeedbackStatus status, Integer ratingLte, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        return customerFeedbackRepository.findByFilter(statusStr, ratingLte, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public FeedbackResponse resolveFeedback(Long id, FeedbackResolveRequest request) {
        CustomerFeedback feedback = customerFeedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ý kiến khiếu nại với ID: " + id));

        feedback.setResolutionNotes(request.getResolutionNotes());
        feedback.setStatus(FeedbackStatus.RESOLVED);

        if (request.isGrantCompensationVoucher()) {
            Customer customer = feedback.getCustomer();
            String code = request.getVoucherCode() != null && !request.getVoucherCode().isBlank() 
                          ? request.getVoucherCode() : "COMPENSATE50";
            BigDecimal val = request.getDiscountValue() != null ? request.getDiscountValue() : BigDecimal.valueOf(50000);

            Promotion promotion = promotionRepository.findByCode(code).orElseGet(() -> {
                return promotionRepository.save(Promotion.builder()
                        .code(code)
                        .name("Voucher Đền Bù Tạ Lỗi CSKH")
                        .description("Voucher đền bù trải nghiệm dịch vụ chưa hài lòng.")
                        .discountType(DiscountType.FIXED_AMOUNT)
                        .value(val)
                        .costPoints(0)
                        .minTier("Member")
                        .minRecencyDays(0)
                        .totalBudget(10000)
                        .issuedCount(0)
                        .redeemedCount(0)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusMonths(3))
                        .status(PromotionStatus.ACTIVE)
                        .build());
            });

            String voucherCode = "VOU-" + promotion.getCode() + "-" + customer.getCustomerId() + "-" + (System.currentTimeMillis() % 10000);
            CustomerPromotion cp = CustomerPromotion.builder()
                    .customer(customer)
                    .promotion(promotion)
                    .voucherCode(voucherCode)
                    .issuedAt(LocalDateTime.now())
                    .expiryDate(LocalDateTime.now().plusMonths(1))
                    .status(CustomerPromotionStatus.ISSUED)
                    .source(CustomerPromotionSource.COMPENSATION)
                    .build();
            customerPromotionRepository.save(cp);

            promotion.setIssuedCount((promotion.getIssuedCount() != null ? promotion.getIssuedCount() : 0) + 1);
            promotionRepository.save(promotion);

            feedback.setCompensationVoucherCode(voucherCode);
            log.info("Issued compensation voucher {} to customer {}", voucherCode, customer.getFullName());
        }

        return mapToResponse(customerFeedbackRepository.save(feedback));
    }

    private FeedbackResponse mapToResponse(CustomerFeedback f) {
        Customer c = f.getCustomer();
        return FeedbackResponse.builder()
                .id(f.getId())
                .customerId(c != null ? c.getCustomerId() : null)
                .customerName(c != null ? c.getFullName() : "Khách ẩn danh")
                .customerPhone(c != null ? c.getPhoneNumber() : "")
                .customerAvatar(c != null ? "https://api.dicebear.com/7.x/avataaars/svg?seed=" + c.getPhoneNumber() : null)
                .bookingId(f.getBookingId())
                .serviceName(f.getServiceName())
                .ratingStars(f.getRatingStars())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .status(f.getStatus())
                .resolutionNotes(f.getResolutionNotes())
                .compensationVoucherCode(f.getCompensationVoucherCode())
                .build();
    }
}
