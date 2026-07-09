package com.autowashpro.autowashpro_be.modules.marketing.repository;

import com.autowashpro.autowashpro_be.modules.marketing.entity.CustomerFeedback;
import com.autowashpro.autowashpro_be.modules.marketing.entity.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long> {

    List<CustomerFeedback> findByCustomerCustomerId(Long customerId);

    @Query(value = "SELECT * FROM customer_feedbacks f WHERE (:status IS NULL OR f.status = CAST(:status AS VARCHAR)) " +
           "AND (:ratingLte IS NULL OR f.rating_stars <= :ratingLte)",
           countQuery = "SELECT COUNT(*) FROM customer_feedbacks f WHERE (:status IS NULL OR f.status = CAST(:status AS VARCHAR)) " +
           "AND (:ratingLte IS NULL OR f.rating_stars <= :ratingLte)",
           nativeQuery = true)
    Page<CustomerFeedback> findByFilter(@Param("status") String status,
                                        @Param("ratingLte") Integer ratingLte,
                                        Pageable pageable);

    boolean existsByBookingId(String bookingId);
}
