package com.autowashpro.autowashpro_be.modules.identity.service;

import com.autowashpro.autowashpro_be.modules.identity.PermissionCatalog;
import com.autowashpro.autowashpro_be.modules.identity.dto.*;
import com.autowashpro.autowashpro_be.modules.identity.entity.Permission;
import com.autowashpro.autowashpro_be.modules.identity.entity.Role;
import com.autowashpro.autowashpro_be.modules.identity.entity.Staff;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class IdentityMapper {

    public StaffResponse toStaffResponse(Staff staff) {
        List<RoleSummary> roles = staff.getRoles().stream()
                .map(r -> RoleSummary.builder()
                        .roleId(r.getRoleId())
                        .roleName(r.getRoleName())
                        .build())
                .sorted(Comparator.comparing(RoleSummary::getRoleName))
                .toList();

        return StaffResponse.builder()
                .staffId(staff.getStaffId())
                .username(staff.getUsername())
                .email(staff.getEmail())
                .fullName(staff.getFullName())
                .phoneNumber(staff.getPhoneNumber())
                .requirePasswordChange(staff.getRequirePasswordChange())
                .status(staff.getStatus())
                .roleLabel(resolveRoleLabel(staff))
                .roles(roles)
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .deletedAt(staff.getDeletedAt())
                .build();
    }

    private static String resolveRoleLabel(Staff staff) {
        return staff.getRoles().stream()
                .map(Role::getDescription)
                .filter(d -> d != null && !d.isBlank())
                .findFirst()
                .orElse(staff.getRoles().stream()
                        .map(Role::getRoleName)
                        .min(String::compareToIgnoreCase)
                        .orElse("Staff"));
    }

    public RoleResponse toRoleResponse(Role role, long staffCount) {
        List<PermissionSummary> permissions = role.getPermissions().stream()
                .map(this::toPermissionSummary)
                .sorted(Comparator.comparing(PermissionSummary::getPermissionCode))
                .toList();

        boolean isSystem = PermissionCatalog.SYSTEM_ROLE_NAMES.contains(role.getRoleName());

        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .displayName(role.getDescription())
                .isSystem(isSystem)
                .staffCount((int) staffCount)
                .permissions(permissions)
                .build();
    }

    public RoleResponse toRoleResponse(Role role) {
        return toRoleResponse(role, 0);
    }

    public PermissionSummary toPermissionSummary(Permission permission) {
        return PermissionSummary.builder()
                .permissionId(permission.getPermissionId())
                .permissionCode(permission.getPermissionCode())
                .description(permission.getDescription())
                .moduleGroup(permission.getModuleGroup())
                .phase(permission.getPhase())
                .enabled(permission.getEnabled())
                .build();
    }
}
