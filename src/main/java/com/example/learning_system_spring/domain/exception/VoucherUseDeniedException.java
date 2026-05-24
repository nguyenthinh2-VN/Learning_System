package com.example.learning_system_spring.domain.exception;

public class VoucherUseDeniedException extends RuntimeException {
    public VoucherUseDeniedException(String message) {
        super(message);
    }
}
