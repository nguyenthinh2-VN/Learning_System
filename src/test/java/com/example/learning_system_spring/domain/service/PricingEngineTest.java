package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adversarial test cho PricingEngine — kiểm tra correctness properties từ
 * .kiro/specs/voucher-pricing/requirements.md (Section "Correctness Properties").
 *
 * Mục tiêu: phơi bày bug, KHÔNG che dấu. Thử các edge case mà dev có thể bỏ sót.
 */
class PricingEngineTest {

    private final PricingEngine engine = new PricingEngine();

    private Voucher percentVoucher(BigDecimal value, BigDecimal maxDiscount) {
        return Voucher.create("TEST", VoucherType.PERCENT, value,
                VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, maxDiscount, 0L, 0, new HashSet<>());
    }

    private Voucher fixedVoucher(BigDecimal value) {
        return Voucher.create("TEST", VoucherType.FIXED, value,
                VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());
    }

    @Nested
    @DisplayName("Invariants — Property #1, #2, #3, #5, #6")
    class Invariants {

        @Test
        @DisplayName("Property #6: voucher null → discount = 0 và finalPrice = original")
        void nullVoucher_noDiscount() {
            PriceQuote quote = engine.compute(new BigDecimal("500000"), null);
            assertThat(quote.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(quote.finalPrice()).isEqualByComparingTo("500000.00");
            assertThat(quote.voucherApplied()).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
                "0,        50",
                "100,      30",
                "500000,   15",
                "999999.99, 100",
                "1,        50"
        })
        @DisplayName("Property #1+#2+#3: 0 ≤ discount ≤ original AND 0 ≤ final ≤ original")
        void invariantsHold(BigDecimal originalPrice, BigDecimal percentValue) {
            Voucher voucher = percentVoucher(percentValue, BigDecimal.ZERO);
            PriceQuote quote = engine.compute(originalPrice, voucher);

            assertThat(quote.discountAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(quote.discountAmount()).isLessThanOrEqualTo(quote.originalPrice());
            assertThat(quote.finalPrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(quote.finalPrice()).isLessThanOrEqualTo(quote.originalPrice());
            assertThat(quote.originalPrice().subtract(quote.discountAmount()))
                    .isEqualByComparingTo(quote.finalPrice());
        }

        @Test
        @DisplayName("Property #5: idempotence — gọi nhiều lần luôn trả về cùng kết quả")
        void idempotence() {
            BigDecimal price = new BigDecimal("237.55");
            Voucher voucher = percentVoucher(new BigDecimal("33"), new BigDecimal("50"));

            PriceQuote first = engine.compute(price, voucher);
            for (int i = 0; i < 10; i++) {
                PriceQuote next = engine.compute(price, voucher);
                assertThat(next.originalPrice()).isEqualByComparingTo(first.originalPrice());
                assertThat(next.discountAmount()).isEqualByComparingTo(first.discountAmount());
                assertThat(next.finalPrice()).isEqualByComparingTo(first.finalPrice());
            }
        }
    }

    @Nested
    @DisplayName("PERCENT — Property #4 MaxDiscount cap")
    class PercentVoucher {

        @Test
        @DisplayName("PERCENT 50% trên 500000, maxDiscount 200000 → discount = 200000 (bị cap)")
        void maxDiscountCap() {
            Voucher v = percentVoucher(new BigDecimal("50"), new BigDecimal("200000"));
            PriceQuote q = engine.compute(new BigDecimal("500000"), v);
            assertThat(q.discountAmount()).isEqualByComparingTo("200000.00");
            assertThat(q.finalPrice()).isEqualByComparingTo("300000.00");
        }

        @Test
        @DisplayName("PERCENT 50% với maxDiscount = 0 → không bị cap, discount = 50% nguyên")
        void zeroMaxDiscount_meansUnlimited() {
            Voucher v = percentVoucher(new BigDecimal("50"), BigDecimal.ZERO);
            PriceQuote q = engine.compute(new BigDecimal("500000"), v);
            assertThat(q.discountAmount()).isEqualByComparingTo("250000.00");
            assertThat(q.finalPrice()).isEqualByComparingTo("250000.00");
        }

        @Test
        @DisplayName("Bug-D candidate: PERCENT precision-loss → voucherApplied = true nhưng discount = 0")
        void precisionLoss_voucherAppliedButZeroDiscount() {
            // 0.01% trên 100đ = 0.01đ → HALF_UP scale 2 = 0.01đ. OK không 0.
            // Edge: 0.001% trên 100đ = 0.001đ → HALF_UP scale 2 = 0.00đ.
            Voucher v = percentVoucher(new BigDecimal("0.001"), BigDecimal.ZERO);
            PriceQuote q = engine.compute(new BigDecimal("100"), v);

            // Voucher có giá trị nhưng do làm tròn discount = 0
            assertThat(q.discountAmount()).isEqualByComparingTo("0.00");
            // !!! voucherApplied vẫn true, lại không có discount thật → có thể gây nhầm cho UI
            assertThat(q.voucherApplied()).isTrue();
            assertThat(q.finalPrice()).isEqualByComparingTo(q.originalPrice());
        }

        @Test
        @DisplayName("Property #8 Metamorphic: voucher value lớn hơn → finalPrice ≤ voucher value nhỏ hơn")
        void largerDiscountSmallerOrEqualFinalPrice() {
            BigDecimal price = new BigDecimal("1000000");
            Voucher v30 = percentVoucher(new BigDecimal("30"), BigDecimal.ZERO);
            Voucher v60 = percentVoucher(new BigDecimal("60"), BigDecimal.ZERO);
            assertThat(engine.compute(price, v60).finalPrice())
                    .isLessThanOrEqualTo(engine.compute(price, v30).finalPrice());
        }

        @Test
        @DisplayName("Half-up rounding: 33.33% trên 100 = 33.33đ (chính xác)")
        void halfUpRounding() {
            Voucher v = percentVoucher(new BigDecimal("33.33"), BigDecimal.ZERO);
            PriceQuote q = engine.compute(new BigDecimal("100"), v);
            assertThat(q.discountAmount()).isEqualByComparingTo("33.33");
            assertThat(q.finalPrice()).isEqualByComparingTo("66.67");
        }
    }

    @Nested
    @DisplayName("FIXED voucher")
    class FixedVoucher {

        @Test
        @DisplayName("FIXED voucher value > originalPrice → discount cap về originalPrice (final = 0)")
        void fixedDiscountCappedAtOriginalPrice() {
            Voucher v = fixedVoucher(new BigDecimal("999999"));
            PriceQuote q = engine.compute(new BigDecimal("100000"), v);
            assertThat(q.discountAmount()).isEqualByComparingTo("100000.00");
            assertThat(q.finalPrice()).isEqualByComparingTo("0.00");
            assertThat(q.voucherApplied()).isTrue();
        }

        @Test
        @DisplayName("FIXED voucher value = originalPrice → final = 0")
        void fixedExactMatch() {
            Voucher v = fixedVoucher(new BigDecimal("500000"));
            PriceQuote q = engine.compute(new BigDecimal("500000"), v);
            assertThat(q.finalPrice()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Edge cases — Property #7, #9 (Negative input)")
    class EdgeCases {

        @Test
        @DisplayName("Property #7: originalPrice = 0 → mọi voucher → finalPrice = 0, discount = 0")
        void zeroOriginalPrice() {
            PriceQuote q = engine.compute(BigDecimal.ZERO,
                    percentVoucher(new BigDecimal("50"), BigDecimal.ZERO));
            assertThat(q.originalPrice()).isEqualByComparingTo("0.00");
            assertThat(q.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(q.finalPrice()).isEqualByComparingTo("0.00");
            // Theo PricingEngine: originalPrice = 0 → noDiscount (voucher coi như không applied)
            assertThat(q.voucherApplied()).isFalse();
        }

        @Test
        @DisplayName("Property #9: originalPrice âm → IllegalArgumentException")
        void negativeOriginalPriceThrows() {
            assertThatThrownBy(() -> engine.compute(new BigDecimal("-1"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("originalPrice null → IllegalArgumentException")
        void nullOriginalPriceThrows() {
            assertThatThrownBy(() -> engine.compute(null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"100", "12345.67", "999999999.99"})
        @DisplayName("Mọi originalPrice non-negative + voucher null → finalPrice = original, discount = 0")
        void anyPositivePriceWithNullVoucher(String priceStr) {
            BigDecimal price = new BigDecimal(priceStr);
            PriceQuote q = engine.compute(price, null);
            assertThat(q.discountAmount().signum()).isZero();
            assertThat(q.finalPrice()).isEqualByComparingTo(price);
        }
    }

    @Nested
    @DisplayName("PriceQuote value object — invariant enforcement")
    class PriceQuoteInvariants {

        @Test
        @DisplayName("Tự xây PriceQuote với discount > original → reject")
        void rejectInvalidQuote() {
            assertThatThrownBy(() -> new PriceQuote(
                    new BigDecimal("100"),
                    new BigDecimal("200"),
                    new BigDecimal("-100"),
                    true, "X", VoucherType.FIXED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Tự xây PriceQuote với finalPrice âm → reject")
        void rejectNegativeFinal() {
            assertThatThrownBy(() -> new PriceQuote(
                    new BigDecimal("100"),
                    new BigDecimal("50"),
                    new BigDecimal("-1"),
                    false, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
