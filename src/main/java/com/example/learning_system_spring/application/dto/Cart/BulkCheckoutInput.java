package com.example.learning_system_spring.application.dto.Cart;

import com.example.learning_system_spring.domain.model.Role;

import java.util.List;

public record BulkCheckoutInput(
        Long requesterId,
        Role requesterRole,
        boolean isInternal,
        List<Long> courseIds,
        String voucherCode
) {}
