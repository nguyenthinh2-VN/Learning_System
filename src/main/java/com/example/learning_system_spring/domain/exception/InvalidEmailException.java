package com.example.learning_system_spring.domain.exception;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String email) {
        super("Invalid email format: " + email);
    }
}
