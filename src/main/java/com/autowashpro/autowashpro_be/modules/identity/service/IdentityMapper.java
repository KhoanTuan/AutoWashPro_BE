package com.autowashpro.autowashpro_be.modules.identity.service;

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
                .requirePasswordChange(staff.getRequirePasswordChange())
                .status(staff.getStatus())
                .roles(roles)
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    public RoleResponse toRoleResponse(Role role) {
        List<PermissionSummary> permissions = role.getPermissions().stream()
                .map(p -> PermissionSummary.builder()
                        .permissionId(p.getPermissionId())
                        .permissionCode(p.getPermissionCode())
                        .build())
                .sorted(Comparator.comparing(PermissionSummary::getPermissionCode))
                .toList();

        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    public PermissionSummary toPermissionSummary(Permission permission) {
        return PermissionSummary.builder()
                .permissionId(permission.getPermissionId())
                .permissionCode(permission.getPermissionCode())
                .build();
    }
}
