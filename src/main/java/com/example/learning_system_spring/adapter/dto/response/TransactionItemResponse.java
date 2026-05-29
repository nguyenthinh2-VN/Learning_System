package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Wallet.TransactionItemOutput;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionItemResponse(
        Long id,
        String referenceCode,
        BigDecimal amount,
        String direction,
        String status,
        String source,
        String note,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TransactionItemResponse from(TransactionItemOutput o) {
        return new TransactionItemResponse(
                o.id(),
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
