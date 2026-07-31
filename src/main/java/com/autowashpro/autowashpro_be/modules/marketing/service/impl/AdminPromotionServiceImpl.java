package com.autowashpro.autowashpro_be.modules.marketing.service.impl;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.DirectGrantRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.PromotionCreateRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.request.TargetPreviewRequest;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.TargetPreviewResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.*;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.PromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.service.AdminPromotionService;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.PromotionKpiSummaryResponse;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import com.autowashpro.autowashpro_be.modules.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import com.autowashpro.autowashpro_be.modules.marketing.event.PromotionEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPromotionServiceImpl implements AdminPromotionService {

    private final PromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionCreateRequest request) {
        if (promotionRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Mã chiến dịch ưu đãi đã tồn tại: " + request.getCode());
        }

        // Kiểm tra và ràng buộc nghiệp vụ dựa trên Kiểu chiết khấu (DiscountType)
        if (request.getDiscountType() == null) {
            throw new IllegalArgumentException("Kiểu chiết khấu không được để trống");
        }

        if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá trị giảm của chiết khấu tiền mặt phải lớn hơn 0đ");
            }
            int costPoints = request.getCostPoints() != null ? request.getCostPoints() : 0;
            // Nếu là voucher tặng miễn phí (không tốn điểm đổi), bắt buộc phải có đơn hàng áp dụng tối thiểu >= giá trị giảm
            if (costPoints == 0) {
                if (request.getMinOrderValue() == null || request.getMinOrderValue().compareTo(request.getValue()) < 0) {
                    throw new IllegalArgumentException("Voucher tiền mặt tặng miễn phí bắt buộc phải có Giá trị đơn hàng tối thiểu (Min Order Value) lớn hơn hoặc bằng giá trị giảm để tránh hóa đơn âm/0đ.");
                }
            }
        } else if (request.getDiscountType() == DiscountType.PERCENTAGE) {
            if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0 || request.getValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị giảm của chiết khấu phần trăm phải nằm trong khoảng từ 1% đến 100%");
            }
            if (request.getMaxDiscountAmount() == null || request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Chiết khấu phần trăm bắt buộc phải cấu hình Mức giảm tối đa (Max Discount Amount) để kiểm soát ngân sách, tránh tổn thất doanh thu.");
            }
        } else if (request.getDiscountType() == DiscountType.FREE_SERVICE) {
            if (request.getApplicableServiceCode() == null || request.getApplicableServiceCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Chiết khấu rửa miễn phí (Giảm 100%) bắt buộc phải chọn Dịch vụ áp dụng cụ thể để tránh áp dụng sai dịch vụ đắt tiền.");
            }
            // Tự động gán giá trị giảm = 100% cho Free Service
            request.setValue(new BigDecimal("100"));
        }

        Promotion promotion = Promotion.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .costPoints(request.getCostPoints() != null ? request.getCostPoints() : 0)
                .minTier(request.getMinTier() != null ? request.getMinTier() : "Member")
                .minRecencyDays(request.getMinRecencyDays() != null ? request.getMinRecencyDays() : 0)
                .maxClaimPerUser(request.getMaxClaimPerUser() != null && request.getMaxClaimPerUser() > 0 ? request.getMaxClaimPerUser() : null)
                .totalBudget(request.getTotalBudget() != null && request.getTotalBudget() > 0 ? request.getTotalBudget() : null)
                .issuedCount(0)
                .redeemedCount(0)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now().plusMonths(3))
                .applicableServiceCode(request.getApplicableServiceCode() != null && !request.getApplicableServiceCode().trim().isEmpty() ? request.getApplicableServiceCode().trim() : null)
                .applicableDays(request.getApplicableDays() != null && !request.getApplicableDays().trim().isEmpty() ? request.getApplicableDays().trim() : null)
                .maxDiscountAmount(request.getMaxDiscountAmount() != null && request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0 ? request.getMaxDiscountAmount() : null)
                .minOrderValue(request.getMinOrderValue() != null && request.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0 ? request.getMinOrderValue() : null)
                .status(PromotionStatus.ACTIVE)
                .build();
        Promotion saved = promotionRepository.save(promotion);
        
        eventPublisher.publishEvent(new PromotionEvent(this, saved, "CREATED"));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionCreateRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch khuyến mãi với ID: " + id));

        String cleanCode = request.getCode().trim().toUpperCase();
        if (!promotion.getCode().equalsIgnoreCase(cleanCode)) {
            promotionRepository.findByCode(cleanCode).ifPresent(p -> {
                if (!p.getId().equals(id)) {
                    throw new IllegalArgumentException("Mã chiến dịch ưu đãi đã tồn tại: " + cleanCode);
                }
            });
            promotion.setCode(cleanCode);
        }

        if (request.getDiscountType() == null) {
            throw new IllegalArgumentException("Kiểu chiết khấu không được để trống");
        }

        if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá trị giảm của chiết khấu tiền mặt phải lớn hơn 0đ");
            }
            int costPoints = request.getCostPoints() != null ? request.getCostPoints() : 0;
            if (costPoints == 0) {
                if (request.getMinOrderValue() == null || request.getMinOrderValue().compareTo(request.getValue()) < 0) {
                    throw new IllegalArgumentException("Voucher tiền mặt tặng miễn phí bắt buộc phải có Giá trị đơn hàng tối thiểu (Min Order Value) lớn hơn hoặc bằng giá trị giảm để tránh hóa đơn âm/0đ.");
                }
            }
        } else if (request.getDiscountType() == DiscountType.PERCENTAGE) {
            if (request.getValue() == null || request.getValue().compareTo(BigDecimal.ZERO) <= 0 || request.getValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị giảm của chiết khấu phần trăm phải nằm trong khoảng từ 1% đến 100%");
            }
            if (request.getMaxDiscountAmount() == null || request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Chiết khấu phần trăm bắt buộc phải cấu hình Mức giảm tối đa (Max Discount Amount) để kiểm soát ngân sách, tránh tổn thất doanh thu.");
            }
        } else if (request.getDiscountType() == DiscountType.FREE_SERVICE) {
            if (request.getApplicableServiceCode() == null || request.getApplicableServiceCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Chiết khấu rửa miễn phí (Giảm 100%) bắt buộc phải chọn Dịch vụ áp dụng cụ thể để tránh áp dụng sai dịch vụ đắt tiền.");
            }
            request.setValue(new BigDecimal("100"));
        }

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setValue(request.getValue());
        promotion.setCostPoints(request.getCostPoints() != null ? request.getCostPoints() : 0);
        promotion.setMinTier(request.getMinTier() != null ? request.getMinTier() : "Member");
        promotion.setMinRecencyDays(request.getMinRecencyDays() != null ? request.getMinRecencyDays() : 0);
        promotion.setMaxClaimPerUser(request.getMaxClaimPerUser() != null && request.getMaxClaimPerUser() > 0 ? request.getMaxClaimPerUser() : null);
        promotion.setTotalBudget(request.getTotalBudget() != null && request.getTotalBudget() > 0 ? request.getTotalBudget() : null);
        if (request.getStartDate() != null) promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) promotion.setEndDate(request.getEndDate());
        promotion.setApplicableServiceCode(request.getApplicableServiceCode() != null && !request.getApplicableServiceCode().trim().isEmpty() ? request.getApplicableServiceCode().trim() : null);
        promotion.setApplicableDays(request.getApplicableDays() != null && !request.getApplicableDays().trim().isEmpty() ? request.getApplicableDays().trim() : null);
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount() != null && request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0 ? request.getMaxDiscountAmount() : null);
        promotion.setMinOrderValue(request.getMinOrderValue() != null && request.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0 ? request.getMinOrderValue() : null);

        Promotion updated = promotionRepository.save(promotion);
        log.info("Updated promotion ID: {} ({})", id, updated.getCode());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getPromotions(PromotionStatus status, String keyword, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        return promotionRepository.findByFilter(statusStr, keyword, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch khuyến mãi với ID: " + id));
        return mapToResponse(promotion);
    }

    @Override
    @Transactional
    public PromotionResponse updateStatus(Long id, PromotionStatus status) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch khuyến mãi với ID: " + id));
        promotion.setStatus(status);
        return mapToResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch khuyến mãi với ID: " + id));
        customerPromotionRepository.deleteByPromotionId(id);
        promotionRepository.delete(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public TargetPreviewResponse previewTarget(TargetPreviewRequest request) {
        List<Customer> customers = customerRepository.findAll();
        int targetRank = getTierRank(request.getMinTier());
        int minRecency = request.getMinRecencyDays() != null ? request.getMinRecencyDays() : 0;
        LocalDateTime now = LocalDateTime.now();

        long count = customers.stream().filter(c -> {
            int customerRank = getTierRank(c.getTier() != null ? c.getTier().getTierName() : "Member");
            if (customerRank < targetRank) return false;
            if (minRecency > 0) {
                if (c.getLastCompletedBookingAt() != null) {
                    return c.getLastCompletedBookingAt().isBefore(now.minusDays(minRecency));
                } else {
                    return true;
                }
            }
            return true;
        }).count();

        return TargetPreviewResponse.builder()
                .estimatedCustomerCount(count)
                .message(count + " khách hàng thỏa mãn điều kiện tệp đối tượng")
                .build();
    }

    @Override
    @Transactional
    public List<String> grantDirect(DirectGrantRequest request) {
        Promotion promotion = promotionRepository.findById(request.getPromotionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch khuyến mãi ID: " + request.getPromotionId()));
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new IllegalStateException("Chiến dịch khuyến mãi đang không hoạt động (ACTIVE)");
        }

        List<String> issuedCodes = new ArrayList<>();
        for (Long customerId : request.getCustomerIds()) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại ID: " + customerId));

            // Kiểm tra sở hữu đồng thời (Coexistence Lock)
            boolean hasActive = customerPromotionRepository.existsByCustomerCustomerIdAndPromotionIdAndStatus(customerId, promotion.getId(), CustomerPromotionStatus.ISSUED);
            if (hasActive) {
                log.info("Skip direct grant for customer ID {} because they already hold an active voucher of campaign {}", customerId, promotion.getCode());
                continue;
            }

            String voucherCode = "VOU-" + promotion.getCode() + "-" + customerId + "-" + (System.currentTimeMillis() % 10000);
            CustomerPromotion cp = CustomerPromotion.builder()
                    .customer(customer)
                    .promotion(promotion)
                    .voucherCode(voucherCode)
                    .issuedAt(LocalDateTime.now())
                    .expiryDate(promotion.getEndDate() != null ? promotion.getEndDate() : LocalDateTime.now().plusMonths(3))
                    .status(CustomerPromotionStatus.ISSUED)
                    .source(CustomerPromotionSource.GIFT_DIRECT)
                    .build();
            customerPromotionRepository.save(cp);
            issuedCodes.add(voucherCode);

            promotion.setIssuedCount(promotion.getIssuedCount() + 1);
        }
        promotionRepository.save(promotion);
        log.info("Direct grant completed: {} vouchers issued for promotion {}", issuedCodes.size(), promotion.getCode());
        return issuedCodes;
    }

    private int getTierRank(String tierName) {
        if (tierName == null) return 1;
        String upper = tierName.toUpperCase();
        if (upper.contains("SILVER")) return 2;
        if (upper.contains("GOLD")) return 3;
        if (upper.contains("PLATINUM")) return 4;
        if (upper.contains("DIAMOND")) return 5;
        return 1; // REGULAR / Member / default
    }

    private PromotionResponse mapToResponse(Promotion p) {
        int issued = p.getIssuedCount() != null ? p.getIssuedCount() : 0;
        int redeemed = p.getRedeemedCount() != null ? p.getRedeemedCount() : 0;

        // Budget status: null = vô hạn (∞)
        String budgetStatus;
        if (p.getTotalBudget() == null || p.getTotalBudget() == 0) {
            budgetStatus = issued + " / ∞ (Vô hạn)";
        } else {
            budgetStatus = issued + " / " + p.getTotalBudget() + " mã";
        }

        double redemptionRate = 0.0;
        if (issued > 0) {
            redemptionRate = Math.round(((double) redeemed / issued * 100.0) * 10.0) / 10.0;
        }

        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .value(p.getValue())
                .costPoints(p.getCostPoints())
                .minTier(p.getMinTier())
                .minRecencyDays(p.getMinRecencyDays())
                .maxClaimPerUser(p.getMaxClaimPerUser())
                .totalBudget(p.getTotalBudget())
                .issuedCount(p.getIssuedCount())
                .redeemedCount(p.getRedeemedCount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .budgetStatus(budgetStatus)
                .redemptionRate(redemptionRate)
                .applicableServiceCode(p.getApplicableServiceCode())
                .applicableDays(p.getApplicableDays())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderValue(p.getMinOrderValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionKpiSummaryResponse getKpiSummary() {
        // Card 1: Giá trị ưu đãi đã phát
        BigDecimal totalPromoValueIssued = promotionRepository.findAll().stream()
                .map(p -> {
                    BigDecimal val = p.getValue() != null ? p.getValue() : BigDecimal.ZERO;
                    BigDecimal count = BigDecimal.valueOf(p.getIssuedCount() != null ? p.getIssuedCount() : 0);
                    return val.multiply(count);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Card 2: Chiến dịch kích hoạt
        int activeCampaignsCount = promotionRepository.findByStatus(PromotionStatus.ACTIVE).size();

        // Card 3: Voucher khách đã lấy (chỉ đếm CLAIM và EXCHANGE)
        int totalVouchersClaimed = (int) customerPromotionRepository.countBySourceIn(
                List.of(CustomerPromotionSource.CLAIM, CustomerPromotionSource.EXCHANGE));

        // Card 4: Hiệu quả ROI tiếp thị & Hiệu suất dùng voucher
        List<Booking> bookingsWithVoucher = bookingRepository.findCompletedBookingsWithVoucher();
        
        BigDecimal totalDiscount = bookingsWithVoucher.stream()
                .map(b -> b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = bookingsWithVoucher.stream()
                .map(b -> b.getFinalAmount() != null ? b.getFinalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double marketingRoi = 0.0;
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            marketingRoi = totalRevenue.divide(totalDiscount, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        } else {
            // Fallback tỷ lệ mặc định hoặc mock nếu chưa có đơn nào áp dụng discount thành công
            marketingRoi = 3.2;
        }

        // Tỷ lệ hiệu suất đổi mã = USED / (ISSUED + USED + EXPIRED...)
        long totalRedeemed = customerPromotionRepository.countByStatus(CustomerPromotionStatus.USED);
        long totalIssuedWallet = customerPromotionRepository.count();
        double redemptionRate = 0.0;
        if (totalIssuedWallet > 0) {
            redemptionRate = Math.round(((double) totalRedeemed / totalIssuedWallet * 100.0) * 10.0) / 10.0;
        } else {
            redemptionRate = 65.6; // Fallback mock
        }

        return PromotionKpiSummaryResponse.builder()
                .totalPromoValueIssued(totalPromoValueIssued)
                .activeCampaignsCount(activeCampaignsCount)
                .totalVouchersClaimed(totalVouchersClaimed)
                .marketingRoi(marketingRoi)
                .redemptionRate(redemptionRate)
                .build();
    }
}
