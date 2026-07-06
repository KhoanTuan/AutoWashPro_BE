package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, Integer> {
    Optional<LoyaltyTier> findByTierName(String tierName);

    List<LoyaltyTier> findAllByOrderByMinSpendAsc();
}
