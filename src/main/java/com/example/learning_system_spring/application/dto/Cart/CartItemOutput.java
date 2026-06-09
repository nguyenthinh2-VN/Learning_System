package com.example.learning_system_spring.application.dto.Cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemOutput(
        Long courseId,
        String title,
        BigDecimal price,
        String thumbnail,
        LocalDateTime addedAt
) {}
