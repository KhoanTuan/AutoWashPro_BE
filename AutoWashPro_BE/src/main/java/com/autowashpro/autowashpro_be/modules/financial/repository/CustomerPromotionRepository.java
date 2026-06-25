package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.financial.entity.CustomerPromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPromotionRepository extends JpaRepository<CustomerPromotion, Long> {

    List<CustomerPromotion> findByCustomerCustomerIdAndStatus(Long customerId, CustomerPromotionStatus status);

    Optional<CustomerPromotion> findTopByCustomerCustomerIdAndStatusOrderByCreatedAtDesc(
            Long customerId, CustomerPromotionStatus status);
}
