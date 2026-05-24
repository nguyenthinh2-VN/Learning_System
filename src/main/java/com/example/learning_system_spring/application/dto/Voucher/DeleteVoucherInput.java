package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Role;

public record DeleteVoucherInput(
        Long voucherId,
        Long requesterId,
        Role requesterRole
) {
}
