package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyConfigRepository extends JpaRepository<LoyaltyConfig, Long> {
    
    Object lock = new Object();

    default LoyaltyConfig getGlobalConfig() {
        synchronized (lock) {
            try {
                return findById(1L).orElseGet(() -> {
                    LoyaltyConfig defaultConfig = LoyaltyConfig.builder()
                            .basePointRate(java.math.BigDecimal.valueOf(10000))
                            .basePoints(1)
                            .roundDown(true)
                            .pointValidityMonths(12)
                            .inactivityDowngradeMonths(6)
                            .inactivityLockoutMonths(12)
                            .build();
                    defaultConfig.setLoyaltyConfigId(1L);
                    return saveAndFlush(defaultConfig);
                });
            } catch (Exception e) {
                return findById(1L).orElseThrow(() -> new RuntimeException("Failed to find or save loyalty config", e));
            }
        }
    }
}
