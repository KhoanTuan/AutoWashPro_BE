package com.autowashpro.autowashpro_be.modules.marketing.repository;

import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionStatus;
import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerPromotionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPromotionRepository extends JpaRepository<CustomerPromotion, Long> {

    List<CustomerPromotion> findByCustomerCustomerId(Long customerId);

    List<CustomerPromotion> findByCustomerCustomerIdAndStatus(Long customerId, CustomerPromotionStatus status);

    Optional<CustomerPromotion> findByVoucherCode(String voucherCode);

    Optional<CustomerPromotion> findByCustomerCustomerIdAndVoucherCode(Long customerId, String voucherCode);

    long countByCustomerCustomerIdAndPromotionId(Long customerId, Long promotionId);

    boolean existsByCustomerCustomerIdAndPromotionIdAndStatus(Long customerId, Long promotionId, CustomerPromotionStatus status);

    long countBySourceIn(List<CustomerPromotionSource> sources);

    long countByStatus(CustomerPromotionStatus status);
}
