package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.dto.Wallet.TopUpStatusOutput;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người dùng chủ động huỷ giao dịch nạp tiền đang chờ (đóng tab VNPay/đổi ý).
 *
 * Chỉ huỷ được giao dịch PENDING của CHÍNH user (theo userId trong JWT).
 * Không cộng tiền. Idempotent: nếu đã COMPLETED/EXPIRED/FAILED thì trả về trạng thái hiện tại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelTopUpUseCase {

    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public TopUpStatusOutput execute(String referenceCode, Long requesterUserId) {
        WalletTransaction tx = walletTransactionRepository
                .findByReferenceCode(referenceCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy giao dịch với mã: " + referenceCode));

        if (!tx.getUserId().equals(requesterUserId)) {
            // Không tiết lộ giao dịch của người khác.
            throw new IllegalStateException("Không tìm thấy giao dịch với mã: " + referenceCode);
        }

        // Chỉ huỷ khi còn PENDING; các trạng thái khác giữ nguyên (idempotent).
        if (tx.isPending()) {
            tx.cancel();
            walletTransactionRepository.save(tx);
            log.info("[Wallet] User {} huỷ giao dịch nạp tiền ref={}", requesterUserId, referenceCode);
        }

        return new TopUpStatusOutput(tx.getReferenceCode(), tx.getStatus().name(), tx.getAmount());
    }
}
