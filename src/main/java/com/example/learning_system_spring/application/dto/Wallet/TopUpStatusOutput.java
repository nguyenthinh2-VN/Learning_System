package com.example.learning_system_spring.application.dto.Wallet;

import java.math.BigDecimal;

/**
 * Trạng thái một giao dịch nạp tiền (cho FE poll / hiển thị kết quả).
 *
 * @param referenceCode mã giao dịch
 * @param status        PENDING | COMPLETED | EXPIRED | FAILED
 * @param amount        số tiền nạp (nội bộ, không phải amount từ query VNPay)
 */
public record TopUpStatusOutput(
        String referenceCode,
        String status,
        BigDecimal amount
) {}
