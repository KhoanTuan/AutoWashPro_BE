package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_item_id")
    private Long bookingItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "service_code_snapshot", nullable = false, length = 50)
    private String serviceCodeSnapshot;

    @Column(name = "service_name_snapshot", nullable = false, length = 150)
    private String serviceNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type_snapshot", nullable = false, length = 30)
    private ServiceType serviceTypeSnapshot;

    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;
}
