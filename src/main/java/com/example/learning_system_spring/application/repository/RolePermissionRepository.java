package com.example.learning_system_spring.application.repository;

import com.example.learning_system_spring.domain.model.RolePermissions;

import java.util.List;

public interface RolePermissionRepository {

    /** Tập permission của một role (theo tên role). */
    RolePermissions findByRoleName(String roleName);

    /** Toàn bộ ma trận: mỗi role kèm tập permission của nó. */
    List<RolePermissions> findMatrix();

    /**
     * Thay thế toàn bộ permission của một role bằng danh sách mới (replace-all).
     * Phải chạy trong một transaction: xóa hết row cũ rồi insert row mới.
     */
    void replacePermissions(String roleName, List<String> permissionNames);
}
