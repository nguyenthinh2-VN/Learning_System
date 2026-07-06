package com.example.learning_system_spring.domain.exception;

public class InvalidTestFormatException extends RuntimeException {
    public InvalidTestFormatException(String message) {
        super(message);
    }
}
