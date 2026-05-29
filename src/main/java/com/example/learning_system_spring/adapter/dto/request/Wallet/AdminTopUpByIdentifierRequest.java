package com.example.learning_system_spring.adapter.dto.request.Wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request cộng tiền admin theo username HOẶC email (ô nhập 1 dòng).
 * Thay cho việc bắt nhập userId dạng số.
 */
public record AdminTopUpByIdentifierRequest(
        @NotBlank(message = "Vui lòng nhập username hoặc email")
        String identifier,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "1", message = "Số tiền cộng phải lớn hơn 0")
        BigDecimal amount,

        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
        String note
) {}
