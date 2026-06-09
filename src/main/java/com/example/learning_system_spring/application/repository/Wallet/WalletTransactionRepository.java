package com.example.learning_system_spring.application.repository.Wallet;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;

import java.util.Optional;

public interface WalletTransactionRepository {

    WalletTransaction save(WalletTransaction tx);

    Optional<WalletTransaction> findByReferenceCode(String referenceCode);

    /**
     * Tìm PENDING transaction theo referenceCode với pessimistic write lock.
     * Dùng trong CompleteTopUpUseCase để tránh race condition.
     */
    Optional<WalletTransaction> findPendingByRefForUpdate(String referenceCode);

    /**
     * Lấy danh sách PENDING đã quá hạn (expiredAt < now) để job dọn dẹp đánh dấu EXPIRED.
     *
     * @param limit số lượng tối đa mỗi lượt quét (tránh ôm quá nhiều một lần).
     */
    java.util.List<WalletTransaction> findExpiredPending(java.time.LocalDateTime now, int limit);

    /**
     * Danh sách giao dịch của một user, phân trang, sắp xếp createdAt DESC.
     */
    PageResult<WalletTransaction> findByUserId(Long userId, int page, int size);
}
