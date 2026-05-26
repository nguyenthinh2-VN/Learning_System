package com.example.learning_system_spring.application.usecase.Wallet;

import com.example.learning_system_spring.application.dto.Wallet.InitTopUpOutput;
import com.example.learning_system_spring.application.port.PaymentGateway;
import com.example.learning_system_spring.application.port.PaymentInitResult;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Khởi tạo yêu cầu nạp tiền.
 * Tạo pending transaction + gọi PaymentGateway để lấy thông tin hiển thị cho FE.
 *
 * Không biết provider là Mock hay VietQR — chỉ biết interface PaymentGateway.
 */
@Service
@RequiredArgsConstructor
public class InitTopUpUseCase {

    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentGateway paymentGateway;

    @Value("${wallet.topup.min-amount:10000}")
    private BigDecimal minAmount;

    @Value("${wallet.topup.pending-ttl-minutes:15}")
    private int ttlMinutes;

    @Transactional
    public InitTopUpOutput execute(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException(
                    "Số tiền nạp tối thiểu là " + minAmount.toPlainString() + "đ");
        }

        // Tạo pending transaction — referenceCode được sinh trong domain model
        WalletTransaction tx = WalletTransaction.createPending(
                userId, amount,
                TxSource.valueOf(paymentGateway.providerName()),
                ttlMinutes);

        WalletTransaction saved = walletTransactionRepository.save(tx);

        // Gọi gateway để lấy thông tin hiển thị (QR URL hoặc message hướng dẫn)
        PaymentInitResult result = paymentGateway.initPayment(saved.getReferenceCode(), amount);

        return new InitTopUpOutput(
                result.referenceCode(),
                result.amount(),
                result.displayData(),
                result.displayType(),
                result.expiredAt()
        );
    }
}
