package com.example.learning_system_spring.application.dto.Permission;

import java.util.List;

/**
 * Toàn bộ ma trận phân quyền: danh sách tất cả permission khả dụng + permission
 * của từng role. FE dùng để render bảng checkbox role × permission.
 */
public record PermissionMatrixOutput(
        List<PermissionOutput> allPermissions,
        List<RolePermissionsOutput> roles) {
}
