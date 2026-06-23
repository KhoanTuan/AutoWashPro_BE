package com.autowashpro.autowashpro_be.modules.booking.repository;

import com.autowashpro.autowashpro_be.modules.booking.entity.WashService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WashServiceRepository extends JpaRepository<WashService, Integer> {
}
