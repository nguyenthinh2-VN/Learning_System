package com.example.learning_system_spring.application.dto.Voucher;

import java.math.BigDecimal;

public record PurchaseCourseOutput(
        Long enrollmentId,
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        BigDecimal paidPrice,
        boolean voucherApplied,
        String voucherCode
) {
}
