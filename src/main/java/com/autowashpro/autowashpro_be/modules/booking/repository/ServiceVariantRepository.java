package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceVariant;
import com.autowashpro.autowashpro_be.modules.customer.entity.CarType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceVariantRepository extends JpaRepository<ServiceVariant, Integer> {
    Optional<ServiceVariant> findByServiceServiceIdAndCarType(Integer serviceId, CarType carType);
}
