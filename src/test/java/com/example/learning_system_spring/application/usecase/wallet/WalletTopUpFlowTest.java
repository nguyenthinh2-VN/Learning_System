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

    private User makeUser(BigDecimal balance) {
        return User.reconstitute(
                1L, "MEM2B4A1D", "u@e.com", "pw", "Test User",
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
        BigDecimal initialBalance = new BigDecimal("100000");
        BigDecimal expectedBalance = new BigDecimal("600000");

        // Gateway trả về message hướng dẫn (mock mode)
        when(paymentGateway.providerName()).thenReturn("MOCK");
        when(paymentGateway.initPayment(any(), eq(amount))).thenAnswer(inv -> {
            String ref = inv.getArgument(0);
            return new PaymentInitResult(ref, amount,
                    "Gọi POST /webhook/mock?ref=" + ref, "MESSAGE",
                    LocalDateTime.now().plusMinutes(15));
        });

        // Lưu tx khi init
        when(walletTxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ── Step 1: User gọi init top-up ─────────────────────────
        InitTopUpOutput initOutput = initTopUpUseCase.execute(1L, amount);

        assertThat(initOutput.referenceCode()).startsWith("NAP");
        assertThat(initOutput.displayType()).isEqualTo("MESSAGE");
        assertThat(initOutput.amount()).isEqualByComparingTo("500000");

        String referenceCode = initOutput.referenceCode();

        // ── Step 2: Mock webhook callback ────────────────────────
        // Tìm pending tx theo ref
        WalletTransaction pendingTx = WalletTransaction.createPending(1L, amount, TxSource.MOCK, 15);
        // Dùng reflection để set referenceCode khớp với initOutput
        ReflectionTestUtils.setField(pendingTx, "referenceCode", referenceCode);

        when(walletTxRepo.findPendingByRefForUpdate(referenceCode))
                .thenReturn(Optional.of(pendingTx));

        User user = makeUser(initialBalance);
        User savedUser = makeUser(expectedBalance);
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
        assertThat(payload.get("newBalance")).isEqualToComparingFieldByField("600000");
        assertThat(payload.get("addedAmount")).isEqualToComparingFieldByField("500000");
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
        // ── Arrange ──────────────────────────────────────────────
        User user = makeUser(new BigDecimal("500000"));
        User savedUser = makeUser(new BigDecimal("700000"));
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
        assertThat((BigDecimal) payload.get("newBalance")).isEqualByComparingTo("700000");
        assertThat((BigDecimal) payload.get("addedAmount")).isEqualByComparingTo("200000");
        assertThat(payload.get("note")).isEqualTo("Bù lỗi #123");
    }

    // ══════════════════════════════════════════════════════════════
    // FLOW 3: Gọi mock webhook 2 lần (idempotency)
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FLOW 3 — mock webhook gọi 2 lần cùng ref → lần 2 không push WS")
    void flow3_doubleWebhook_onlyOnePush() {
        String ref = "NAP4F8A2C1B3";
        WalletTransaction pendingTx = WalletTransaction.createPending(1L, new BigDecimal("500000"), TxSource.MOCK, 15);
        ReflectionTestUtils.setField(pendingTx, "referenceCode", ref);

        User user = makeUser(BigDecimal.ZERO);
        User savedUser = makeUser(new BigDecimal("500000"));

        // Lần 1: tìm thấy PENDING
        // Lần 2: không còn PENDING (đã COMPLETED)
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
            // expected
        }

        // Chỉ 1 lần push WS
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
        User userA = User.reconstitute(1L, "USER_A", "a@e.com", "pw", "A",
                Role.reconstitute(1L, "MEMBER", null), false,
                new BigDecimal("100000"), LocalDateTime.now(), LocalDateTime.now());
        User savedA = User.reconstitute(1L, "USER_A", "a@e.com", "pw", "A",
                Role.reconstitute(1L, "MEMBER", null), false,
                new BigDecimal("200000"), LocalDateTime.now(), LocalDateTime.now());

        User userB = User.reconstitute(2L, "USER_B", "b@e.com", "pw", "B",
                Role.reconstitute(1L, "MEMBER", null), false,
                new BigDecimal("50000"), LocalDateTime.now(), LocalDateTime.now());
        User savedB = User.reconstitute(2L, "USER_B", "b@e.com", "pw", "B",
                Role.reconstitute(1L, "MEMBER", null), false,
                new BigDecimal("150000"), LocalDateTime.now(), LocalDateTime.now());

        WalletTransaction txA = WalletTransaction.createCompleted(1L, new BigDecimal("100000"), TxSource.ADMIN, "for A");
        WalletTransaction txB = WalletTransaction.createCompleted(2L, new BigDecimal("100000"), TxSource.ADMIN, "for B");

        when(userRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));
        when(userRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(userB));
        when(userRepo.save(argThat(u -> u.getId().equals(1L)))).thenReturn(savedA);
        when(userRepo.save(argThat(u -> u.getId().equals(2L)))).thenReturn(savedB);
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
        verify(messagingTemplate).convertAndSendToUser(eq("USER_A"), eq("/queue/wallet"), captorA.capture());
        assertThat(captorA.getValue().get("note")).isEqualTo("for A");
        assertThat((BigDecimal) captorA.getValue().get("newBalance")).isEqualByComparingTo("200000");

        // Verify: USER_B nhận event của mình
        ArgumentCaptor<Map<String, Object>> captorB = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(eq("USER_B"), eq("/queue/wallet"), captorB.capture());
        assertThat(captorB.getValue().get("note")).isEqualTo("for B");
        assertThat((BigDecimal) captorB.getValue().get("newBalance")).isEqualByComparingTo("150000");
    }
}
