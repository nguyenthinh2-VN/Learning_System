package com.example.learning_system_spring.domain.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super("Không tìm thấy khóa học với ID: " + id);
    }
}
