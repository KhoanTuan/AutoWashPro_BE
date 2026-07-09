package com.autowashpro.autowashpro_be.modules.marketing.entity;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "booking_id", length = 50)
    private String bookingId;

    @Column(name = "service_name", length = 150)
    private String serviceName;

    @Column(name = "rating_stars", nullable = false)
    private Integer ratingStars;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "compensation_voucher_code", length = 80)
    private String compensationVoucherCode;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = FeedbackStatus.NEW;
    }
}
