package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.ProtectedRoleException;
import lombok.experimental.UtilityClass;

/**
 * Luật bất biến của phân quyền động (chốt 2.3):
 * - {@code SUPER_ADMIN} luôn full quyền và KHÔNG được sửa ma trận qua API
 *   (read-only trong UI) → tránh tình huống tự khóa (lockout).
 *
 * Domain policy thuần túy, không gọi DB.
 */
@UtilityClass
public class RolePermissionPolicy {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** Role mà ma trận không được phép thay đổi qua API quản trị. */
    public boolean isProtected(String roleName) {
        return roleName != null && SUPER_ADMIN.equalsIgnoreCase(roleName.trim());
    }

    /**
     * Chặn mọi thao tác sửa permission của role được bảo vệ.
     *
     * @throws ProtectedRoleException nếu role là SUPER_ADMIN.
     */
    public void assertEditable(String roleName) {
        if (isProtected(roleName)) {
            throw new ProtectedRoleException(roleName);
        }
    }
}
