package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.GarageClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GarageClosureRepository extends JpaRepository<GarageClosure, Long> {
    Optional<GarageClosure> findByClosureDate(LocalDate closureDate);
    boolean existsByClosureDate(LocalDate closureDate);
    int deleteByClosureDateBefore(LocalDate closureDate);
}
