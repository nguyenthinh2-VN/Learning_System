package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dọn dẹp các giao dịch nạp tiền PENDING đã quá hạn (user đóng tab/đổi ý, không hoàn tất).
 *
 * Chuyển PENDING → EXPIRED khi vượt expiredAt. KHÔNG cộng tiền.
 * Được gọi định kỳ bởi scheduled job; cũng có thể gọi thủ công nếu cần.
 *
 * Idempotent + an toàn: chỉ tác động lên PENDING, không đụng COMPLETED/EXPIRED/FAILED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireStalePendingTopUpsUseCase {

    private final WalletTransactionRepository walletTransactionRepository;

    /** Số giao dịch tối đa xử lý mỗi lượt quét — tránh ôm quá nhiều trong một transaction. */
    private static final int BATCH_LIMIT = 200;

    /**
     * @return số giao dịch đã chuyển sang EXPIRED.
     */
    @Transactional
    public int execute() {
        LocalDateTime now = LocalDateTime.now();
        List<WalletTransaction> stale = walletTransactionRepository.findExpiredPending(now, BATCH_LIMIT);

        int expired = 0;
        for (WalletTransaction tx : stale) {
            try {
                tx.expire();
                walletTransactionRepository.save(tx);
                expired++;
            } catch (IllegalStateException e) {
                // Đã đổi trạng thái bởi luồng khác giữa lúc đọc và xử lý — bỏ qua.
                log.debug("[TopUpExpiry] Bỏ qua ref={}: {}", tx.getReferenceCode(), e.getMessage());
            }
        }

        if (expired > 0) {
            log.info("[TopUpExpiry] Đã đánh dấu hết hạn {} giao dịch nạp tiền treo", expired);
        }
        return expired;
    }
}
