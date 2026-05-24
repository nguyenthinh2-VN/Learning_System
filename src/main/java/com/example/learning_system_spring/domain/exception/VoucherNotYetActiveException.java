package com.example.learning_system_spring.domain.exception;

public class VoucherNotYetActiveException extends RuntimeException {
    public VoucherNotYetActiveException(String code) {
        super("Voucher chưa đến ngày hiệu lực: " + code);
    }
}
