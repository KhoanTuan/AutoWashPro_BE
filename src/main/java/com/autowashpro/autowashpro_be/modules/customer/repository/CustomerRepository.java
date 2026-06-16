package com.autowashpro.autowashpro_be.modules.customer.repository;

import com.autowashpro.autowashpro_be.modules.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
