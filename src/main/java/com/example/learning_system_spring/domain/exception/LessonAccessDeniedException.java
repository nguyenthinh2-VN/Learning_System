package com.example.learning_system_spring.domain.exception;

public class LessonAccessDeniedException extends RuntimeException {
    public LessonAccessDeniedException(Long lessonId) {
        super("Không có quyền thao tác bài giảng với id: " + lessonId);
    }
    
    public LessonAccessDeniedException(String message) {
        super(message);
    }
}