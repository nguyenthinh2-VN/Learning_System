package com.example.learning_system_spring.domain.exception;

public class VoucherExpiredException extends RuntimeException {
    public VoucherExpiredException(String code) {
        super("Voucher đã hết hạn: " + code);
    }
}
