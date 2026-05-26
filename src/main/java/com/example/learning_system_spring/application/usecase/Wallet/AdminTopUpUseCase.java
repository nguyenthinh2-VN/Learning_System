package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.dto.Wallet.AdminTopUpOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Admin cộng tiền thủ công cho user bất kỳ.
 * Không qua pending — tạo COMPLETED transaction ngay lập tức.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTopUpUseCase {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public AdminTopUpOutput execute(Long targetUserId, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền cộng phải lớn hơn 0");
        }

        // Pessimistic lock trên user
        User user = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        user.addBalance(amount);
        User saved = userRepository.save(user);

        // Tạo audit record
        WalletTransaction tx = WalletTransaction.createCompleted(
                targetUserId, amount, TxSource.ADMIN, note);
        WalletTransaction savedTx = walletTransactionRepository.save(tx);

        log.info("[Wallet] Admin top-up: user={} amount={} note={}",
                saved.getUsername(), amount, note);

        return new AdminTopUpOutput(
                saved.getId(),
                saved.getUsername(),
                amount,
                saved.getBalance(),
                note,
                savedTx.getReferenceCode()
        );
    }
}
