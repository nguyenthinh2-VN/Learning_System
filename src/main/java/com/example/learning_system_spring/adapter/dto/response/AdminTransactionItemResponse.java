package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Report.AdminTransactionItemOutput;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response 1 dòng giao dịch toàn hệ thống (admin) — enum serialize thành String.
 */
public record AdminTransactionItemResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String referenceCode,
        BigDecimal amount,
        String direction,
        String status,
        String source,
        String note,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static AdminTransactionItemResponse from(AdminTransactionItemOutput o) {
        return new AdminTransactionItemResponse(
                o.id(),
                o.userId(),
                o.username(),
                o.email(),
                o.referenceCode(),
                o.amount(),
                o.direction(),
                o.status().name(),
                o.source().name(),
                o.note(),
                o.createdAt(),
                o.completedAt());
    }
}
