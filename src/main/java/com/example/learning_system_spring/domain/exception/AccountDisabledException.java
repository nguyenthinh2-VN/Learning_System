package com.example.learning_system_spring.domain.exception;

/**
 * Ném khi user bị khóa (enabled = false) cố gắng đăng nhập.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
    }

    public AccountDisabledException(String message) {
        super(message);
    }
}
