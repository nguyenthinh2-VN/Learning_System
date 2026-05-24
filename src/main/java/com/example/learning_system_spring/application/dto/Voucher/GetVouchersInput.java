package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Role;

public record GetVouchersInput(
        int page,
        int size,
        Long requesterId,
        Role requesterRole
) {
}
