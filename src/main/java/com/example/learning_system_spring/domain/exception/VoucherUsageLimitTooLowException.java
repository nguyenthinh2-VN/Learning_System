package com.example.learning_system_spring.domain.exception;

public class VoucherUsageLimitTooLowException extends RuntimeException {
    public VoucherUsageLimitTooLowException(long currentUsage, long newLimit) {
        super("Không thể giảm usageLimit (" + newLimit + ") xuống dưới số lượt đã dùng (" + currentUsage + ")");
    }
}
