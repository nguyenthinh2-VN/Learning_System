package com.example.learning_system_spring.domain.exception;

public class AlreadyEnrolledException extends RuntimeException {
    public AlreadyEnrolledException(Long userId, Long courseId) {
        super("User " + userId + " đã enroll khóa học " + courseId);
    }
}
