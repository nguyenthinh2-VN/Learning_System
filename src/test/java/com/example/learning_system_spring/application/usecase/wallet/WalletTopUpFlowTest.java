package com.example.learning_system_spring.application.usecase.wallet;

import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.AdminTopUpUseCase;
import com.example.learning_system_spring.application.usecase.Wallet.CompleteTopUpUseCase;
import com.example.learning_system_spring.application.usecase.Wallet.InitTopUpUseCase;
import com.example.learning_system_spring.application.dto.Wallet.AdminTopUpOutput;
import com.example.learning_system_spring.application.dto.Wallet.InitTopUpOutput;
import com.example.learning_system_spring.application.port.PaymentGateway;
import com.example.learning_system_spring.application.port.PaymentInitResult;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import com.example.learning_system_spring.infrastructure.service.WalletNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration flow test: Publisher → Subscriber (WebSocket).
 *
 * Test toàn bộ luồng:
 *   1. InitTopUpUseCase tạo pending transaction
 *   2. CompleteTopUpUseCase (mock webhook) cộng tiền
 *   3. WalletNotificationService push WALLET_UPDATED tới đúng user (subscriber)
 *
 * Không cần Spring context — wire thủ công để test nhanh.
 */
@DisplayName("Wallet Top-up Flow — Publisher → Subscriber")
class WalletTopUpFlowTest {

    // ── Mocks ──────────────────────────────────────────────────────
    @Mock private WalletTransactionRepository walletTxRepo;
    @Mock private UserRepository userRepo;
    @Mock private PaymentGateway paymentGateway;
    @Mock private SimpMessagingTemplate messagingTemplate;  // ← Subscriber nhận từ đây

    // ── Services được wire thủ công ────────────────────────────────
    private InitTopUpUseCase initTopUpUseCase;
    private CompleteTopUpUseCase completeTopUpUseCase;
    private AdminTopUpUseCase adminTopUpUseCase;
    private WalletNotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        notificationService = new WalletNotificationService(messagingTemplate);
        initTopUpUseCase = new InitTopUpUseCase(walletTxRepo, paymentGateway);
        completeTopUpUseCase = new CompleteTopUpUseCase(walletTxRepo, userRepo);
        adminTopUpUseCase = new AdminTopUpUseCase(userRepo, walletTxRepo);

        // Inject @Value fields
        ReflectionTestUtils.setField(initTopUpUseCase, "minAmount", new BigDecimal("10000"));
        ReflectionTestUtils.setField(initTopUpUseCase, "ttlMinutes", 15);
    }

    private User makeUser(Long id, String username, BigDecimal balance) {
        return User.reconstitute(
                id, username, username + "@e.com", "pw", "Test User",
                Role.reconstitute(1L, "MEMBER", null),
                false, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════
    // FLOW 1: User tự nạp qua Mock Gateway
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FLOW 1 — init → mock webhook → subscriber nhận WALLET_UPDATED")
    @SuppressWarnings("unchecked")
    void flow1_initThenMockWebhook_subscriberReceivesEvent() {
        // ── Arrange ──────────────────────────────────────────────
        BigDecimal amount = new BigDecimal("500000");
        BigDecimal expectedBalance = new BigDecimal("600000");

        when(paymentGateway.providerName()).thenReturn("MOCK");
        when(paymentGateway.initPayment(any(), eq(amount))).thenAnswer(inv -> {
            String ref = inv.getArgument(0);
            return new PaymentInitResult(ref, amount,
                    "Gọi POST /webhook/mock?ref=" + ref, "MESSAGE",
                    LocalDateTime.now().plusMinutes(15));
        });
        when(walletTxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ── Step 1: User gọi init top-up ─────────────────────────
        InitTopUpOutput initOutput = initTopUpUseCase.execute(1L, amount);

        assertThat(initOutput.referenceCode()).startsWith("NAP");
        assertThat(initOutput.displayType()).isEqualTo("MESSAGE");

        String referenceCode = initOutput.referenceCode();

        // ── Step 2: Mock webhook callback ────────────────────────
        WalletTransaction pendingTx = WalletTransaction.createPending(1L, amount, TxSource.MOCK, 15);
        ReflectionTestUtils.setField(pendingTx, "referenceCode", referenceCode);

        when(walletTxRepo.findPendingByRefForUpdate(referenceCode))
                .thenReturn(Optional.of(pendingTx));

        User user = makeUser(1L, "MEM2B4A1D", new BigDecimal("100000"));
        User savedUser = makeUser(1L, "MEM2B4A1D", expectedBalance);
        when(userRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenReturn(savedUser);

        CompleteTopUpUseCase.Result result = completeTopUpUseCase.execute(referenceCode, "mock-payment");

        // ── Step 3: Push WebSocket (Publisher → Subscriber) ──────
        notificationService.pushWalletUpdated(
                result.username(), result.userId(),
                result.newBalance(), result.addedAmount(),
                result.source(), result.referenceCode(), result.note());

        // ── Assert: Subscriber nhận đúng event ───────────────────
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq("MEM2B4A1D"), eq("/queue/wallet"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload.get("event")).isEqualTo("WALLET_UPDATED");
        assertThat(payload.get("userId")).isEqualTo(1L);
        // So sánh BigDecimal bằng compareTo để tránh scale mismatch
        assertThat(((BigDecimal) payload.get("newBalance"))
                .compareTo(new BigDecimal("600000"))).isZero();
        assertThat(((BigDecimal) payload.get("addedAmount"))
                .compareTo(new BigDecimal("500000"))).isZero();
        assertThat(payload.get("source")).isEqualTo("MOCK");
        assertThat(payload.get("referenceCode")).isEqualTo(referenceCode);
        assertThat(payload.get("timestamp")).isNotNull();
    }

    // ══════════════════════════════════════════════════════════════
    // FLOW 2: Admin cộng tiền thủ công
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FLOW 2 — admin top-up → subscriber nhận WALLET_UPDATED với source=ADMIN")
    @SuppressWarnings("unchecked")
    void flow2_adminTopUp_subscriberReceivesEvent() {
        User user = makeUser(1L, "MEM2B4A1D", new BigDecimal("500000"));
        User savedUser = makeUser(1L, "MEM2B4A1D", new BigDecimal("700000"));
        WalletTransaction tx = WalletTransaction.createCompleted(
                1L, new BigDecimal("200000"), TxSource.ADMIN, "Bù lỗi #123");

        when(userRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenReturn(savedUser);
        when(walletTxRepo.save(any())).thenReturn(tx);

        // ── Step 1: Admin gọi top-up ──────────────────────────────
        AdminTopUpOutput output = adminTopUpUseCase.execute(1L, new BigDecimal("200000"), "Bù lỗi #123");

        // ── Step 2: Push WebSocket ────────────────────────────────
        notificationService.pushWalletUpdated(
                output.username(), output.userId(),
                output.newBalance(), output.addedAmount(),
                "ADMIN", output.referenceCode(), output.note());

        // ── Assert: Subscriber nhận đúng event ───────────────────
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq("MEM2B4A1D"), eq("/queue/wallet"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload.get("event")).isEqualTo("WALLET_UPDATED");
        assertThat(payload.get("source")).isEqualTo("ADMIN");
        assertThat(((BigDecimal) payload.get("newBalance"))
                .compareTo(new BigDecimal("700000"))).isZero();
        assertThat(((BigDecimal) payload.get("addedAmount"))
                .compareTo(new BigDecimal("200000"))).isZero();
        assertThat(payload.get("note")).isEqualTo("Bù lỗi #123");
    }

    // ══════════════════════════════════════════════════════════════
    // FLOW 3: Gọi mock webhook 2 lần (idempotency)
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FLOW 3 — mock webhook gọi 2 lần cùng ref → lần 2 không push WS")
    void flow3_doubleWebhook_onlyOnePush() {
        String ref = "NAP4F8A2C1B3";
        WalletTransaction pendingTx = WalletTransaction.createPending(
                1L, new BigDecimal("500000"), TxSource.MOCK, 15);
        ReflectionTestUtils.setField(pendingTx, "referenceCode", ref);

        User user = makeUser(1L, "MEM2B4A1D", BigDecimal.ZERO);
        User savedUser = makeUser(1L, "MEM2B4A1D", new BigDecimal("500000"));

        // Lần 1: tìm thấy PENDING — lần 2: không còn PENDING
        when(walletTxRepo.findPendingByRefForUpdate(ref))
                .thenReturn(Optional.of(pendingTx))
                .thenReturn(Optional.empty());
        when(walletTxRepo.save(any())).thenReturn(pendingTx);
        when(userRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenReturn(savedUser);

        // Lần 1: thành công → push WS
        CompleteTopUpUseCase.Result result = completeTopUpUseCase.execute(ref, "first");
        notificationService.pushWalletUpdated(
                result.username(), result.userId(), result.newBalance(),
                result.addedAmount(), result.source(), result.referenceCode(), result.note());

        // Lần 2: exception → không push WS
        try {
            completeTopUpUseCase.execute(ref, "second");
        } catch (IllegalStateException ignored) {
            // expected — không push WS ở đây
        }

        // Chỉ đúng 1 lần push WS
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(anyString(), eq("/queue/wallet"), any(Map.class));
    }

    // ══════════════════════════════════════════════════════════════
    // FLOW 4: 2 user khác nhau nhận đúng event của mình
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FLOW 4 — admin cộng tiền 2 user → mỗi user nhận đúng event của mình")
    @SuppressWarnings("unchecked")
    void flow4_twoUsers_eachReceivesOwnEvent() {
        User userA = makeUser(1L, "USER_A", new BigDecimal("100000"));
        User savedA = makeUser(1L, "USER_A", new BigDecimal("200000"));
        User userB = makeUser(2L, "USER_B", new BigDecimal("50000"));
        User savedB = makeUser(2L, "USER_B", new BigDecimal("150000"));

        // Dùng thenAnswer thay vì argThat để tránh NPE khi Mockito probe matcher
        when(userRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));
        when(userRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(userB));

        // save: trả savedA khi user có id=1, savedB khi id=2
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u.getId().equals(1L) ? savedA : savedB;
        });
        when(walletTxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Admin cộng tiền cho cả 2
        AdminTopUpOutput outA = adminTopUpUseCase.execute(1L, new BigDecimal("100000"), "for A");
        notificationService.pushWalletUpdated(outA.username(), outA.userId(),
                outA.newBalance(), outA.addedAmount(), "ADMIN", outA.referenceCode(), outA.note());

        AdminTopUpOutput outB = adminTopUpUseCase.execute(2L, new BigDecimal("100000"), "for B");
        notificationService.pushWalletUpdated(outB.username(), outB.userId(),
                outB.newBalance(), outB.addedAmount(), "ADMIN", outB.referenceCode(), outB.note());

        // Verify: USER_A nhận event của mình
        ArgumentCaptor<Map<String, Object>> captorA = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("USER_A"), eq("/queue/wallet"), captorA.capture());
        assertThat(captorA.getValue().get("note")).isEqualTo("for A");
        assertThat(((BigDecimal) captorA.getValue().get("newBalance"))
                .compareTo(new BigDecimal("200000"))).isZero();

        // Verify: USER_B nhận event của mình
        ArgumentCaptor<Map<String, Object>> captorB = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("USER_B"), eq("/queue/wallet"), captorB.capture());
        assertThat(captorB.getValue().get("note")).isEqualTo("for B");
        assertThat(((BigDecimal) captorB.getValue().get("newBalance"))
                .compareTo(new BigDecimal("150000"))).isZero();
    }
}
