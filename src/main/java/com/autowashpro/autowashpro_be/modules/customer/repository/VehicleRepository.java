package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);

    List<Vehicle> findByCustomerCustomerIdOrderByCreatedAtAsc(Long customerId);

    Optional<Vehicle> findFirstByCustomerCustomerIdOrderByCreatedAtAsc(Long customerId);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM vehicle WHERE UPPER(license_plate) = UPPER(:licensePlate) LIMIT 1", nativeQuery = true)
    Optional<Vehicle> findByLicensePlateUnfiltered(@org.springframework.data.repository.query.Param("licensePlate") String licensePlate);

    long countByCustomerCustomerId(Long customerId);
}

