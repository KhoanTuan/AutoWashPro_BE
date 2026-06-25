package com.autowashpro.autowashpro_be.modules.financial.repository;

import com.autowashpro.autowashpro_be.modules.financial.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
