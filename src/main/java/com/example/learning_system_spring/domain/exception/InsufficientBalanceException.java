package com.example.learning_system_spring.domain.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Số dư ví không đủ để thanh toán. Vui lòng nạp thêm tiền.");
    }
}
