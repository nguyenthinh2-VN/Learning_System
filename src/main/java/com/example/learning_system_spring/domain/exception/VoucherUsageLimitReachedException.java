package com.example.learning_system_spring.domain.exception;

public class VoucherUsageLimitReachedException extends RuntimeException {
    public VoucherUsageLimitReachedException(String code) {
        super("Voucher đã hết lượt sử dụng: " + code);
    }
}
