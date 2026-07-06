package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findAllByOrderByDisplayOrderAsc();
    List<TimeSlot> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}
