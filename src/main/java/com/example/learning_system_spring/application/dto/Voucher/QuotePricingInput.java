package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Role;

public record QuotePricingInput(
        Long courseId,
        String voucherCode,
        Long requesterId,
        Role requesterRole,
        boolean isInternal
) {
}
