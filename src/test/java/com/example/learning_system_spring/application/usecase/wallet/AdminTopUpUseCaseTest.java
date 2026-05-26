package com.example.learning_system_spring.application.usecase.wallet;

import com.example.learning_system_spring.application.dto.Wallet.AdminTopUpOutput;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.application.usecase.Wallet.AdminTopUpUseCase;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
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
 * Unit test cho AdminTopUpUseCase.
 * Kiểm tra: cộng tiền trực tiếp, tạo audit record COMPLETED, trả đúng output.
 */
@DisplayName("AdminTopUpUseCase")
class AdminTopUpUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private AdminTopUpUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User makeUser(Long id, String username, BigDecimal balance) {
        return User.reconstitute(
                id, username, username + "@e.com", "pw", "Test",
                Role.reconstitute(1L, "MEMBER", null),
                false, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    private WalletTransaction makeCompletedTx() {
        return WalletTransaction.createCompleted(1L, new BigDecimal("200000"), TxSource.ADMIN, "note");
    }

    @Test
    @DisplayName("happy path → output chứa đúng userId, username, addedAmount, newBalance")
    void happyPath() {
        User user = makeUser(5L, "MEM2B4A1D", new BigDecimal("500000"));
        User savedUser = makeUser(5L, "MEM2B4A1D", new BigDecimal("700000"));
        WalletTransaction tx = makeCompletedTx();

        when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);
        when(walletTransactionRepository.save(any())).thenReturn(tx);

        AdminTopUpOutput output = useCase.execute(5L, new BigDecimal("200000"), "Bù lỗi #123");

        assertThat(output.userId()).isEqualTo(5L);
        assertThat(output.username()).isEqualTo("MEM2B4A1D");
        assertThat(output.addedAmount()).isEqualByComparingTo("200000");
        assertThat(output.newBalance()).isEqualByComparingTo("700000");
        assertThat(output.note()).isEqualTo("Bù lỗi #123");
        assertThat(output.referenceCode()).isNotNull();
    }

    @Test
    @DisplayName("user không tồn tại → UserNotFoundException")
    void userNotFound() {
        when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, new BigDecimal("100000"), "note"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("amount = 0 → IllegalArgumentException")
    void zeroAmountRejected() {
        assertThatThrownBy(() -> useCase.execute(1L, BigDecimal.ZERO, "note"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("amount âm → IllegalArgumentException")
    void negativeAmountRejected() {
        assertThatThrownBy(() -> useCase.execute(1L, new BigDecimal("-1"), "note"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("walletTransactionRepository.save() được gọi với source=ADMIN và status=COMPLETED")
    void auditRecordCreated() {
        User user = makeUser(1L, "MEM", BigDecimal.ZERO);
        User savedUser = makeUser(1L, "MEM", new BigDecimal("100000"));

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);
        when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(1L, new BigDecimal("100000"), "test note");

        verify(walletTransactionRepository, times(1)).save(argThat(tx ->
                tx.getSource() == TxSource.ADMIN &&
                tx.getStatus() == TxStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("note null → vẫn thành công (note là optional)")
    void nullNoteAllowed() {
        User user = makeUser(1L, "MEM", BigDecimal.ZERO);
        User savedUser = makeUser(1L, "MEM", new BigDecimal("50000"));
        WalletTransaction tx = makeCompletedTx();

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(savedUser);
        when(walletTransactionRepository.save(any())).thenReturn(tx);

        AdminTopUpOutput output = useCase.execute(1L, new BigDecimal("50000"), null);
        assertThat(output).isNotNull();
    }
}
