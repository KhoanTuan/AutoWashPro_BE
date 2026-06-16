package com.autowashpro.autowashpro_be.modules.identity.repository;

import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByPermissionCode(String permissionCode);
    List<Permission> findAllByOrderByPermissionCodeAsc();
}
