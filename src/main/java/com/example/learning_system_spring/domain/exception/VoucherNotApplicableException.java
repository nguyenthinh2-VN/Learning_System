package com.example.learning_system_spring.domain.exception;

public class VoucherNotApplicableException extends RuntimeException {
    public VoucherNotApplicableException(String code, Long courseId) {
        super("Voucher " + code + " không áp dụng cho khóa học ID: " + courseId);
    }
}
