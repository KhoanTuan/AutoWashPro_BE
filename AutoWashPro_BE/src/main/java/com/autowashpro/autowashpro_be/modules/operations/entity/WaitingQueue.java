package com.autowashpro.autowashpro_be.modules.operations.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "waiting_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_waiting_queue_booking", columnNames = "booking_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long queueId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_lane", nullable = false, length = 20)
    private QueueLane queueLane;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 20)
    @Builder.Default
    private QueueStatus queueStatus = QueueStatus.WAITING;

    @Column(name = "priority_score", nullable = false)
    @Builder.Default
    private Double priorityScore = 0.0;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "lane_position")
    private Integer lanePosition;
}
