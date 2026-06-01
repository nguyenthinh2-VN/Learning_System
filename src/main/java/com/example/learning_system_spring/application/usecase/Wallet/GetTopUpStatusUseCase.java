package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.dto.Wallet.TopUpStatusOutput;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc trạng thái một giao dịch nạp tiền — KHÔNG cộng tiền, chỉ đọc.
 *
 * Dùng cho FE poll khi complete-mode=IPN (chờ IPN cộng tiền xong) hoặc hiển thị kết quả.
 * Chặn truy cập chéo: chỉ chủ giao dịch (theo userId trong JWT) mới xem được.
 */
@Service
@RequiredArgsConstructor
public class GetTopUpStatusUseCase {

    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public TopUpStatusOutput execute(String referenceCode, Long requesterUserId) {
        WalletTransaction tx = walletTransactionRepository
                .findByReferenceCode(referenceCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy giao dịch với mã: " + referenceCode));

        if (!tx.getUserId().equals(requesterUserId)) {
            // Không tiết lộ giao dịch của người khác.
            throw new IllegalStateException("Không tìm thấy giao dịch với mã: " + referenceCode);
        }

        return new TopUpStatusOutput(
                tx.getReferenceCode(),
                tx.getStatus().name(),
                tx.getAmount()
        );
    }
}
