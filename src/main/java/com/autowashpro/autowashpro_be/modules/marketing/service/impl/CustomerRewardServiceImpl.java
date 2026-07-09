package com.autowashpro.autowashpro_be.modules.marketing.service.impl;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerRewardShopResponse;
import com.autowashpro.autowashpro_be.modules.marketing.dto.response.CustomerVoucherResponse;
import com.autowashpro.autowashpro_be.modules.marketing.entity.*;
import com.autowashpro.autowashpro_be.modules.marketing.repository.CustomerPromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.repository.PromotionRepository;
import com.autowashpro.autowashpro_be.modules.marketing.service.CustomerRewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRewardServiceImpl implements CustomerRewardService {

    private final PromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    private final CustomerPromotionRepository customerPromotionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRewardShopResponse> getRewardShop(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại ID: " + customerId));

        List<Promotion> activePromotions = promotionRepository.findActivePromotions(LocalDateTime.now());
        int customerRank = getTierRank(customer.getTier() != null ? customer.getTier().getTierName() : "Member");
        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

        return activePromotions.stream().map(p -> {
            int reqRank = getTierRank(p.getMinTier());
            int cost = p.getCostPoints() != null ? p.getCostPoints() : 0;
            long claimed = customerPromotionRepository.countByCustomerCustomerIdAndPromotionId(customerId, p.getId());
            int maxClaim = p.getMaxClaimPerUser() != null ? p.getMaxClaimPerUser() : 1;

            boolean isUnlocked = true;
            String tooltip = null;

            // maxClaimPerUser == null nghĩa là VÔ HẠN
            if (p.getMaxClaimPerUser() != null && claimed >= p.getMaxClaimPerUser()) {
                isUnlocked = false;
                tooltip = "🔒 Bạn đã lấy tối đa " + p.getMaxClaimPerUser() + " lần cho mã quà tặng này.";
            } else if (customerRank < reqRank) {
                isUnlocked = false;
                tooltip = "🔒 Độc quyền cho thành viên hạng " + p.getMinTier() + " trở lên.";
            } else if (currentPoints < cost) {
                isUnlocked = false;
                tooltip = "🔒 Cần thêm " + (cost - currentPoints) + " điểm Loyalty để đổi mã này.";
            } else if (p.getTotalBudget() != null && p.getIssuedCount() != null && p.getIssuedCount() >= p.getTotalBudget()) {
                isUnlocked = false;
                tooltip = "🔒 Mã ưu đãi này đã được đổi hết ngân sách phát hành.";
            }

            return CustomerRewardShopResponse.builder()
                    .id(p.getId())
                    .code(p.getCode())
                    .title(p.getName())
                    .description(p.getDescription())
                    .discountType(p.getDiscountType())
                    .value(p.getValue())
                    .pointsCost(cost)
                    .minTier(p.getMinTier())
                    .endDate(p.getEndDate())
                    .isUnlocked(isUnlocked)
                    .isGrayscale(!isUnlocked)
                    .unlockTooltip(tooltip)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerVoucherResponse claimFreeVoucher(Long customerId, Long promotionId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại ID: " + customerId));
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Khách ưu đãi không tồn tại ID: " + promotionId));

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new IllegalStateException("Chiến dịch ưu đãi không hoạt động hoặc đã kết thúc.");
        }
        if (promotion.getCostPoints() != null && promotion.getCostPoints() > 0) {
            throw new IllegalArgumentException("Voucher này yêu cầu đổi bằng " + promotion.getCostPoints() + " điểm, không thể lấy miễn phí.");
        }

        // maxClaimPerUser == null → vô hạn, bỏ qua kiểm tra
        if (promotion.getMaxClaimPerUser() != null) {
            long claimed = customerPromotionRepository.countByCustomerCustomerIdAndPromotionId(customerId, promotionId);
            if (claimed >= promotion.getMaxClaimPerUser()) {
                throw new IllegalStateException("Bạn đã nhận đủ số lượng tối đa (" + promotion.getMaxClaimPerUser() + " lần) cho ưu đãi này.");
            }
        }

        // totalBudget == null → vô hạn, bỏ qua kiểm tra
        if (promotion.getTotalBudget() != null && promotion.getIssuedCount() != null && promotion.getIssuedCount() >= promotion.getTotalBudget()) {
            throw new IllegalStateException("Voucher đã được phát hết số lượng giới hạn.");
        }

        int reqRank = getTierRank(promotion.getMinTier());
        int customerRank = getTierRank(customer.getTier() != null ? customer.getTier().getTierName() : "Member");
        if (customerRank < reqRank) {
            throw new IllegalStateException("Ưu đãi dành riêng cho thành viên hạng " + promotion.getMinTier() + " trở lên.");
        }

        String voucherCode = "VOU-" + promotion.getCode() + "-" + customerId + "-" + (System.currentTimeMillis() % 10000);
        LocalDateTime claimExpiry = LocalDateTime.now().plusDays(30);
        if (promotion.getEndDate() != null && claimExpiry.isAfter(promotion.getEndDate())) {
            claimExpiry = promotion.getEndDate();
        }

        CustomerPromotion cp = CustomerPromotion.builder()
                .customer(customer)
                .promotion(promotion)
                .voucherCode(voucherCode)
                .issuedAt(LocalDateTime.now())
                .expiryDate(claimExpiry)
                .status(CustomerPromotionStatus.ISSUED)
                .source(CustomerPromotionSource.CLAIM)
                .build();
        CustomerPromotion saved = customerPromotionRepository.save(cp);

        promotion.setIssuedCount((promotion.getIssuedCount() != null ? promotion.getIssuedCount() : 0) + 1);
        promotionRepository.save(promotion);

        log.info("Customer {} successfully claimed free voucher {}", customer.getFullName(), voucherCode);
        return mapToVoucherResponse(saved);
    }

    @Override
    @Transactional
    public CustomerVoucherResponse exchangePoints(Long customerId, Long promotionId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại ID: " + customerId));
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Khách ưu đãi không tồn tại ID: " + promotionId));

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new IllegalStateException("Chiến dịch ưu đãi không hoạt động hoặc đã kết thúc.");
        }

        int cost = promotion.getCostPoints() != null ? promotion.getCostPoints() : 0;
        if (cost <= 0) {
            throw new IllegalArgumentException("Voucher này miễn phí, hãy sử dụng chức năng Thu thập (Claim) thay vì Đổi điểm.");
        }

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (currentPoints < cost) {
            throw new IllegalStateException("Bạn không đủ điểm Loyalty. Cần " + cost + " điểm, hiện có " + currentPoints + " điểm.");
        }

        // maxClaimPerUser == null → vô hạn, bỏ qua kiểm tra
        if (promotion.getMaxClaimPerUser() != null) {
            long claimed = customerPromotionRepository.countByCustomerCustomerIdAndPromotionId(customerId, promotionId);
            if (claimed >= promotion.getMaxClaimPerUser()) {
                throw new IllegalStateException("Bạn đã nhận đủ số lượng tối đa (" + promotion.getMaxClaimPerUser() + " lần) cho ưu đãi này.");
            }
        }

        // totalBudget == null → vô hạn, bỏ qua kiểm tra
        if (promotion.getTotalBudget() != null && promotion.getIssuedCount() != null && promotion.getIssuedCount() >= promotion.getTotalBudget()) {
            throw new IllegalStateException("Voucher đã được phát hết số lượng giới hạn.");
        }

        int reqRank = getTierRank(promotion.getMinTier());
        int customerRank = getTierRank(customer.getTier() != null ? customer.getTier().getTierName() : "Member");
        if (customerRank < reqRank) {
            throw new IllegalStateException("Ưu đãi dành riêng cho thành viên hạng " + promotion.getMinTier() + " trở lên.");
        }

        // Subtract points from customer
        customer.setLoyaltyPoints(currentPoints - cost);
        customerRepository.save(customer);

        String voucherCode = "VOU-" + promotion.getCode() + "-" + customerId + "-" + (System.currentTimeMillis() % 10000);
        LocalDateTime exchangeExpiry = LocalDateTime.now().plusDays(14);
        if (promotion.getEndDate() != null && exchangeExpiry.isAfter(promotion.getEndDate())) {
            exchangeExpiry = promotion.getEndDate();
        }

        CustomerPromotion cp = CustomerPromotion.builder()
                .customer(customer)
                .promotion(promotion)
                .voucherCode(voucherCode)
                .issuedAt(LocalDateTime.now())
                .expiryDate(exchangeExpiry)
                .status(CustomerPromotionStatus.ISSUED)
                .source(CustomerPromotionSource.EXCHANGE)
                .build();
        CustomerPromotion saved = customerPromotionRepository.save(cp);

        promotion.setIssuedCount((promotion.getIssuedCount() != null ? promotion.getIssuedCount() : 0) + 1);
        promotionRepository.save(promotion);

        log.info("Customer {} exchanged {} points for voucher {}", customer.getFullName(), cost, voucherCode);
        return mapToVoucherResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerVoucherResponse> getMyVouchers(Long customerId, String statusParam) {
        List<CustomerPromotion> list;
        if (statusParam != null && !statusParam.isBlank() && !"ALL".equalsIgnoreCase(statusParam)) {
            try {
                CustomerPromotionStatus st = CustomerPromotionStatus.valueOf(statusParam.toUpperCase());
                list = customerPromotionRepository.findByCustomerCustomerIdAndStatus(customerId, st);
            } catch (Exception e) {
                list = customerPromotionRepository.findByCustomerCustomerId(customerId);
            }
        } else {
            list = customerPromotionRepository.findByCustomerCustomerId(customerId);
        }
        return list.stream().map(this::mapToVoucherResponse).collect(Collectors.toList());
    }

    private int getTierRank(String tierName) {
        if (tierName == null) return 1;
        String upper = tierName.toUpperCase();
        if (upper.contains("SILVER")) return 2;
        if (upper.contains("GOLD")) return 3;
        if (upper.contains("PLATINUM")) return 4;
        if (upper.contains("DIAMOND")) return 5;
        return 1;
    }

    private CustomerVoucherResponse mapToVoucherResponse(CustomerPromotion cp) {
        Promotion p = cp.getPromotion();
        boolean isExpired = false;
        if (cp.getExpiryDate() != null && cp.getExpiryDate().isBefore(LocalDateTime.now())) {
            isExpired = true;
        }
        return CustomerVoucherResponse.builder()
                .id(cp.getId())
                .voucherCode(cp.getVoucherCode())
                .title(p != null ? p.getName() : "Voucher Quà Tặng")
                .description(p != null ? p.getDescription() : "")
                .discountType(p != null ? p.getDiscountType() : DiscountType.FIXED_AMOUNT)
                .value(p != null ? p.getValue() : null)
                .issuedAt(cp.getIssuedAt())
                .expiryDate(cp.getExpiryDate())
                .status(cp.getStatus())
                .source(cp.getSource())
                .isExpired(isExpired)
                .build();
    }
}
