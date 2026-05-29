package com.example.learning_system_spring.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Không tìm thấy người dùng với ID: " + id);
    }

    public UserNotFoundException(String identifier) {
        super("Không tìm thấy người dùng: " + identifier);
    }
}
