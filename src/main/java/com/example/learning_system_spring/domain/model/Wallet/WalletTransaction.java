package com.example.learning_system_spring.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure domain model — không có annotation Spring/JPA.
 */
public class WalletTransaction {

    private Long id;
    private Long userId;
    private String referenceCode;
    private BigDecimal amount;
    private TxStatus status;
    private TxSource source;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;

    private WalletTransaction() {}

    /** Tạo pending transaction mới (user tự nạp qua payment gateway). */
    public static WalletTransaction createPending(Long userId, BigDecimal amount,
                                                   TxSource source, int ttlMinutes) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        WalletTransaction tx = new WalletTransaction();
        tx.userId = userId;
        tx.referenceCode = generateReferenceCode();
        tx.amount = amount;
        tx.status = TxStatus.PENDING;
        tx.source = source;
        tx.createdAt = LocalDateTime.now();
        tx.expiredAt = LocalDateTime.now().plusMinutes(ttlMinutes);
        return tx;
    }

    /** Tạo completed transaction ngay (admin cộng tiền thủ công). */
    public static WalletTransaction createCompleted(Long userId, BigDecimal amount,
                                                     TxSource source, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền cộng phải lớn hơn 0");
        }
        WalletTransaction tx = new WalletTransaction();
        tx.userId = userId;
        tx.referenceCode = generateReferenceCode();
        tx.amount = amount;
        tx.status = TxStatus.COMPLETED;
        tx.source = source;
        tx.note = note;
        tx.createdAt = LocalDateTime.now();
        tx.completedAt = LocalDateTime.now();
        // Admin tx không cần expiredAt nhưng DB NOT NULL — đặt xa tương lai
        tx.expiredAt = LocalDateTime.now().plusYears(100);
        return tx;
    }

    /** Tạo completed transaction cho giao dịch mua khóa học (tiền ra khỏi ví). */
    public static WalletTransaction createPurchase(Long userId, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền mua phải lớn hơn 0");
        }
        WalletTransaction tx = new WalletTransaction();
        tx.userId = userId;
        tx.referenceCode = generateReferenceCode("BUY");
        tx.amount = amount;
        tx.status = TxStatus.COMPLETED;
        tx.source = TxSource.PURCHASE;
        tx.note = note;
        tx.createdAt = LocalDateTime.now();
        tx.completedAt = LocalDateTime.now();
        // Purchase tx không cần expiredAt nhưng DB NOT NULL — đặt xa tương lai
        tx.expiredAt = LocalDateTime.now().plusYears(100);
        return tx;
    }

    /** Reconstitute từ DB. */
    public static WalletTransaction reconstitute(Long id, Long userId, String referenceCode,
                                                  BigDecimal amount, TxStatus status, TxSource source,
                                                  String note, LocalDateTime createdAt,
                                                  LocalDateTime completedAt, LocalDateTime expiredAt) {
        WalletTransaction tx = new WalletTransaction();
        tx.id = id;
        tx.userId = userId;
        tx.referenceCode = referenceCode;
        tx.amount = amount;
        tx.status = status;
        tx.source = source;
        tx.note = note;
        tx.createdAt = createdAt;
        tx.completedAt = completedAt;
        tx.expiredAt = expiredAt;
        return tx;
    }

    /** Đánh dấu hoàn thành — gọi khi webhook xác nhận thanh toán. */
    public void complete(String note) {
        if (this.status != TxStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hoàn thành giao dịch đang PENDING");
        }
        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            this.status = TxStatus.EXPIRED;
            throw new IllegalStateException("Giao dịch đã hết hạn");
        }
        this.status = TxStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.note = note;
    }

    public boolean isPending() {
        return this.status == TxStatus.PENDING;
    }

    public boolean isExpired() {
        return this.status == TxStatus.PENDING && LocalDateTime.now().isAfter(this.expiredAt);
    }

    private static String generateReferenceCode() {
        return generateReferenceCode("NAP");
    }

    private static String generateReferenceCode(String prefix) {
        // prefix + 9 ký tự hex uppercase — đủ ngẫu nhiên, không đoán được
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + uuid.substring(0, 9);
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getReferenceCode() { return referenceCode; }
    public BigDecimal getAmount() { return amount; }
    public TxStatus getStatus() { return status; }
    public TxSource getSource() { return source; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
}
