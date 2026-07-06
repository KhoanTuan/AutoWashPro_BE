package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyTierResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyTierRepository loyaltyTierRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<LoyaltyTierResponse> getAllTiers() {
        return loyaltyTierRepository.findAllByOrderByMinSpendAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoyaltyTierResponse getMyLoyaltyStatus(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));

        List<LoyaltyTier> allTiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
        LoyaltyTier currentTier = customer.getTier();
        if (currentTier == null && !allTiers.isEmpty()) {
            currentTier = allTiers.get(0);
        }

        BigDecimal currentSpend = customer.getTotalSpending() != null ? customer.getTotalSpending() : BigDecimal.ZERO;

        int currentIndex = -1;
        for (int i = 0; i < allTiers.size(); i++) {
            if (currentTier != null && allTiers.get(i).getTierId().equals(currentTier.getTierId())) {
                currentIndex = i;
                break;
            }
        }

        LoyaltyTier nextTier = (currentIndex >= 0 && currentIndex + 1 < allTiers.size())
                ? allTiers.get(currentIndex + 1)
                : null;

        LoyaltyTierResponse response = mapToResponse(currentTier != null ? currentTier : (allTiers.isEmpty() ? new LoyaltyTier() : allTiers.get(0)));
        response.setCurrentSpend(currentSpend);

        if (nextTier != null) {
            response.setNextTierDisplayName(getDisplayName(nextTier.getTierName()));
            response.setNextTierMinSpend(nextTier.getMinSpend());

            BigDecimal needed = nextTier.getMinSpend().subtract(currentSpend);
            response.setSpendNeededForNextTier(needed.compareTo(BigDecimal.ZERO) > 0 ? needed : BigDecimal.ZERO);

            BigDecimal tierBaseSpend = currentTier != null && currentTier.getMinSpend() != null ? currentTier.getMinSpend() : BigDecimal.ZERO;
            BigDecimal range = nextTier.getMinSpend().subtract(tierBaseSpend);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal progress = currentSpend.subtract(tierBaseSpend)
                        .divide(range, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                int pct = progress.intValue();
                response.setProgressPercentage(Math.max(0, Math.min(100, pct)));
            } else {
                response.setProgressPercentage(100);
            }
        } else {
            // Đã đạt hạng cao nhất
            response.setNextTierDisplayName("Tối đa");
            response.setNextTierMinSpend(BigDecimal.ZERO);
            response.setSpendNeededForNextTier(BigDecimal.ZERO);
            response.setProgressPercentage(100);
        }

        return response;
    }

    @Transactional
    public LoyaltyTierResponse updateTierConfig(Integer tierId, LoyaltyTierRequest request) {
        LoyaltyTier tier = loyaltyTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng VIP với ID: " + tierId));

        tier.setMinSpend(request.getMinSpend());
        tier.setTierMultiplier(request.getTierMultiplier());
        tier.setBookingWindowDays(request.getBookingWindowDays());

        LoyaltyTier updated = loyaltyTierRepository.save(tier);
        log.info("Updated loyalty tier config: {} (Window: {} days)", updated.getTierName(), updated.getBookingWindowDays());
        return mapToResponse(updated);
    }

    private LoyaltyTierResponse mapToResponse(LoyaltyTier tier) {
        if (tier == null || tier.getTierName() == null) {
            return LoyaltyTierResponse.builder().build();
        }
        String dispName = getDisplayName(tier.getTierName());
        String summary = String.format("Đặt trước %d ngày • Tích điểm x%s",
                tier.getBookingWindowDays() != null ? tier.getBookingWindowDays() : 7,
                tier.getTierMultiplier() != null ? tier.getTierMultiplier().toString() : "1.00");

        return LoyaltyTierResponse.builder()
                .tierId(tier.getTierId())
                .tierName(tier.getTierName())
                .displayName(dispName)
                .minSpend(tier.getMinSpend())
                .tierMultiplier(tier.getTierMultiplier())
                .bookingWindowDays(tier.getBookingWindowDays())
                .benefitsSummary(summary)
                .build();
    }

    private String getDisplayName(String tierName) {
        if (tierName == null) return "Member";
        return switch (tierName.toUpperCase()) {
            case "REGULAR", "MEMBER" -> "Member";
            case "SILVER" -> "Silver";
            case "GOLD" -> "Gold";
            case "PLATINUM" -> "Platinum";
            default -> tierName.substring(0, 1).toUpperCase() + tierName.substring(1).toLowerCase();
        };
    }
}
