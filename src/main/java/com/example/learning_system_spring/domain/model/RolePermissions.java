package com.example.learning_system_spring.domain.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Aggregate phục vụ luồng quản trị phân quyền động: gắn một role (theo tên) với
 * tập permission (theo tên) mà role đó đang sở hữu.
 *
 * Cố ý KHÔNG nhúng vào {@link Role} để tránh phải sửa hàng loạt nơi tái tạo
 * {@code Role} từ JWT claim (vốn không có thông tin permission).
 */
public class RolePermissions {

    private final String roleName;
    private final Set<String> permissionNames;

    private RolePermissions(String roleName, Set<String> permissionNames) {
        this.roleName = roleName;
        this.permissionNames = permissionNames;
    }

    public static RolePermissions of(String roleName, Set<String> permissionNames) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
        Set<String> normalized = new LinkedHashSet<>();
        if (permissionNames != null) {
            permissionNames.stream()
                    .filter(p -> p != null && !p.isBlank())
                    .map(p -> p.toUpperCase().trim())
                    .forEach(normalized::add);
        }
        return new RolePermissions(roleName.toUpperCase().trim(), normalized);
    }

    public boolean has(String permissionName) {
        if (permissionName == null) return false;
        return permissionNames.contains(permissionName.toUpperCase().trim());
    }

    public String getRoleName() {
        return roleName;
    }

    public Set<String> getPermissionNames() {
        return Collections.unmodifiableSet(permissionNames);
    }
}
