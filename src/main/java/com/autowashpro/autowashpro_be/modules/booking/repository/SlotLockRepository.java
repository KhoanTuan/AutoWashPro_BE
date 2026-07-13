package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.SlotLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotLockRepository extends JpaRepository<SlotLock, Long> {
    Optional<SlotLock> findByLockDateAndTimeSlotSlotId(LocalDate lockDate, Long slotId);
    List<SlotLock> findAllByLockDate(LocalDate lockDate);
}
