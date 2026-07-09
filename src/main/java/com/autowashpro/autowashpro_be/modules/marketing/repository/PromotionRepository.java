package com.autowashpro.autowashpro_be.modules.marketing.repository;

import com.autowashpro.autowashpro_be.modules.marketing.entity.Promotion;
import com.autowashpro.autowashpro_be.modules.marketing.entity.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    List<Promotion> findByStatus(PromotionStatus status);

    @Query(value = "SELECT * FROM promotions p WHERE (:status IS NULL OR p.status = CAST(:status AS VARCHAR)) " +
           "AND (:keyword IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(*) FROM promotions p WHERE (:status IS NULL OR p.status = CAST(:status AS VARCHAR)) " +
           "AND (:keyword IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           nativeQuery = true)
    Page<Promotion> findByFilter(@Param("status") String status,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' " +
           "AND (p.startDate IS NULL OR p.startDate <= :now) " +
           "AND (p.endDate IS NULL OR p.endDate >= :now)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
}
