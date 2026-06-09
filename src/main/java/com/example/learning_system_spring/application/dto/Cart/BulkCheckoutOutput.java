package com.example.learning_system_spring.application.dto.Cart;

import java.math.BigDecimal;
import java.util.List;

public record BulkCheckoutOutput(
        List<Long> enrolledCourseIds,
        List<Long> skippedCourseIds, // Courses already enrolled
        BigDecimal originalTotalPrice,
        BigDecimal discountAmount,
        BigDecimal finalTotalPrice,
        BigDecimal paidPrice,
        boolean voucherApplied,
        String voucherCode
) {}
