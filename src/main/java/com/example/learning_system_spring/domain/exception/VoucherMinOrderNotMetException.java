package com.example.learning_system_spring.domain.exception;

public class VoucherMinOrderNotMetException extends RuntimeException {
    public VoucherMinOrderNotMetException(String code) {
        super("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher: " + code);
    }
}
