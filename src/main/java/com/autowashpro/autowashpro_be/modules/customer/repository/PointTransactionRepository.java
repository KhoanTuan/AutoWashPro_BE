package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.PointTransaction;
import com.autowashpro.autowashpro_be.modules.customer.entity.PointActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findAllByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.customer.customerId = :customerId AND pt.activityType = :type")
    int sumPointsByCustomerIdAndType(@Param("customerId") Long customerId, @Param("type") PointActivityType type);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.customer.customerId = :customerId AND pt.activityType = :type AND pt.createdAt < :date")
    int sumPointsByCustomerIdAndTypeAndCreatedAtBefore(@Param("customerId") Long customerId, @Param("type") PointActivityType type, @Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt WHERE pt.customer.customerId = :customerId AND pt.activityType IN :types")
    int sumPointsByCustomerIdAndTypes(@Param("customerId") Long customerId, @Param("types") Collection<PointActivityType> types);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE point_transaction SET created_at = :createdAt WHERE point_transaction_id = :id", nativeQuery = true)
    void updateCreatedAt(@Param("id") Long id, @Param("createdAt") LocalDateTime createdAt);
}
