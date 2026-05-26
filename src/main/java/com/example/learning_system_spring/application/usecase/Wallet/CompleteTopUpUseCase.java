package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case dùng CHUNG cho mọi payment provider (Mock, VietQR, v.v.).
 *
 * Webhook controller chỉ parse request rồi gọi use case này.
 * Business logic hoàn toàn nằm ở đây — không lặp ở từng webhook controller.
 *
 * Trả về username để WalletNotificationService push WebSocket đúng user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteTopUpUseCase {

    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    public record Result(String username, Long userId, java.math.BigDecimal newBalance,
                         java.math.BigDecimal addedAmount, String source,
                         String referenceCode, String note) {}

    @Transactional
    public Result execute(String referenceCode, String externalNote) {
        // Pessimistic lock trên transaction để tránh double-complete
        WalletTransaction tx = walletTransactionRepository
                .findPendingByRefForUpdate(referenceCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy giao dịch PENDING với mã: " + referenceCode));

        // Domain method kiểm tra hết hạn + đổi status
        tx.complete(externalNote);
        walletTransactionRepository.save(tx);

        // Pessimistic lock trên user để cộng tiền an toàn
        User user = userRepository.findByIdForUpdate(tx.getUserId())
                .orElseThrow(() -> new UserNotFoundException(tx.getUserId()));

        user.addBalance(tx.getAmount());
        User saved = userRepository.save(user);

        log.info("[Wallet] Completed top-up: user={} amount={} ref={} source={}",
                saved.getUsername(), tx.getAmount(), referenceCode, tx.getSource());

        return new Result(
                saved.getUsername(),
                saved.getId(),
                saved.getBalance(),
                tx.getAmount(),
                tx.getSource().name(),
                referenceCode,
                externalNote
        );
    }
}
