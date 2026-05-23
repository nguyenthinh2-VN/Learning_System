package com.example.learning_system_spring.domain.exception;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(Long lessonId) {
        super("Không tìm thấy bài giảng với id: " + lessonId);
    }
    
    public LessonNotFoundException(String message) {
        super(message);
    }
}