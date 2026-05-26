package com.example.learning_system_spring.adapter.dto.request.Wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminTopUpRequest(
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1", message = "Số tiền cộng phải lớn hơn 0")
        BigDecimal amount,

        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
        String note
) {}
