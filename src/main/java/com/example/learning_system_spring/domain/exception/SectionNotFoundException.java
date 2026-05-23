package com.example.learning_system_spring.domain.exception;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(Long id) {
        super("Không tìm thấy chương học với ID: " + id);
    }
}
