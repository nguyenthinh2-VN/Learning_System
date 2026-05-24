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

    @ExceptionHandler(CourseNotPublishedException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotPublished(CourseNotPublishedException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.COURSE_NOT_PUBLISHED, ex.getMessage()));
    }

    @ExceptionHandler(CoursePriceLockedException.class)
    public ResponseEntity<ErrorResponse> handleCoursePriceLocked(CoursePriceLockedException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.COURSE_PRICE_LOCKED, ex.getMessage()));
    }

    @ExceptionHandler(CourseAlreadyPublishedException.class)
    public ResponseEntity<ErrorResponse> handleCourseAlreadyPublished(CourseAlreadyPublishedException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.COURSE_ALREADY_PUBLISHED, ex.getMessage()));
    }

    // ==== Voucher exceptions ====

    @ExceptionHandler(VoucherNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVoucherNotFound(VoucherNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.VOUCHER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(VoucherInactiveException.class)
    public ResponseEntity<ErrorResponse> handleVoucherInactive(VoucherInactiveException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_INACTIVE, ex.getMessage()));
    }

    @ExceptionHandler(VoucherNotYetActiveException.class)
    public ResponseEntity<ErrorResponse> handleVoucherNotYetActive(VoucherNotYetActiveException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_NOT_YET_ACTIVE, ex.getMessage()));
    }

    @ExceptionHandler(VoucherExpiredException.class)
    public ResponseEntity<ErrorResponse> handleVoucherExpired(VoucherExpiredException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_EXPIRED, ex.getMessage()));
    }

    @ExceptionHandler(VoucherNotApplicableException.class)
    public ResponseEntity<ErrorResponse> handleVoucherNotApplicable(VoucherNotApplicableException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_NOT_APPLICABLE, ex.getMessage()));
    }

    @ExceptionHandler(VoucherMinOrderNotMetException.class)
    public ResponseEntity<ErrorResponse> handleVoucherMinOrderNotMet(VoucherMinOrderNotMetException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET, ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsageLimitReachedException.class)
    public ResponseEntity<ErrorResponse> handleVoucherUsageLimitReached(VoucherUsageLimitReachedException ex) {
        return ResponseEntity.status(409).body(ErrorResponse.of(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED, ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsagePerUserExceededException.class)
    public ResponseEntity<ErrorResponse> handleVoucherUsagePerUser(VoucherUsagePerUserExceededException ex) {
        return ResponseEntity.status(409).body(ErrorResponse.of(ErrorCode.VOUCHER_USAGE_PER_USER_EXCEEDED, ex.getMessage()));
    }

    @ExceptionHandler(VoucherUseDeniedException.class)
    public ResponseEntity<ErrorResponse> handleVoucherUseDenied(VoucherUseDeniedException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ErrorCode.VOUCHER_USE_DENIED, ex.getMessage()));
    }

    @ExceptionHandler(VoucherCodeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleVoucherCodeExists(VoucherCodeAlreadyExistsException ex) {
        return ResponseEntity.status(409).body(ErrorResponse.of(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsageLimitTooLowException.class)
    public ResponseEntity<ErrorResponse> handleVoucherUsageLimitTooLow(VoucherUsageLimitTooLowException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_USAGE_LIMIT_TOO_LOW, ex.getMessage()));
    }

    @ExceptionHandler(VoucherImmutableFieldException.class)
    public ResponseEntity<ErrorResponse> handleVoucherImmutable(VoucherImmutableFieldException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.VOUCHER_IMMUTABLE_FIELD, ex.getMessage()));
    }

    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyEnrolled(AlreadyEnrolledException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.ALREADY_ENROLLED, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.INSUFFICIENT_BALANCE, ex.getMessage()));
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
