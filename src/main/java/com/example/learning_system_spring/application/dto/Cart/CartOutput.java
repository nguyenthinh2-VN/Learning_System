package com.example.learning_system_spring.application.dto.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartOutput(
        List<CartItemOutput> items,
        BigDecimal totalPrice
) {}
