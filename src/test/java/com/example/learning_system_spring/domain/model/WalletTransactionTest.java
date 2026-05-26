package com.example.learning_system_spring.domain.model;

import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test thuần cho WalletTransaction domain model.
 * Không cần Spring context, không cần DB.
 */
@DisplayName("WalletTransaction domain model")
class WalletTransactionTest {

    // ─────────────────────────────────────────────────────────────
    // createPending
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createPending()")
    class CreatePending {

        @Test
        @DisplayName("tạo thành công → status=PENDING, referenceCode bắt đầu bằng NAP")
        void success() {
            WalletTransaction tx = WalletTransaction.createPending(1L, new BigDecimal("500000"), TxSource.MOCK, 15);

            assertThat(tx.getStatus()).isEqualTo(TxStatus.PENDING);
            assertThat(tx.getReferenceCode()).startsWith("NAP");
            assertThat(tx.getReferenceCode()).hasSize(12); // "NAP" + 9 ký tự
            assertThat(tx.getAmount()).isEqualByComparingTo("500000");
            assertThat(tx.getSource()).isEqualTo(TxSource.MOCK);
            assertThat(tx.getCompletedAt()).isNull();
            assertThat(tx.getExpiredAt()).isNotNull();
        }

        @Test
        @DisplayName("referenceCode mỗi lần tạo là duy nhất")
        void referenceCodeUnique() {
            WalletTransaction tx1 = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            WalletTransaction tx2 = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);

            assertThat(tx1.getReferenceCode()).isNotEqualTo(tx2.getReferenceCode());
        }

        @Test
        @DisplayName("amount = 0 → IllegalArgumentException")
        void zeroAmountRejected() {
            assertThatThrownBy(() ->
                    WalletTransaction.createPending(1L, BigDecimal.ZERO, TxSource.MOCK, 15))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("amount âm → IllegalArgumentException")
        void negativeAmountRejected() {
            assertThatThrownBy(() ->
                    WalletTransaction.createPending(1L, new BigDecimal("-1"), TxSource.MOCK, 15))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("amount null → IllegalArgumentException")
        void nullAmountRejected() {
            assertThatThrownBy(() ->
                    WalletTransaction.createPending(1L, null, TxSource.MOCK, 15))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("expiredAt = now + ttlMinutes")
        void expiredAtSetCorrectly() {
            var before = java.time.LocalDateTime.now().plusMinutes(14).plusSeconds(59);
            WalletTransaction tx = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            var after = java.time.LocalDateTime.now().plusMinutes(15).plusSeconds(1);

            assertThat(tx.getExpiredAt()).isAfter(before).isBefore(after);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createCompleted (admin)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createCompleted()")
    class CreateCompleted {

        @Test
        @DisplayName("tạo thành công → status=COMPLETED, completedAt không null")
        void success() {
            WalletTransaction tx = WalletTransaction.createCompleted(
                    5L, new BigDecimal("200000"), TxSource.ADMIN, "Bù lỗi #123");

            assertThat(tx.getStatus()).isEqualTo(TxStatus.COMPLETED);
            assertThat(tx.getCompletedAt()).isNotNull();
            assertThat(tx.getNote()).isEqualTo("Bù lỗi #123");
            assertThat(tx.getSource()).isEqualTo(TxSource.ADMIN);
        }

        @Test
        @DisplayName("amount = 0 → IllegalArgumentException")
        void zeroRejected() {
            assertThatThrownBy(() ->
                    WalletTransaction.createCompleted(1L, BigDecimal.ZERO, TxSource.ADMIN, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // complete()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("PENDING → complete() → status=COMPLETED, completedAt set")
        void pendingToCompleted() {
            WalletTransaction tx = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            tx.complete("bank-tx-001");

            assertThat(tx.getStatus()).isEqualTo(TxStatus.COMPLETED);
            assertThat(tx.getCompletedAt()).isNotNull();
            assertThat(tx.getNote()).isEqualTo("bank-tx-001");
        }

        @Test
        @DisplayName("COMPLETED → complete() lần 2 → IllegalStateException")
        void doubleCompleteRejected() {
            WalletTransaction tx = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            tx.complete("first");

            assertThatThrownBy(() -> tx.complete("second"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("isPending() = true khi PENDING và chưa hết hạn")
        void isPendingTrue() {
            WalletTransaction tx = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            assertThat(tx.isPending()).isTrue();
        }

        @Test
        @DisplayName("isPending() = false sau khi complete()")
        void isPendingFalseAfterComplete() {
            WalletTransaction tx = WalletTransaction.createPending(1L, BigDecimal.TEN, TxSource.MOCK, 15);
            tx.complete("done");
            assertThat(tx.isPending()).isFalse();
        }
    }
}
