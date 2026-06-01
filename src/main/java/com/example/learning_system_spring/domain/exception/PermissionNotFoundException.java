package com.example.learning_system_spring.domain.exception;

public class PermissionNotFoundException extends RuntimeException {
    public PermissionNotFoundException(String name) {
        super("Không tìm thấy permission với tên: " + name);
    }
}
