package com.example.learning_system_spring.application.usecase.wallet;

import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.CompleteTopUpUseCase;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho CompleteTopUpUseCase.
 *
 * Đây là use case dùng chung cho Mock và VietQR webhook.
 * Kiểm tra: cộng tiền đúng, trả về đúng Result để push WebSocket.
 */
@DisplayName("CompleteTopUpUseCase")
class CompleteTopUpUseCaseTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompleteTopUpUseCase useCase;

    private static final String REF = "NAP4F8A2C1B3";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User makeUser(BigDecimal balance) {
        return User.reconstitute(
                1L, "MEM2B4A1D", "u@e.com", "pw", "Test User",
                Role.reconstitute(1L, "MEMBER", null),
                false, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    private WalletTransaction makePendingTx(BigDecimal amount) {
        return WalletTransaction.createPending(1L, amount, TxSource.MOCK, 15);
    }

    @Test
    @DisplayName("happy path → cộng tiền đúng, trả Result với newBalance và username")
    void happyPath() {
        WalletTransaction tx = makePendingTx(new BigDecimal("500000"));
        User user = makeUser(new BigDecimal("100000"));
        User savedUser = makeUser(new BigDecimal("600000")); // sau khi cộng

        when(walletTransactionRepository.findPendingByRefForUpdate(REF))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.save(any())).thenReturn(tx);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);

        CompleteTopUpUseCase.Result result = useCase.execute(REF, "bank-tx-001");

        assertThat(result.username()).isEqualTo("MEM2B4A1D");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.newBalance()).isEqualByComparingTo("600000");
        assertThat(result.addedAmount()).isEqualByComparingTo("500000");
        assertThat(result.source()).isEqualTo("MOCK");
        assertThat(result.referenceCode()).isEqualTo(REF);
        assertThat(result.note()).isEqualTo("bank-tx-001");
    }

    @Test
    @DisplayName("referenceCode không tồn tại → IllegalStateException")
    void refNotFound() {
        when(walletTransactionRepository.findPendingByRefForUpdate(REF))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(REF, "note"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(REF);
    }

    @Test
    @DisplayName("gọi 2 lần cùng ref → lần 2 không tìm thấy PENDING → IllegalStateException (idempotent)")
    void doubleCallIdempotent() {
        WalletTransaction tx = makePendingTx(new BigDecimal("500000"));
        User user = makeUser(new BigDecimal("100000"));
        User savedUser = makeUser(new BigDecimal("600000"));

        // Lần 1: tìm thấy
        when(walletTransactionRepository.findPendingByRefForUpdate(REF))
                .thenReturn(Optional.of(tx))
                .thenReturn(Optional.empty()); // Lần 2: không còn PENDING
        when(walletTransactionRepository.save(any())).thenReturn(tx);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);

        // Lần 1 thành công
        useCase.execute(REF, "first");

        // Lần 2 ném exception
        assertThatThrownBy(() -> useCase.execute(REF, "second"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sau khi complete → walletTransactionRepository.save() được gọi với tx đã COMPLETED")
    void txSavedAsCompleted() {
        WalletTransaction tx = makePendingTx(new BigDecimal("500000"));
        User user = makeUser(BigDecimal.ZERO);
        User savedUser = makeUser(new BigDecimal("500000"));

        when(walletTransactionRepository.findPendingByRefForUpdate(REF))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.save(any())).thenReturn(tx);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);

        useCase.execute(REF, "done");

        // Verify tx.complete() đã được gọi → status = COMPLETED
        verify(walletTransactionRepository, times(1)).save(argThat(t ->
                t.getStatus().name().equals("COMPLETED")
        ));
    }

    @Test
    @DisplayName("userRepository.save() được gọi với balance đã tăng")
    void userSavedWithIncreasedBalance() {
        WalletTransaction tx = makePendingTx(new BigDecimal("300000"));
        User user = makeUser(new BigDecimal("200000"));
        User savedUser = makeUser(new BigDecimal("500000"));

        when(walletTransactionRepository.findPendingByRefForUpdate(REF))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.save(any())).thenReturn(tx);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);

        useCase.execute(REF, "done");

        verify(userRepository, times(1)).save(argThat(u ->
                u.getBalance().compareTo(new BigDecimal("500000")) == 0
        ));
    }
}
