package com.example.learning_system_spring.domain.model.Voucher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adversarial test cho Voucher domain model.
 * Mục tiêu: phơi bày các kẽ hở validation.
 */
class VoucherTest {

    private final LocalDateTime now = LocalDateTime.now();

    private Voucher.VoucherBuilder baseBuilder() {
        return Voucher.builder()
                .code("TEST")
                .type(VoucherType.PERCENT)
                .value(new BigDecimal("10"))
                .status(VoucherStatus.ACTIVE)
                .scope(VoucherScope.ALL_COURSES)
                .validFrom(now.minusDays(1))
                .validTo(now.plusDays(1))
                .minOrderAmount(BigDecimal.ZERO)
                .maxDiscount(BigDecimal.ZERO)
                .usageLimit(0L)
                .usagePerUser(0)
                .applicableCourseIds(new HashSet<>())
                .createdAt(now)
                .updatedAt(now);
    }

    @Nested
    @DisplayName("normalizeCode")
    class NormalizeCode {

        @Test
        @DisplayName("Lowercase → Uppercase")
        void lowerToUpper() {
            assertThat(Voucher.normalizeCode("welcome50")).isEqualTo("WELCOME50");
        }

        @Test
        @DisplayName("Mixed-case có whitespace → trim + uppercase")
        void trimAndUpper() {
            assertThat(Voucher.normalizeCode("  weLCome  ")).isEqualTo("WELCOME");
        }

        @Test
        @DisplayName("null → null")
        void nullInput() {
            assertThat(Voucher.normalizeCode(null)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "\t", "\n"})
        @DisplayName("Empty / whitespace-only → empty string")
        void blankInput(String input) {
            assertThat(Voucher.normalizeCode(input)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Voucher.create — input validation")
    class CreateValidation {

        @Test
        @DisplayName("code blank → IllegalArgumentException")
        void blankCode() {
            assertThatThrownBy(() -> Voucher.create(
                    "  ", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code");
        }

        @Test
        @DisplayName("validFrom > validTo → reject")
        void invalidDateRange() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    now.plusDays(2), now.plusDays(1),
                    null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validFrom");
        }

        @Test
        @DisplayName("value = 0 → reject")
        void zeroValue() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, BigDecimal.ZERO,
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("value âm → reject")
        void negativeValue() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("-10"),
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SPECIFIC_COURSES nhưng applicableCourseIds rỗng → reject")
        void specificCoursesEmpty() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.SPECIFIC_COURSES, now, now.plusDays(1),
                    null, null, null, null, new HashSet<>()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SPECIFIC_COURSES");
        }

        @Test
        @DisplayName("SPECIFIC_COURSES nhưng applicableCourseIds null → reject")
        void specificCoursesNull() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.SPECIFIC_COURSES, now, now.plusDays(1),
                    null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ALL_COURSES kèm applicableCourseIds non-empty → reject")
        void allCoursesWithIds() {
            assertThatThrownBy(() -> Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, Set.of(1L, 2L)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Bug observation: Voucher.create KHÔNG enforce PERCENT value <= 100")
        void percentOver100_DomainAcceptsButUseCaseShouldReject() {
            // Voucher.create CHỈ check value > 0. Cap 100 cho PERCENT phải nằm ở UseCase.
            // Test phơi bày: domain cho phép percent value = 200 → đó là technical debt
            Voucher v = Voucher.create(
                    "TEST", VoucherType.PERCENT, new BigDecimal("200"),
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, null);
            assertThat(v.getValue()).isEqualByComparingTo("200");
            // → Người gọi Voucher.create trực tiếp (không qua CreateVoucherUseCase) sẽ bypass cap này.
            // Nên đặt invariant trong domain hoặc trong Voucher.create.
        }
    }

    @Nested
    @DisplayName("Voucher code is normalized to uppercase on create")
    class CodeNormalization {

        @Test
        @DisplayName("create với lowercase → lưu uppercase")
        void normalize() {
            Voucher v = Voucher.create(
                    "welcome50", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES, now, now.plusDays(1),
                    null, null, null, null, null);
            assertThat(v.getCode()).isEqualTo("WELCOME50");
        }
    }

    @Nested
    @DisplayName("appliesTo")
    class AppliesTo {

        @Test
        @DisplayName("ALL_COURSES → mọi courseId đều true")
        void allCourses() {
            Voucher v = baseBuilder().scope(VoucherScope.ALL_COURSES).build();
            assertThat(v.appliesTo(1L)).isTrue();
            assertThat(v.appliesTo(99999L)).isTrue();
        }

        @Test
        @DisplayName("SPECIFIC_COURSES → chỉ khớp khi courseId trong set")
        void specific() {
            Voucher v = baseBuilder()
                    .scope(VoucherScope.SPECIFIC_COURSES)
                    .applicableCourseIds(Set.of(1L, 2L))
                    .build();
            assertThat(v.appliesTo(1L)).isTrue();
            assertThat(v.appliesTo(3L)).isFalse();
        }

        @Test
        @DisplayName("Bug-A: scope NULL → fall through, return false (không exception)")
        void nullScopeFallsThrough() {
            // BUG: appliesTo không check scope null. Trả false (không phải NPE).
            // Voucher state corrupted nhưng method không phát hiện.
            Voucher v = baseBuilder().scope(null).build();
            assertThat(v.appliesTo(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("updateSoftFields validation gaps — Bug discovery")
    class UpdateSoftFields {

        @Test
        @DisplayName("Bug-A: updateSoftFields chấp nhận status NULL → voucher rơi vào trạng thái không xác định")
        void allowsNullStatus() {
            Voucher v = baseBuilder().build();
            // Method không validate status → set null thành công
            v.updateSoftFields(null,
                    now.minusDays(1), now.plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    VoucherScope.ALL_COURSES, new HashSet<>());

            // Sau update, isInactive() = false (vì status != INACTIVE),
            // isActive() = false (vì status != ACTIVE)
            // → voucher tồn tại trong "limbo state" và sẽ pass VoucherValidator.
            assertThat(v.getStatus()).isNull();
            assertThat(v.isInactive()).isFalse();
            assertThat(v.isActive()).isFalse();
        }

        @Test
        @DisplayName("Bug-A: updateSoftFields chấp nhận scope NULL khi không phải SPECIFIC_COURSES")
        void allowsNullScope() {
            Voucher v = baseBuilder().build();
            // Code: chỉ check khi scope == SPECIFIC_COURSES yêu cầu non-empty applicable.
            // Nếu scope = null, branch SPECIFIC_COURSES không match → pass.
            // Branch ALL_COURSES check applicable empty → null hoặc empty → pass.
            // → null scope SLIPS THROUGH.
            v.updateSoftFields(VoucherStatus.ACTIVE,
                    now.minusDays(1), now.plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    null, new HashSet<>());

            assertThat(v.getScope()).isNull();
        }

        @Test
        @DisplayName("validFrom > validTo → reject")
        void rejectInvalidDateRange() {
            Voucher v = baseBuilder().build();
            assertThatThrownBy(() -> v.updateSoftFields(VoucherStatus.ACTIVE,
                    now.plusDays(5), now.plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    VoucherScope.ALL_COURSES, new HashSet<>()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SPECIFIC_COURSES kèm applicable rỗng → reject")
        void rejectSpecificEmpty() {
            Voucher v = baseBuilder().build();
            assertThatThrownBy(() -> v.updateSoftFields(VoucherStatus.ACTIVE,
                    now.minusDays(1), now.plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    VoucherScope.SPECIFIC_COURSES, new HashSet<>()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ALL_COURSES kèm applicable non-empty → reject")
        void rejectAllCoursesWithIds() {
            Voucher v = baseBuilder().build();
            assertThatThrownBy(() -> v.updateSoftFields(VoucherStatus.ACTIVE,
                    now.minusDays(1), now.plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    VoucherScope.ALL_COURSES, Set.of(1L, 2L)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deactivate — soft-delete")
    class Deactivate {

        @Test
        @DisplayName("ACTIVE → INACTIVE và update timestamp")
        void deactivate() {
            Voucher v = baseBuilder().build();
            LocalDateTime before = v.getUpdatedAt();
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            v.deactivate();
            assertThat(v.getStatus()).isEqualTo(VoucherStatus.INACTIVE);
            assertThat(v.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Deactivate idempotent — gọi 2 lần vẫn INACTIVE")
        void deactivateIdempotent() {
            Voucher v = baseBuilder().build();
            v.deactivate();
            v.deactivate();
            assertThat(v.getStatus()).isEqualTo(VoucherStatus.INACTIVE);
        }
    }
}
