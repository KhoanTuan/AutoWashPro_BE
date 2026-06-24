package com.autowashpro.autowashpro_be.modules.booking.entity;

import com.autowashpro.autowashpro_be.common.base.BaseEntity;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service_variant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private WashService service;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false, length = 20)
    private CarType carType;

    @Column(name = "calculated_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal calculatedPrice;
}
