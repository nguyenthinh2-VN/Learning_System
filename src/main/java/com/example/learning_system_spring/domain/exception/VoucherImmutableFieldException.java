package com.example.learning_system_spring.domain.exception;

public class VoucherImmutableFieldException extends RuntimeException {
    public VoucherImmutableFieldException(String fieldName) {
        super("Voucher đã có lượt dùng, không thể thay đổi field: " + fieldName);
    }
}
