package com.example.learning_system_spring.domain.exception;

public class VoucherCodeAlreadyExistsException extends RuntimeException {
    public VoucherCodeAlreadyExistsException(String code) {
        super("Mã voucher đã tồn tại: " + code);
    }
}
