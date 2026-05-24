package com.example.learning_system_spring.domain.exception;

public class CoursePriceLockedException extends RuntimeException {
    public CoursePriceLockedException(Long courseId) {
        super("Giá khóa học đã được khóa, chỉ admin mới có quyền sửa. ID: " + courseId);
    }
}
