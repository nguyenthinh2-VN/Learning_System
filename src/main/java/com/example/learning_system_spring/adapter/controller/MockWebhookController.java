package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.application.usecase.Wallet.CompleteTopUpUseCase;
import com.example.learning_system_spring.infrastructure.service.WalletNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock webhook — chỉ active khi payment.provider=mock.
 * Dùng để test toàn bộ flow (pending → completed → WebSocket push) mà không cần ngân hàng thật.
 *
 * Khi ghép VietQR: tạo VietQrWebhookController mới, controller này tự động bị disable
 * bằng cách đổi payment.provider=vietqr trong application.properties.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockWebhookController {

    private final CompleteTopUpUseCase completeTopUpUseCase;
    private final WalletNotificationService walletNotificationService;

    /**
     * Giả lập thanh toán thành công.
     *
     * @param ref referenceCode từ /wallet/top-up/init (ví dụ: NAP4F8A2C1B3)
     */
    @PostMapping("/mock")
    public ResponseEntity<Map<String, Object>> mockCallback(@RequestParam("ref") String ref) {
        log.info("[MockWebhook] Received callback for ref={}", ref);

        try {
            CompleteTopUpUseCase.Result result = completeTopUpUseCase.execute(ref, "mock-payment");

            // Push WebSocket event tới FE của user
            walletNotificationService.pushWalletUpdated(
                    result.username(),
                    result.userId(),
                    result.newBalance(),
                    result.addedAmount(),
                    result.source(),
                    result.referenceCode(),
                    result.note()
            );

            return ResponseEntity.ok(Map.of(
                    "received", true,
                    "message", "Đã cộng tiền thành công (mock)",
                    "newBalance", result.newBalance()
            ));
        } catch (IllegalStateException e) {
            log.warn("[MockWebhook] Failed for ref={}: {}", ref, e.getMessage());
            // Trả 200 để tránh retry vô hạn — log lỗi nội bộ
            return ResponseEntity.ok(Map.of(
                    "received", true,
                    "message", e.getMessage()
            ));
        }
    }
}
