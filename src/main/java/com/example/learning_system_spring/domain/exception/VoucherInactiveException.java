package com.example.learning_system_spring.domain.exception;

public class VoucherInactiveException extends RuntimeException {
    public VoucherInactiveException(String code) {
        super("Voucher đã bị vô hiệu hóa: " + code);
    }
}
