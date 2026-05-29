package com.example.learning_system_spring.domain.exception;

/**
 * Ném khi file upload có content type không nằm trong whitelist cho phép.
 */
public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String message) {
        super(message);
    }
}
