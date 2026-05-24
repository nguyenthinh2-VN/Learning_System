package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record CreateVoucherInput(
        Long requesterId,
        Role requesterRole,
        String code,
        VoucherType type,
        BigDecimal value,
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
