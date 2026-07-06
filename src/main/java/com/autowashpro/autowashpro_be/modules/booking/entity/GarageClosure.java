package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "garage_closure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageClosure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closure_id")
    private Long closureId;

    @Column(name = "closure_date", nullable = false, unique = true)
    private LocalDate closureDate;

    @Column(name = "reason", length = 255)
    private String reason;

    @Builder.Default
    @Column(name = "is_full_day", nullable = false)
    private Boolean isFullDay = true;
}
