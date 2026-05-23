package com.example.learning_system_spring.infrastructure.exception;

import com.example.learning_system_spring.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.INVALID_EMAIL, ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(409).body(ErrorResponse.of(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.USER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.COURSE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(CourseAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCourseAccessDenied(CourseAccessDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(SectionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSectionNotFound(SectionNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.SECTION_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(SectionAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSectionAccessDenied(SectionAccessDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ErrorCode.SECTION_ACCESS_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLessonNotFound(LessonNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.LESSON_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(LessonAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleLessonAccessDenied(LessonAccessDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ErrorCode.LESSON_ACCESS_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401).body(ErrorResponse.of(ErrorCode.INVALID_CREDENTIALS, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"));
    }
}
