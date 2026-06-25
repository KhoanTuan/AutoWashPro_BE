package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.financial.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByPromotionCode(String promotionCode);

    List<Promotion> findByStatusAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            PromotionStatus status, LocalDateTime now1, LocalDateTime now2);
}
