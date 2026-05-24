package com.example.learning_system_spring.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adversarial test cho User wallet operations — boundary và edge tài chính.
 */
class UserBalanceTest {

    private User user(BigDecimal balance) {
        return User.reconstitute(
                1L, "u", "u@e.com", "p", "n",
                Role.reconstitute(1L, "MEMBER", null),
                false, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("addBalance số dương → balance tăng đúng")
    void addPositive() {
        User u = user(new BigDecimal("100"));
        u.addBalance(new BigDecimal("50"));
        assertThat(u.getBalance()).isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("addBalance = 0 → reject (phải > 0)")
    void addZeroRejected() {
        User u = user(new BigDecimal("100"));
        assertThatThrownBy(() -> u.addBalance(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addBalance âm → reject")
    void addNegativeRejected() {
        User u = user(new BigDecimal("100"));
        assertThatThrownBy(() -> u.addBalance(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addBalance null → reject")
    void addNullRejected() {
        User u = user(new BigDecimal("100"));
        assertThatThrownBy(() -> u.addBalance(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deductBalance trừ đúng số")
    void deductOk() {
        User u = user(new BigDecimal("100"));
        u.deductBalance(new BigDecimal("30"));
        assertThat(u.getBalance()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("deductBalance trừ vừa đủ balance → balance = 0")
    void deductExact() {
        User u = user(new BigDecimal("100"));
        u.deductBalance(new BigDecimal("100"));
        assertThat(u.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("deductBalance vượt balance → IllegalStateException Insufficient")
    void deductOverflow() {
        User u = user(new BigDecimal("100"));
        assertThatThrownBy(() -> u.deductBalance(new BigDecimal("100.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("deductBalance amount = 0 → cho phép, balance không đổi")
    void deductZeroAllowed() {
        User u = user(new BigDecimal("100"));
        u.deductBalance(BigDecimal.ZERO);
        assertThat(u.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("deductBalance âm → IllegalArgumentException")
    void deductNegativeRejected() {
        User u = user(new BigDecimal("100"));
        assertThatThrownBy(() -> u.deductBalance(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("balance ZERO + deduct dương nhỏ → Insufficient")
    void deductFromZero() {
        User u = user(BigDecimal.ZERO);
        assertThatThrownBy(() -> u.deductBalance(new BigDecimal("0.01")))
                .isInstanceOf(IllegalStateException.class);
    }
}
