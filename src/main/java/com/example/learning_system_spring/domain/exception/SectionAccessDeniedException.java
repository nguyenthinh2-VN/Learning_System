package com.example.learning_system_spring.domain.exception;

public class SectionAccessDeniedException extends RuntimeException {
    public SectionAccessDeniedException(String message) {
        super(message);
    }
}
