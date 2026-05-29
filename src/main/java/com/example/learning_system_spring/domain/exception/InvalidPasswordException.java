package com.example.learning_system_spring.domain.exception;

/**
 * Ném khi mật khẩu hiện tại người dùng cung cấp không khớp (đổi mật khẩu).
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Mật khẩu hiện tại không đúng.");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }
}
