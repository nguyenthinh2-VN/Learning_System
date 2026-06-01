package com.example.learning_system_spring.domain.exception;

public class ProtectedRoleException extends RuntimeException {
    public ProtectedRoleException(String roleName) {
        super("Không thể chỉnh sửa phân quyền của vai trò được bảo vệ: " + roleName);
    }
}
