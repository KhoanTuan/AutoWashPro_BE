package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.common.exception.BadRequestException;
import com.autowashpro.autowashpro_be.common.exception.ResourceNotFoundException;
import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.repository.PermissionRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.RoleRepository;
import com.autowashpro.autowashpro_be.modules.identity.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final StaffRepository staffRepository;
    private final IdentityMapper mapper;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getRoleName))
                .map(role -> toRoleResponse(role))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Integer roleId) {
        return toRoleResponse(findRole(roleId));
    }

    @Transactional(readOnly = true)
    public RoleMatrixResponse getRbacMatrix(boolean includeDisabled) {
        List<PermissionSummary> permissions = getAllPermissions(includeDisabled);
        List<RoleMatrixRow> rows = roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getRoleName))
                .map(this::toMatrixRow)
                .toList();
        return RoleMatrixResponse.builder()
                .permissions(permissions)
                .roles(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PermissionSummary> getAllPermissions(Boolean includeDisabled) {
        return permissionRepository.findAllByOrderByModuleGroupAscPermissionCodeAsc().stream()
                .filter(p -> Boolean.TRUE.equals(includeDisabled) || Boolean.TRUE.equals(p.getEnabled()))
                .map(mapper::toPermissionSummary)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String roleName = request.getRoleName().trim().toUpperCase();
        if (PermissionCatalog.SYSTEM_ROLE_NAMES.contains(roleName)) {
            throw new BadRequestException("Cannot create role with reserved system name: " + roleName);
        }
        if (roleRepository.findByRoleName(roleName).isPresent()) {
            throw new BadRequestException("Role name already exists");
        }

        Role role = Role.builder()
                .roleName(roleName)
                .description(request.getDescription().trim())
                .permissions(new HashSet<>())
                .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            role.setPermissions(new HashSet<>(resolveAssignablePermissions(request.getPermissionIds())));
        }

        Role saved = roleRepository.save(role);
        return mapper.toRoleResponse(saved, 0);
    }

    @Transactional
    public RoleResponse updateRole(Integer roleId, UpdateRoleRequest request) {
        Role role = findRole(roleId);
        role.setDescription(request.getDescription().trim());
        Role saved = roleRepository.save(role);
        return toRoleResponse(saved);
    }

    @Transactional
    public void deleteRole(Integer roleId) {
        Role role = findRole(roleId);

        if (PermissionCatalog.NON_DELETABLE_ROLE_NAMES.contains(role.getRoleName())) {
            throw new BadRequestException("ROLE_ADMIN is protected and cannot be deleted");
        }
        if (PermissionCatalog.SYSTEM_ROLE_NAMES.contains(role.getRoleName())) {
            throw new BadRequestException("Cannot delete system role: " + role.getRoleName());
        }

        long assigned = staffRepository.countByRoleId(roleId);
        if (assigned > 0) {
            throw new BadRequestException("Cannot delete role assigned to " + assigned + " staff member(s)");
        }

        roleRepository.delete(role);
    }

    @Transactional
    public RoleResponse updateRolePermissions(Integer roleId, UpdateRolePermissionsRequest request) {
        Role role = findRole(roleId);
        assertPermissionEditable(role);

        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            if (PermissionCatalog.SYSTEM_ROLE_NAMES.contains(role.getRoleName())) {
                throw new BadRequestException("System role must retain at least one permission");
            }
            role.setPermissions(new HashSet<>());
            return toRoleResponse(roleRepository.save(role));
        }

        List<Permission> permissions = resolveAssignablePermissions(request.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));
        return toRoleResponse(roleRepository.save(role));
    }

    private void assertPermissionEditable(Role role) {
        if (PermissionCatalog.PERMISSION_LOCKED_ROLE_NAMES.contains(role.getRoleName())) {
            throw new BadRequestException(
                    "Permissions for " + role.getRoleName() + " are system-managed and cannot be modified");
        }
    }

    private List<Permission> resolveAssignablePermissions(List<Integer> permissionIds) {
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BadRequestException("One or more permissions not found");
        }
        for (Permission permission : permissions) {
            if (!Boolean.TRUE.equals(permission.getEnabled())) {
                throw new BadRequestException(
                        "Permission chưa mở (Flow 2+): " + permission.getPermissionCode());
            }
        }
        return permissions;
    }

    private RoleMatrixRow toMatrixRow(Role role) {
        boolean isSystem = PermissionCatalog.SYSTEM_ROLE_NAMES.contains(role.getRoleName());
        boolean permissionEditable = !PermissionCatalog.PERMISSION_LOCKED_ROLE_NAMES.contains(role.getRoleName());
        boolean deletable = !PermissionCatalog.NON_DELETABLE_ROLE_NAMES.contains(role.getRoleName())
                && !isSystem;

        List<Integer> permissionIds = role.getPermissions().stream()
                .map(Permission::getPermissionId)
                .sorted()
                .toList();

        return RoleMatrixRow.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .displayName(role.getDescription())
                .isSystem(isSystem)
                .permissionEditable(permissionEditable)
                .deletable(deletable)
                .staffCount((int) staffRepository.countByRoleId(role.getRoleId()))
                .permissionIds(permissionIds)
                .build();
    }

    private RoleResponse toRoleResponse(Role role) {
        return mapper.toRoleResponse(role, staffRepository.countByRoleId(role.getRoleId()));
    }

    private Role findRole(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }
}
