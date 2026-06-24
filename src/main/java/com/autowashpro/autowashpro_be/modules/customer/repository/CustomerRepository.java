package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import com.autowashpro.autowashpro_be.modules.customer.entity.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByStatus(CustomerStatus status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt >= :since")
    long countRegisteredSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :activeStatus AND " +
           "(c.lastCompletedBookingAt IS NULL OR c.lastCompletedBookingAt < :threshold)")
    long countChurnRisk(@Param("activeStatus") CustomerStatus activeStatus,
                        @Param("threshold") LocalDateTime threshold);

    @Query("SELECT c FROM Customer c WHERE (:status IS NULL OR c.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR c.phoneNumber LIKE CONCAT('%', :keyword, '%') " +
           "OR LOWER(c.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> search(@Param("status") CustomerStatus status,
                          @Param("keyword") String keyword,
                          Pageable pageable);
}
