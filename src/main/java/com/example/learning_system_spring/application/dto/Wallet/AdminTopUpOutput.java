package com.example.learning_system_spring.application.dto.Wallet;

import java.math.BigDecimal;

public record AdminTopUpOutput(
        Long userId,
        String username,
        BigDecimal addedAmount,
        BigDecimal newBalance,
        String note,
        String referenceCode
) {}
