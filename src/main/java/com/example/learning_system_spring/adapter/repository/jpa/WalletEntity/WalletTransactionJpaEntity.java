package com.example.learning_system_spring.adapter.repository.jpa.WalletEntity;

import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_tx_ref", columnNames = "reference_code")
        },
        indexes = {
                @Index(name = "idx_wallet_tx_ref", columnList = "reference_code"),
                @Index(name = "idx_wallet_tx_user", columnList = "user_id"),
                @Index(name = "idx_wallet_tx_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WalletTransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reference_code", nullable = false, length = 32, unique = true)
    private String referenceCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxSource source;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public WalletTransaction toDomain() {
        return WalletTransaction.reconstitute(
                id, userId, referenceCode, amount, status, source,
                note, createdAt, completedAt, expiredAt);
    }

    public static WalletTransactionJpaEntity fromDomain(WalletTransaction tx) {
        WalletTransactionJpaEntity e = new WalletTransactionJpaEntity();
        e.id = tx.getId();
        e.userId = tx.getUserId();
        e.referenceCode = tx.getReferenceCode();
        e.amount = tx.getAmount();
        e.status = tx.getStatus();
        e.source = tx.getSource();
        e.note = tx.getNote();
        e.createdAt = tx.getCreatedAt();
        e.completedAt = tx.getCompletedAt();
        e.expiredAt = tx.getExpiredAt();
        return e;
    }
}
