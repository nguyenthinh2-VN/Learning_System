package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Wallet.TransactionItemOutput;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lấy lịch sử giao dịch ví của chính người gọi (phân trang, mới nhất trước).
 * Chỉ trả về giao dịch có userId bằng userId của requester.
 */
@Service
@RequiredArgsConstructor
public class GetMyTransactionsUseCase {

    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public PageResult<TransactionItemOutput> execute(Long userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size phải trong khoảng [1, 100]");
        }
        return walletTransactionRepository.findByUserId(userId, page, size)
                .map(TransactionItemOutput::from);
    }
}
