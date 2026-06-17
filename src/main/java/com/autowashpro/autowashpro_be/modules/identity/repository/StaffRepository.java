package com.autowashpro.autowashpro_be.modules.identity.repository;

import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import com.autowashpro.autowashpro_be.modules.identity.entity.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT s FROM Staff s WHERE (:status IS NULL OR s.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Staff> search(@Param("status") StaffStatus status,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}
