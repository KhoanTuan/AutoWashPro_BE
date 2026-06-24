package com.autowashpro.autowashpro_be.modules.identity.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Column(name = "require_password_change", nullable = false)
    @Builder.Default
    private Boolean requirePasswordChange = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_status", nullable = false, length = 20)
    @Builder.Default
    private StaffWorkStatus workStatus = StaffWorkStatus.IDLE;

    /** KPI hiệu suất 0–100 — chỉ hệ thống cập nhật, không sửa qua CRUD */
    @Column(name = "performance_kpi", nullable = false)
    @Builder.Default
    private Double performanceKpi = 0.0;

    @Column(name = "total_jobs_completed", nullable = false)
    @Builder.Default
    private Integer totalJobsCompleted = 0;

    /** Điểm đánh giá dịch vụ 0–5 — read-only qua API quản trị */
    @Column(name = "service_rating", nullable = false)
    @Builder.Default
    private Double serviceRating = 5.0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "staff_role",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
