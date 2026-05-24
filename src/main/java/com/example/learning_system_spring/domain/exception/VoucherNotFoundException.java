package com.example.learning_system_spring.domain.exception;

public class VoucherNotFoundException extends RuntimeException {
    public VoucherNotFoundException(String code) {
        super("Không tìm thấy voucher với mã: " + code);
    }
    public VoucherNotFoundException(Long id) {
        super("Không tìm thấy voucher với ID: " + id);
    }
}
