package com.autowashpro.autowashpro_be.modules.customer.scheduler;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig;
import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.repository.CustomerRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyConfigRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.LoyaltyTierRepository;
import com.autowashpro.autowashpro_be.modules.customer.repository.PointTransactionRepository;
import com.autowashpro.autowashpro_be.modules.notification.entity.NotificationType;
import com.autowashpro.autowashpro_be.modules.notification.service.RealtimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyScheduler {

    private final CustomerRepository customerRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final LoyaltyConfigRepository loyaltyConfigRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final RealtimeNotificationService notificationService;

    /**
     * Run every day at 1:00 AM to scan and expire loyalty points that have exceeded pointValidityMonths.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireExpiredPoints() {
        log.info("Starting Loyalty points expiration scan...");
        LoyaltyConfig config = loyaltyConfigRepository.getGlobalConfig();
        int validityMonths = config.getPointValidityMonths();
        LocalDateTime expiryBoundary = LocalDateTime.now().minusMonths(validityMonths);

        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            try {
                int earnedBeforeBoundary = pointTransactionRepository.sumPointsByCustomerIdAndTypeAndCreatedAtBefore(
                        customer.getCustomerId(), PointActivityType.EARNED, expiryBoundary);

                int totalSpent = Math.abs(pointTransactionRepository.sumPointsByCustomerIdAndTypes(
                        customer.getCustomerId(), Arrays.asList(PointActivityType.REDEEMED, PointActivityType.PENALTY)));

                int alreadyExpired = Math.abs(pointTransactionRepository.sumPointsByCustomerIdAndType(
                        customer.getCustomerId(), PointActivityType.EXPIRY));

                int toExpire = Math.max(0, earnedBeforeBoundary - totalSpent) - alreadyExpired;

                if (toExpire > 0) {
                    customer.setLoyaltyPoints(Math.max(0, customer.getLoyaltyPoints() - toExpire));
                    customerRepository.save(customer);

                    PointTransaction pt = PointTransaction.builder()
                            .customer(customer)
                            .points(-toExpire)
                            .activityType(PointActivityType.EXPIRY)
                            .build();
                    pointTransactionRepository.save(pt);

                    notificationService.notifyGeneral(
                            customer.getCustomerId(),
                            "Thu hồi điểm quá hạn",
                            String.format("Tài khoản của bạn đã bị thu hồi -%d điểm Loyalty do quá hạn %d tháng chưa sử dụng.", toExpire, validityMonths),
                            NotificationType.SYSTEM_ALERT
                    );

                    log.info("Expired {} points for customer: {}", toExpire, customer.getFullName());
                }
            } catch (Exception e) {
                log.error("Error expiring points for customer: " + customer.getCustomerId(), e);
            }
        }
        log.info("Loyalty points expiration scan completed.");
    }

    /**
     * Run every day at 2:00 AM to downgrade customers who haven't completed any booking in inactivityDowngradeMonths.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void downgradeInactiveTiers() {
        log.info("Starting VIP tier inactivity downgrade scan...");
        LoyaltyConfig config = loyaltyConfigRepository.getGlobalConfig();
        int downgradeMonths = config.getInactivityDowngradeMonths();
        LocalDateTime inactivityBoundary = LocalDateTime.now().minusMonths(downgradeMonths);

        List<LoyaltyTier> allTiers = loyaltyTierRepository.findAllByOrderByMinSpendAsc();
        if (allTiers.isEmpty()) return;
        LoyaltyTier regularTier = allTiers.get(0);

        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            try {
                // Chỉ xét hạ hạng đối với các khách có hạng cao hơn Regular
                if (customer.getTier() != null && !customer.getTier().getTierId().equals(regularTier.getTierId())) {
                    LocalDateTime lastActive = customer.getLastCompletedBookingAt();
                    if (lastActive == null) {
                        lastActive = customer.getCreatedAt();
                    }

                    if (lastActive != null && lastActive.isBefore(inactivityBoundary)) {
                        // Xác định hạng tiếp theo phía dưới
                        LoyaltyTier currentTier = customer.getTier();
                        LoyaltyTier newTier = regularTier;
                        for (int i = 1; i < allTiers.size(); i++) {
                            if (allTiers.get(i).getTierId().equals(currentTier.getTierId())) {
                                newTier = allTiers.get(i - 1);
                                break;
                            }
                        }

                        customer.setTier(newTier);
                        // Reset tier spending về mức tối thiểu của hạng mới
                        customer.setTierSpending(newTier.getMinSpend() != null ? newTier.getMinSpend() : BigDecimal.ZERO);
                        customerRepository.save(customer);

                        notificationService.notifyGeneral(
                                customer.getCustomerId(),
                                "Hạ hạng thành viên",
                                String.format("Hạng VIP của bạn đã bị lùi xuống hạng %s do không có hoạt động trong %d tháng qua.", getDisplayName(newTier.getTierName()), downgradeMonths),
                                NotificationType.SYSTEM_ALERT
                        );

                        log.info("Downgraded customer {} from {} to {}", customer.getFullName(), currentTier.getTierName(), newTier.getTierName());
                    }
                }
            } catch (Exception e) {
                log.error("Error downgrading VIP tier for customer: " + customer.getCustomerId(), e);
            }
        }
        log.info("VIP tier inactivity downgrade scan completed.");
    }

    /**
     * Run every day at 3:00 AM to lock customer accounts that have been inactive for inactivityLockoutMonths.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void lockInactiveAccounts() {
        log.info("Starting inactive accounts lockout scan...");
        LoyaltyConfig config = loyaltyConfigRepository.getGlobalConfig();
        int lockoutMonths = config.getInactivityLockoutMonths();
        LocalDateTime lockoutBoundary = LocalDateTime.now().minusMonths(lockoutMonths);

        List<Customer> customers = customerRepository.findAll();
        for (Customer customer : customers) {
            try {
                if (customer.getStatus() == CustomerStatus.ACTIVE) {
                    LocalDateTime lastActive = customer.getLastCompletedBookingAt();
                    if (lastActive == null) {
                        lastActive = customer.getCreatedAt();
                    }

                    if (lastActive != null && lastActive.isBefore(lockoutBoundary)) {
                        customer.setStatus(CustomerStatus.INACTIVE);
                        customerRepository.save(customer);

                        notificationService.notifyGeneral(
                                customer.getCustomerId(),
                                "Tài khoản bị tạm khóa",
                                String.format("Tài khoản của bạn đã bị khóa tạm thời do không có hoạt động dọn rửa trong %d tháng qua.", lockoutMonths),
                                NotificationType.SYSTEM_ALERT
                        );

                        log.info("Locked inactive customer account: {}", customer.getFullName());
                    }
                }
            } catch (Exception e) {
                log.error("Error locking account for customer: " + customer.getCustomerId(), e);
            }
        }
        log.info("Inactive accounts lockout scan completed.");
    }

    private String getDisplayName(String tierName) {
        if (tierName == null) return "Member";
        return switch (tierName.toUpperCase()) {
            case "REGULAR", "MEMBER" -> "Member";
            case "SILVER" -> "Silver";
            case "GOLD" -> "Gold";
            case "PLATINUM" -> "Platinum";
            default -> tierName;
        };
    }
}
