package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceCatalog;
import com.autowashpro.autowashpro_be.modules.booking.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    List<ServiceCatalog> findAllByIsActiveTrueOrderByDisplayOrderAsc();
    List<ServiceCatalog> findAllByOrderByDisplayOrderAsc();
    List<ServiceCatalog> findAllByServiceTypeAndIsActiveTrueOrderByDisplayOrderAsc(ServiceType serviceType);
    Optional<ServiceCatalog> findByServiceCode(String serviceCode);
    boolean existsByServiceCode(String serviceCode);
}
