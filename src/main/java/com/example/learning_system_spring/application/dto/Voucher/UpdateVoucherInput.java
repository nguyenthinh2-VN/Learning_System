package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record UpdateVoucherInput(
        Long voucherId,
        Long requesterId,
        Role requesterRole,
        // Immutable fields — nullable: null = giữ nguyên, non-null = muốn sửa
        // UseCase sẽ chặn nếu usedCount > 0
        String newCode,
        VoucherType newType,
        BigDecimal newValue,
        // Soft fields
        VoucherStatus status,
        VoucherScope scope,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscount,
        Long usageLimit,
        Integer usagePerUser,
        Set<Long> applicableCourseIds
) {
}
