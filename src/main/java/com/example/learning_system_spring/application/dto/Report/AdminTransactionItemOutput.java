package com.example.learning_system_spring.application.dto.Report;

import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một dòng giao dịch trong báo cáo admin — kèm thông tin user (toàn hệ thống).
 * direction: PURCHASE → "DEBIT" (tiền ra), còn lại → "CREDIT" (tiền vào).
 */
public record AdminTransactionItemOutput(
        Long id,
        Long userId,
        String username,
        String email,
        String referenceCode,
        BigDecimal amount,
        String direction,
        TxStatus status,
        TxSource source,
        String note,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static AdminTransactionItemOutput from(WalletTransaction tx, String username, String email) {
        String direction = (tx.getSource() == TxSource.PURCHASE) ? "DEBIT" : "CREDIT";
        return new AdminTransactionItemOutput(
                tx.getId(),
                tx.getUserId(),
                username,
                email,
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
