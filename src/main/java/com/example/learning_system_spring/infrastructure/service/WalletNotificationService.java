package com.example.learning_system_spring.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service push WebSocket event tới FE khi ví user được cập nhật.
 *
 * FE subscribe: /user/queue/wallet
 * BE push tới: username (= JWT subject = username của user)
 *
 * Spring STOMP tự route đúng user nhờ Principal được set trong WebSocketConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push event WALLET_UPDATED tới đúng user.
     *
     * @param username      JWT subject (username của user, ví dụ: "MEM2B4A1D")
     * @param userId        ID của user
     * @param newBalance    Số dư mới sau khi cộng tiền
     * @param addedAmount   Số tiền vừa được cộng
     * @param source        Nguồn: "MOCK", "VIETQR", hoặc "ADMIN"
     * @param referenceCode Mã tham chiếu giao dịch
     * @param note          Ghi chú (có thể null)
     */
    public void pushWalletUpdated(String username, Long userId, BigDecimal newBalance,
                                   BigDecimal addedAmount, String source,
                                   String referenceCode, String note) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "WALLET_UPDATED");
        payload.put("userId", userId);
        payload.put("newBalance", newBalance);
        payload.put("addedAmount", addedAmount);
        payload.put("source", source);
        payload.put("referenceCode", referenceCode);
        payload.put("note", note);
        payload.put("timestamp", LocalDateTime.now().toString());

        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/wallet", payload);
            log.info("[WS] Pushed WALLET_UPDATED to user={} amount={} source={}",
                    username, addedAmount, source);
        } catch (Exception e) {
            // Không throw — WebSocket push là best-effort, không ảnh hưởng transaction chính
            log.warn("[WS] Failed to push WALLET_UPDATED to user={}: {}", username, e.getMessage());
        }
    }
}
