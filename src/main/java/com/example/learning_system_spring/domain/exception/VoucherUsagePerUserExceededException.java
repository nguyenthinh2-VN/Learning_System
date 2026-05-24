package com.example.learning_system_spring.domain.exception;

public class VoucherUsagePerUserExceededException extends RuntimeException {
    public VoucherUsagePerUserExceededException(String code) {
        super("Bạn đã đạt giới hạn số lần dùng voucher này: " + code);
    }
}
