package com.autowashpro.autowashpro_be.modules.customer.service;

import com.autowashpro.autowashpro_be.modules.customer.dto.CustomerLoyaltyProfileResponse;
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

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.modules.customer.dto.PointTransactionResponse;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltyConfigRequest;
import com.autowashpro.autowashpro_be.modules.customer.dto.LoyaltySettingsResponse;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyConfigRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyTierRepository loyaltyTierRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyConfigRepository loyaltyConfigRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional(readOnly = true)
    public List<PointTransactionResponse> getCustomerPointHistory(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId);
        }
        return pointTransactionRepository.findAllByCustomerCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(pt -> PointTransactionResponse.builder()
                        .pointTransactionId(pt.getPointTransactionId())
                        .points(pt.getPoints())
                        .activityType(pt.getActivityType() != null ? pt.getActivityType().name() : null)
                        .bookingCode(pt.getBookingCode())
                        .createdAt(pt.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public LoyaltySettingsResponse getLoyaltySettings() {
        return LoyaltySettingsResponse.builder()
                .config(loyaltyConfigRepository.getGlobalConfig())
                .tiers(getAllTiers())
                .build();
    }

    @Transactional
    public LoyaltyConfig updateLoyaltyConfig(LoyaltyConfigRequest request) {
        if (request.getInactivityLockoutMonths() <= request.getInactivityDowngradeMonths()) {
            throw new BadRequestException("Thời gian khóa tài khoản (" + request.getInactivityLockoutMonths() + " tháng) phải lớn hơn thời gian hạ hạng (" + request.getInactivityDowngradeMonths() + " tháng)!");
        }

        LoyaltyConfig config = loyaltyConfigRepository.getGlobalConfig();
        config.setBasePointRate(request.getBasePointRate());
        config.setBasePoints(request.getBasePoints());
        config.setRoundDown(request.getRoundDown());
        config.setPointValidityMonths(request.getPointValidityMonths());
        config.setInactivityDowngradeMonths(request.getInactivityDowngradeMonths());
        config.setInactivityLockoutMonths(request.getInactivityLockoutMonths());

        log.info("Updated global loyalty config: point rate: {}, round down: {}", config.getBasePointRate(), config.getRoundDown());
        return loyaltyConfigRepository.save(config);
    }

    @Transactional
    public void simulateSetInactivity(Long customerId, int months) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));
        
        java.time.LocalDateTime targetDate = java.time.LocalDateTime.now().minusMonths(months);
        customer.setLastCompletedBookingAt(targetDate);
        customerRepository.save(customer);
        customerRepository.updateCreatedAt(customerId, targetDate);
        log.info("Simulated inactivity for customer: {} (set last completed booking & created_at to {} months ago)", customer.getFullName(), months);
    }

    @Transactional
    public void simulateSetPointsExpired(Long customerId, int months) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));
        
        // Cộng 100 điểm để có điểm giả lập hết hạn
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + 100);
        customerRepository.save(customer);

        PointTransaction pt = PointTransaction.builder()
                .customer(customer)
                .points(100)
                .activityType(PointActivityType.EARNED)
                .bookingCode("SIM-MOCK-EARNED")
                .build();
        pointTransactionRepository.saveAndFlush(pt);
        
        java.time.LocalDateTime targetDate = java.time.LocalDateTime.now().minusMonths(months);
        pointTransactionRepository.updateCreatedAt(pt.getPointTransactionId(), targetDate);
        log.info("Simulated point expiration for customer: {} (added 100 points transaction set to {} months ago)", customer.getFullName(), months);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTierResponse> getAllTiers() {
        return loyaltyTierRepository.findAllByOrderByMinSpendAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public LoyaltyTierResponse getMyLoyaltyStatus(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));

        List<LoyaltyTier> allTiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
        BigDecimal tierSpend = customer.getTierSpending() != null ? customer.getTierSpending() : BigDecimal.ZERO;
        
        for (int i = allTiers.size() - 1; i >= 0; i--) {
            LoyaltyTier tier = allTiers.get(i);
            if (tierSpend.compareTo(tier.getMinSpend()) >= 0) {
                if (customer.getTier() == null || customer.getTier().getMinSpend().compareTo(tier.getMinSpend()) < 0) {
                    customer.setTier(tier);
                    customerRepository.save(customer);
                    log.info("Customer {} dynamically upgraded to VIP tier: {} based on spending", customer.getCustomerId(), tier.getTierName());
                }
                break;
            }
        }

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

    @Transactional
    public CustomerLoyaltyProfileResponse getCustomerLoyaltyProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + customerId));

        LoyaltyTierResponse status = getMyLoyaltyStatus(customerId);

        return CustomerLoyaltyProfileResponse.builder()
                .customerId(customer.getCustomerId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalSpending(customer.getTotalSpending())
                .tierName(customer.getTier() != null ? customer.getTier().getTierName() : "MEMBER")
                .nextTierName(status.getNextTierDisplayName() != null ? status.getNextTierDisplayName() : "SILVER")
                .nextTierMinSpend(status.getNextTierMinSpend())
                .spendNeededForNextTier(status.getSpendNeededForNextTier())
                .progressPercentage(status.getProgressPercentage() != null ? status.getProgressPercentage().doubleValue() : 0.0)
                .build();
    }
}
