package com.example.learning_system_spring.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test cho WalletNotificationService (Publisher).
 *
 * Kiểm tra:
 * - convertAndSendToUser được gọi đúng username + destination
 * - Payload chứa đúng các field cần thiết
 * - Không throw khi SimpMessagingTemplate ném exception (best-effort)
 */
@DisplayName("WalletNotificationService — Publisher")
class WalletNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WalletNotificationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("pushWalletUpdated → gọi convertAndSendToUser đúng username và destination")
    void pushToCorrectUser() {
        service.pushWalletUpdated(
                "MEM2B4A1D", 1L,
                new BigDecimal("700000"), new BigDecimal("200000"),
                "ADMIN", "NAPADMIN001", "Bù lỗi #123");

        verify(messagingTemplate, times(1))
                .convertAndSendToUser(
                        eq("MEM2B4A1D"),
                        eq("/queue/wallet"),
                        any(Map.class));
    }

    @Test
    @DisplayName("pushWalletUpdated → payload chứa đúng event=WALLET_UPDATED và các field")
    @SuppressWarnings("unchecked")
    void payloadContainsCorrectFields() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        service.pushWalletUpdated(
                "MEM2B4A1D", 1L,
                new BigDecimal("700000"), new BigDecimal("200000"),
                "ADMIN", "NAPADMIN001", "Bù lỗi #123");

        verify(messagingTemplate).convertAndSendToUser(
                eq("MEM2B4A1D"), eq("/queue/wallet"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload.get("event")).isEqualTo("WALLET_UPDATED");
        assertThat(payload.get("userId")).isEqualTo(1L);
        assertThat(payload.get("newBalance")).isEqualTo(new BigDecimal("700000"));
        assertThat(payload.get("addedAmount")).isEqualTo(new BigDecimal("200000"));
        assertThat(payload.get("source")).isEqualTo("ADMIN");
        assertThat(payload.get("referenceCode")).isEqualTo("NAPADMIN001");
        assertThat(payload.get("note")).isEqualTo("Bù lỗi #123");
        assertThat(payload.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("pushWalletUpdated với source=MOCK → payload source=MOCK")
    @SuppressWarnings("unchecked")
    void mockSourceInPayload() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        service.pushWalletUpdated(
                "MEM2B4A1D", 1L,
                new BigDecimal("500000"), new BigDecimal("500000"),
                "MOCK", "NAP4F8A2C1B3", null);

        verify(messagingTemplate).convertAndSendToUser(
                eq("MEM2B4A1D"), eq("/queue/wallet"), captor.capture());

        assertThat(captor.getValue().get("source")).isEqualTo("MOCK");
        assertThat(captor.getValue().get("note")).isNull();
    }

    @Test
    @DisplayName("SimpMessagingTemplate ném exception → không propagate (best-effort)")
    void doesNotThrowWhenWsFails() {
        doThrow(new RuntimeException("WS broker down"))
                .when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any(Map.class));

        // Không throw — WS push là best-effort
        service.pushWalletUpdated(
                "MEM2B4A1D", 1L,
                BigDecimal.TEN, BigDecimal.TEN,
                "MOCK", "NAP123", null);
    }

    @Test
    @DisplayName("push tới 2 user khác nhau → mỗi user nhận đúng payload của mình")
    @SuppressWarnings("unchecked")
    void pushToDifferentUsers() {
        service.pushWalletUpdated("USER_A", 1L, new BigDecimal("100"), new BigDecimal("100"),
                "ADMIN", "REF_A", "note A");
        service.pushWalletUpdated("USER_B", 2L, new BigDecimal("200"), new BigDecimal("200"),
                "ADMIN", "REF_B", "note B");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(2))
                .convertAndSendToUser(anyString(), eq("/queue/wallet"), captor.capture());

        var payloads = captor.getAllValues();
        assertThat(payloads.get(0).get("referenceCode")).isEqualTo("REF_A");
        assertThat(payloads.get(1).get("referenceCode")).isEqualTo("REF_B");
    }
}
