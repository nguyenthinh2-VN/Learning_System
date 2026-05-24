package com.example.learning_system_spring.domain.exception;

public class CourseNotPublishedException extends RuntimeException {
    public CourseNotPublishedException(Long courseId) {
        super("Khóa học chưa được duyệt và công khai. ID: " + courseId);
    }

    public CourseNotPublishedException(String message) {
        super(message);
    }
}
