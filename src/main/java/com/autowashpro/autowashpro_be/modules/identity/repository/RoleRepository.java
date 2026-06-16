package com.autowashpro.autowashpro_be.modules.identity.repository;

import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}
