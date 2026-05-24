package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record VoucherOutput(
        Long id,
        String code,
        VoucherType type,
        BigDecimal value,
        VoucherStatus status,
        VoucherScope scope,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscount,
        Long usageLimit,
        Integer usagePerUser,
        Set<Long> applicableCourseIds,
        long usedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VoucherOutput from(Voucher v, long usedCount) {
        return new VoucherOutput(
                v.getId(), v.getCode(), v.getType(), v.getValue(),
                v.getStatus(), v.getScope(), v.getValidFrom(), v.getValidTo(),
                v.getMinOrderAmount(), v.getMaxDiscount(),
                v.getUsageLimit(), v.getUsagePerUser(),
                v.getApplicableCourseIds(),
                usedCount,
                v.getCreatedAt(), v.getUpdatedAt()
        );
    }
}
