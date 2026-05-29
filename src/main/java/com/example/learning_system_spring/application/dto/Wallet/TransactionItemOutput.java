package com.example.learning_system_spring.application.dto.Wallet;

import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionItemOutput(
        Long id,
        String referenceCode,
        BigDecimal amount,        // luôn > 0
        String direction,         // "CREDIT" | "DEBIT"
        TxStatus status,
        TxSource source,
        String note,              // nullable
        LocalDateTime createdAt,
        LocalDateTime completedAt // nullable
) {
    public static TransactionItemOutput from(WalletTransaction tx) {
        String direction = (tx.getSource() == TxSource.PURCHASE) ? "DEBIT" : "CREDIT";
        return new TransactionItemOutput(
                tx.getId(),
                tx.getReferenceCode(),
                tx.getAmount(),
                direction,
                tx.getStatus(),
                tx.getSource(),
                tx.getNote(),
                tx.getCreatedAt(),
                tx.getCompletedAt());
    }
}
