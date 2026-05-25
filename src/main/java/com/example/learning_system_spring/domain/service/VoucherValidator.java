package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.exception.VoucherExpiredException;
import com.example.learning_system_spring.domain.exception.VoucherInactiveException;
import com.example.learning_system_spring.domain.exception.VoucherMinOrderNotMetException;
import com.example.learning_system_spring.domain.exception.VoucherNotApplicableException;
import com.example.learning_system_spring.domain.exception.VoucherNotYetActiveException;
import com.example.learning_system_spring.domain.exception.VoucherUsageLimitReachedException;
import com.example.learning_system_spring.domain.exception.VoucherUsagePerUserExceededException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain service kiểm tra một voucher có hợp lệ trong context cho trước hay không.
 * KHÔNG phụ thuộc Spring / JPA. Ném domain exception cụ thể cho mỗi loại lỗi.
 *
 * Thứ tự kiểm tra (cố định, để bảo đảm cùng đầu vào → cùng exception):
 *   1. status = ACTIVE
 *   2. validFrom <= now
 *   3. now <= validTo
 *   4. scope (course có nằm trong applicableCourseIds)
 *   5. minOrderAmount
 *   6. usageLimit (toàn cục)
 *   7. usagePerUser
 */
public class VoucherValidator {

    public void validate(Voucher voucher,
                         Long courseId,
                         BigDecimal originalPrice,
                         LocalDateTime now,
                         long globalUsedCount,
                         long perUserUsedCount) {
        if (voucher == null) {
            throw new IllegalArgumentException("voucher must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (originalPrice == null) {
            throw new IllegalArgumentException("originalPrice must not be null");
        }

        // 1. Status — FIX-A: kiểm tra strict ACTIVE thay vì chỉ check isInactive()
        //    Điều này bắt cả trường hợp status = null (do bug cũ) hoặc giá trị enum không mong đợi.
        if (voucher.getStatus() != com.example.learning_system_spring.domain.model.Voucher.VoucherStatus.ACTIVE) {
            throw new VoucherInactiveException(voucher.getCode());
        }

        // 2. validFrom
        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom())) {
            throw new VoucherNotYetActiveException(voucher.getCode());
        }

        // 3. validTo
        if (voucher.getValidTo() != null && now.isAfter(voucher.getValidTo())) {
            throw new VoucherExpiredException(voucher.getCode());
        }

        // 4. Scope
        if (!voucher.appliesTo(courseId)) {
            throw new VoucherNotApplicableException(voucher.getCode(), courseId);
        }

        // 5. minOrderAmount
        BigDecimal minOrder = voucher.getMinOrderAmount();
        if (minOrder != null && minOrder.signum() > 0 && originalPrice.compareTo(minOrder) < 0) {
            throw new VoucherMinOrderNotMetException(voucher.getCode());
        }

        // 6. Usage limit toàn cục (0 = không giới hạn)
        Long usageLimit = voucher.getUsageLimit();
        if (usageLimit != null && usageLimit > 0 && globalUsedCount >= usageLimit) {
            throw new VoucherUsageLimitReachedException(voucher.getCode());
        }

        // 7. Usage per user (0 = không giới hạn)
        Integer usagePerUser = voucher.getUsagePerUser();
        if (usagePerUser != null && usagePerUser > 0 && perUserUsedCount >= usagePerUser) {
            throw new VoucherUsagePerUserExceededException(voucher.getCode());
        }
    }
}
