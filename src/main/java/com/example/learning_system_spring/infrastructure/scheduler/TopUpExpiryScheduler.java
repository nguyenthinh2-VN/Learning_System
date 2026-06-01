package com.example.learning_system_spring.infrastructure.scheduler;

import com.example.learning_system_spring.application.usecase.Wallet.ExpireStalePendingTopUpsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job định kỳ dọn dẹp giao dịch nạp tiền PENDING quá hạn → EXPIRED.
 *
 * Giải quyết tình huống user mở tab thanh toán VNPay rồi đóng/đổi ý: giao dịch không treo
 * "Đang xử lý" mãi mà tự hết hạn sau khi vượt TTL (mặc định 15 phút).
 *
 * Chạy mỗi 5 phút. Có thể chỉnh qua biến môi trường wallet.topup.expiry-scan-ms.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TopUpExpiryScheduler {

    private final ExpireStalePendingTopUpsUseCase expireStalePendingTopUpsUseCase;

    @Scheduled(
            fixedDelayString = "${wallet.topup.expiry-scan-ms:300000}",
            initialDelayString = "${wallet.topup.expiry-initial-delay-ms:60000}")
    public void scan() {
        try {
            expireStalePendingTopUpsUseCase.execute();
        } catch (Exception e) {
            // Không để exception làm chết scheduler — log và chờ lượt sau.
            log.error("[TopUpExpiry] Lỗi khi quét giao dịch treo", e);
        }
    }
}
