package com.example.learning_system_spring.domain.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email đã tồn tại trong hệ thống: " + email);
    }
}
