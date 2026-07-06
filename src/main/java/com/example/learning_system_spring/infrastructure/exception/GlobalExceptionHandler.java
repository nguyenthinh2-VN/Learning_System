package com.example.learning_system_spring.infrastructure.exception;

import com.example.learning_system_spring.domain.exception.*;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, ErrorCode.INVALID_EMAIL.name(), ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(409, ErrorCode.EMAIL_ALREADY_EXISTS.name(), ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.USER_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.COURSE_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.ROLE_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePermissionNotFound(PermissionNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.PERMISSION_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(ProtectedRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleProtectedRole(ProtectedRoleException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.PROTECTED_ROLE.name(), ex.getMessage()));
    }

    @ExceptionHandler(CourseAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseAccessDenied(CourseAccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.error(403, ErrorCode.ACCESS_DENIED.name(), ex.getMessage()));
    }

    @ExceptionHandler(SectionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSectionNotFound(SectionNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.SECTION_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(SectionAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSectionAccessDenied(SectionAccessDeniedException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, ErrorCode.SECTION_ACCESS_DENIED.name(), ex.getMessage()));
    }

    @ExceptionHandler(SectionTestNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSectionTestNotFound(SectionTestNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.SECTION_TEST_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidTestFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTestFormat(InvalidTestFormatException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.INVALID_TEST_FORMAT.name(), ex.getMessage()));
    }

    @ExceptionHandler(TestHasNoQuestionsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTestHasNoQuestions(TestHasNoQuestionsException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.TEST_HAS_NO_QUESTIONS.name(), ex.getMessage()));
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLessonNotFound(LessonNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.LESSON_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(LessonAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLessonAccessDenied(LessonAccessDeniedException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, ErrorCode.LESSON_ACCESS_DENIED.name(), ex.getMessage()));
    }

    @ExceptionHandler(CourseNotPublishedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseNotPublished(CourseNotPublishedException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.COURSE_NOT_PUBLISHED.name(), ex.getMessage()));
    }

    @ExceptionHandler(CoursePriceLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoursePriceLocked(CoursePriceLockedException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.COURSE_PRICE_LOCKED.name(), ex.getMessage()));
    }

    @ExceptionHandler(CourseAlreadyPublishedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseAlreadyPublished(CourseAlreadyPublishedException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.COURSE_ALREADY_PUBLISHED.name(), ex.getMessage()));
    }

    // ==== Voucher exceptions ====

    @ExceptionHandler(VoucherNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherNotFound(VoucherNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, ErrorCode.VOUCHER_NOT_FOUND.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherInactive(VoucherInactiveException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_INACTIVE.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherNotYetActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherNotYetActive(VoucherNotYetActiveException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_NOT_YET_ACTIVE.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherExpired(VoucherExpiredException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_EXPIRED.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherNotApplicableException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherNotApplicable(VoucherNotApplicableException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_NOT_APPLICABLE.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherMinOrderNotMetException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherMinOrderNotMet(VoucherMinOrderNotMetException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_MIN_ORDER_NOT_MET.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsageLimitReachedException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherUsageLimitReached(VoucherUsageLimitReachedException ex) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(409, ErrorCode.VOUCHER_USAGE_LIMIT_REACHED.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsagePerUserExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherUsagePerUser(VoucherUsagePerUserExceededException ex) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(409, ErrorCode.VOUCHER_USAGE_PER_USER_EXCEEDED.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherUseDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherUseDenied(VoucherUseDeniedException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, ErrorCode.VOUCHER_USE_DENIED.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherCodeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherCodeExists(VoucherCodeAlreadyExistsException ex) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(409, ErrorCode.VOUCHER_CODE_ALREADY_EXISTS.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherUsageLimitTooLowException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherUsageLimitTooLow(VoucherUsageLimitTooLowException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_USAGE_LIMIT_TOO_LOW.name(), ex.getMessage()));
    }

    @ExceptionHandler(VoucherImmutableFieldException.class)
    public ResponseEntity<ApiResponse<Void>> handleVoucherImmutable(VoucherImmutableFieldException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.VOUCHER_IMMUTABLE_FIELD.name(), ex.getMessage()));
    }

    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyEnrolled(AlreadyEnrolledException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.ALREADY_ENROLLED.name(), ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.INSUFFICIENT_BALANCE.name(), ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(403).body(
                ApiResponse.error(403, ErrorCode.ACCESS_DENIED.name(), "Bạn không có quyền thực hiện hành động này."));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401)
                .body(ApiResponse.error(401, ErrorCode.INVALID_CREDENTIALS.name(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.INVALID_PASSWORD.name(), ex.getMessage()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDisabled(AccountDisabledException ex) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(403, ErrorCode.ACCOUNT_DISABLED.name(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidFileType(InvalidFileTypeException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, ErrorCode.INVALID_FILE_TYPE.name(), ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, ErrorCode.FILE_TOO_LARGE.name(),
                "File vượt quá kích thước tối đa cho phép (2MB)."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(400).body(ApiResponse.error(400, ErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(ApiResponse.error(400, ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(400).body(ApiResponse.error(400, ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, ErrorCode.INTERNAL_ERROR.name(), "Internal server error"));
    }
}
