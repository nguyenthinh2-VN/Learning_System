package com.example.learning_system_spring.application.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kết quả khởi tạo thanh toán trả về cho FE.
 *
 * displayType:
 *   "QR_URL"  — displayData là URL ảnh QR (VietQR, Momo, v.v.)
 *   "MESSAGE" — displayData là text hướng dẫn (Mock/dev environment)
 */
public record PaymentInitResult(
        String referenceCode,
        BigDecimal amount,
        String displayData,
        String displayType,
        LocalDateTime expiredAt
) {}
