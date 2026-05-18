package com.example.learning_system_spring.domain.exception;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String email) {
        super("Định dạng email không hợp lệ: " + email);
    }
}
