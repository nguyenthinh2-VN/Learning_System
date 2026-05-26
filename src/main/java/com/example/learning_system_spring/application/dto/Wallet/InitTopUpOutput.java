package com.example.learning_system_spring.application.dto.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InitTopUpOutput(
        String referenceCode,
        BigDecimal amount,
        String displayData,
        String displayType,
        LocalDateTime expiredAt
) {}
