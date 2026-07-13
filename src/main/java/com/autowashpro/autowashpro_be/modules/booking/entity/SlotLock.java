package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "slot_lock", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lock_date", "time_slot_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotLock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_lock_id")
    private Long slotLockId;

    @Column(name = "lock_date", nullable = false)
    private LocalDate lockDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Builder.Default
    @Column(name = "lock_count", nullable = false)
    private Integer lockCount = 0;
}
