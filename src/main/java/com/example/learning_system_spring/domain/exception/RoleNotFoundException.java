package com.example.learning_system_spring.domain.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String name) {
        super("Không tìm thấy vai trò với tên: " + name);
    }
}
