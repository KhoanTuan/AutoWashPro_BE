package com.autowashpro.autowashpro_be.modules.operations.repository;

import com.autowashpro.autowashpro_be.modules.operations.entity.TechnicalChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicalChecklistItemRepository extends JpaRepository<TechnicalChecklistItem, Long> {

    List<TechnicalChecklistItem> findByTaskChecklistIdOrderBySortOrderAsc(Long taskChecklistId);
}
