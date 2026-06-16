package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.identity.dto.PermissionSummary;
import com.autowashpro.autowashpro_be.modules.identity.dto.RoleResponse;
import com.autowashpro.autowashpro_be.modules.identity.dto.UpdateRolePermissionsRequest;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.repository.PermissionRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdentityMapper mapper;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(mapper::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionSummary> getAllPermissions() {
        return permissionRepository.findAllByOrderByPermissionCodeAsc().stream()
                .map(mapper::toPermissionSummary)
                .toList();
    }

    @Transactional
    public RoleResponse updateRolePermissions(Integer roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        if (permissions.size() != request.getPermissionIds().size()) {
            throw new BadRequestException("One or more permissions not found");
        }

        role.setPermissions(new HashSet<>(permissions));
        return mapper.toRoleResponse(roleRepository.save(role));
    }
}
