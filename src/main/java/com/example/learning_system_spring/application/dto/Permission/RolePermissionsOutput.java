package com.example.learning_system_spring.application.dto.Permission;

import com.example.learning_system_spring.domain.model.RolePermissions;

import java.util.List;

/**
 * Permission của một role + cờ {@code locked} cho biết role này có bị bảo vệ
 * (read-only) trên UI hay không.
 */
public record RolePermissionsOutput(String roleName, List<String> permissions, boolean locked) {
    public static RolePermissionsOutput from(RolePermissions rp, boolean locked) {
        return new RolePermissionsOutput(
                rp.getRoleName(),
                rp.getPermissionNames().stream().sorted().toList(),
                locked);
    }
}
