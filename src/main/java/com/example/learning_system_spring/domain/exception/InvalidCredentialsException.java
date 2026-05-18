package com.example.learning_system_spring.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Tên đăng nhập, email hoặc mật khẩu không chính xác");
    }
}
