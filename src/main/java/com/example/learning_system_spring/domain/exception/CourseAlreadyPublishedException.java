package com.example.learning_system_spring.domain.exception;

public class CourseAlreadyPublishedException extends RuntimeException {
    public CourseAlreadyPublishedException(Long courseId) {
        super("Khóa học đã được duyệt và công khai trước đó. ID: " + courseId);
    }
}
