package com.autowashpro.autowashpro_be.modules.operations.repository;

import com.autowashpro.autowashpro_be.modules.operations.entity.QueueLane;
import com.autowashpro.autowashpro_be.modules.operations.entity.QueueStatus;
import com.autowashpro.autowashpro_be.modules.operations.entity.WaitingQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {

    Optional<WaitingQueue> findByBookingBookingId(Long bookingId);

    List<WaitingQueue> findByQueueLaneAndQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(
            QueueLane queueLane,
            List<QueueStatus> statuses,
            LocalDate bookingDate
    );

    List<WaitingQueue> findByQueueStatusInAndBookingBookingDateOrderByPriorityScoreDesc(
            List<QueueStatus> statuses,
            LocalDate bookingDate
    );
}
