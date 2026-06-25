package com.autowashpro.autowashpro_be.modules.operations.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.TaskStatus;
import com.autowashpro.autowashpro_be.modules.operations.entity.TaskChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskChecklistRepository extends JpaRepository<TaskChecklist, Long> {

    Optional<TaskChecklist> findFirstByBookingBookingIdOrderByIdAsc(Long bookingId);

    List<TaskChecklist> findByTechnicianStaffIdAndStatusInOrderByCreatedAtAsc(
            Long technicianId,
            List<TaskStatus> statuses
    );

    boolean existsByBookingBookingId(Long bookingId);
}
