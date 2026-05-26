package com.example.learning_system_spring.adapter.dto.request.Wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitTopUpRequest(
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "10000", message = "Số tiền nạp tối thiểu là 10,000đ")
        BigDecimal amount
) {}
