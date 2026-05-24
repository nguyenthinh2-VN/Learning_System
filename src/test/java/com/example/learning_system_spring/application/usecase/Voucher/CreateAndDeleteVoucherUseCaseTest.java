package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.CreateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.DeleteVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherCodeAlreadyExistsException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adversarial test cho CreateVoucherUseCase và DeleteVoucherUseCase.
 *
 * Tập trung non-CRUD edge cases:
 *   - Authorization (role check + null role NPE bug-C)
 *   - Validation: percent > 100, duplicate code, scope/applicable mismatch
 *   - Code normalization
 *   - Delete = soft-delete (set INACTIVE), không xóa cứng
 */
@ExtendWith(MockitoExtension.class)
class CreateAndDeleteVoucherUseCaseTest {

    @Mock private VoucherRepository voucherRepository;

    @InjectMocks
    private CreateVoucherUseCase createUseCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);
    private final Role staffRole = Role.reconstitute(3L, "STAFF", null);
    private final Role superAdminRole = Role.reconstitute(5L, "SUPER_ADMIN", null);

    private CreateVoucherInput baseInput(BigDecimal value, VoucherType type, Set<Long> applicable, VoucherScope scope) {
        return new CreateVoucherInput(
                5L, staffRole, "TEST", type, value,
                scope,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, applicable);
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — authorization")
    class CreateAuthorization {

        @Test
        @DisplayName("MEMBER → CourseAccessDenied")
        void memberDenied() {
            CreateVoucherInput input = new CreateVoucherInput(
                    5L, memberRole, "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    null, null, null, null, null);

            assertThatThrownBy(() -> createUseCase.execute(input))
                    .isInstanceOf(CourseAccessDeniedException.class);
        }

        @Test
        @DisplayName("STAFF → cho phép")
        void staffAllowed() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> {
                Voucher v = inv.getArgument(0);
                return Voucher.reconstitute(99L, v.getCode(), v.getType(), v.getValue(),
                        v.getStatus(), v.getScope(), v.getValidFrom(), v.getValidTo(),
                        v.getMinOrderAmount(), v.getMaxDiscount(),
                        v.getUsageLimit(), v.getUsagePerUser(),
                        v.getApplicableCourseIds(), v.getCreatedAt(), v.getUpdatedAt());
            });

            VoucherOutput out = createUseCase.execute(baseInput(
                    new BigDecimal("10"), VoucherType.PERCENT, new HashSet<>(), VoucherScope.ALL_COURSES));

            assertThat(out.id()).isEqualTo(99L);
            assertThat(out.status()).isEqualTo(VoucherStatus.ACTIVE);
        }

        @Test
        @DisplayName("SUPER_ADMIN → cho phép")
        void superAdminAllowed() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateVoucherInput input = new CreateVoucherInput(
                    5L, superAdminRole, "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    null, null, null, null, null);

            createUseCase.execute(input);
        }

        @Test
        @DisplayName("Bug-C: role null → NullPointerException (escapes to controller)")
        void nullRoleNpe() {
            CreateVoucherInput input = new CreateVoucherInput(
                    5L, null, "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    null, null, null, null, null);

            assertThatThrownBy(() -> createUseCase.execute(input))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — value validation")
    class ValueValidation {

        @Test
        @DisplayName("PERCENT value = 100 → boundary OK")
        void percent100Boundary() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoucherOutput out = createUseCase.execute(baseInput(
                    new BigDecimal("100"), VoucherType.PERCENT, new HashSet<>(), VoucherScope.ALL_COURSES));

            assertThat(out.value()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("PERCENT value = 100.01 → IllegalArgumentException")
        void percentOver100() {
            assertThatThrownBy(() -> createUseCase.execute(baseInput(
                    new BigDecimal("100.01"), VoucherType.PERCENT, new HashSet<>(), VoucherScope.ALL_COURSES)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("FIXED value = 999999999 → cho phép (không có cap ở use case)")
        void fixedHugeValueAllowed() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Bug observation: spec mục 1.4 ghi "value <= cấu hình voucher.fixed.max-value
            // mặc định 100,000,000". Hiện tại code KHÔNG check cap này.
            // Test phơi bày: tạo voucher FIXED 999,999,999 vẫn pass.
            createUseCase.execute(baseInput(
                    new BigDecimal("999999999"), VoucherType.FIXED, new HashSet<>(), VoucherScope.ALL_COURSES));
        }

        @Test
        @DisplayName("Value = 0 → reject (Voucher.create check value > 0)")
        void valueZero() {
            assertThatThrownBy(() -> createUseCase.execute(baseInput(
                    BigDecimal.ZERO, VoucherType.PERCENT, new HashSet<>(), VoucherScope.ALL_COURSES)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — duplicate code")
    class DuplicateCode {

        @Test
        @DisplayName("Code đã tồn tại (sau normalize) → VoucherCodeAlreadyExistsException")
        void duplicate() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(true);

            assertThatThrownBy(() -> createUseCase.execute(baseInput(
                    new BigDecimal("10"), VoucherType.PERCENT, new HashSet<>(), VoucherScope.ALL_COURSES)))
                    .isInstanceOf(VoucherCodeAlreadyExistsException.class);

            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("Code lowercase, server normalize uppercase trước khi check existsByCode")
        void duplicateAfterNormalize() {
            when(voucherRepository.existsByCode("WELCOME50")).thenReturn(true);

            CreateVoucherInput input = new CreateVoucherInput(
                    5L, staffRole, "welcome50",  // lowercase
                    VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    null, null, null, null, null);

            assertThatThrownBy(() -> createUseCase.execute(input))
                    .isInstanceOf(VoucherCodeAlreadyExistsException.class);

            // Verify gọi với UPPERCASE
            verify(voucherRepository).existsByCode("WELCOME50");
        }
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — scope validation")
    class ScopeValidation {

        @Test
        @DisplayName("SPECIFIC_COURSES kèm rỗng → reject")
        void specificEmpty() {
            assertThatThrownBy(() -> createUseCase.execute(baseInput(
                    new BigDecimal("10"), VoucherType.PERCENT, new HashSet<>(), VoucherScope.SPECIFIC_COURSES)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ALL_COURSES kèm courseIds non-empty → reject")
        void allCoursesWithIds() {
            assertThatThrownBy(() -> createUseCase.execute(baseInput(
                    new BigDecimal("10"), VoucherType.PERCENT, Set.of(1L, 2L), VoucherScope.ALL_COURSES)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("SPECIFIC_COURSES + courseIds non-empty → OK")
        void specificWithIds() {
            when(voucherRepository.existsByCode("TEST")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoucherOutput out = createUseCase.execute(baseInput(
                    new BigDecimal("10"), VoucherType.PERCENT, Set.of(1L, 2L, 3L), VoucherScope.SPECIFIC_COURSES));

            assertThat(out.applicableCourseIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — date range validation")
    class DateRangeValidation {

        @Test
        @DisplayName("validFrom > validTo → reject")
        void invalidRange() {
            CreateVoucherInput input = new CreateVoucherInput(
                    5L, staffRole, "TEST", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().plusDays(2),
                    LocalDateTime.now().plusDays(1),  // validTo trước validFrom
                    null, null, null, null, null);

            assertThatThrownBy(() -> createUseCase.execute(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CreateVoucherUseCase — code normalization")
    class CodeNormalization {

        @Test
        @DisplayName("Tạo với code có whitespace → lưu uppercase trim")
        void normalize() {
            when(voucherRepository.existsByCode("WELCOME50")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateVoucherInput input = new CreateVoucherInput(
                    5L, staffRole, "  welcome50  ", VoucherType.PERCENT, new BigDecimal("10"),
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    null, null, null, null, null);

            VoucherOutput out = createUseCase.execute(input);
            assertThat(out.code()).isEqualTo("WELCOME50");
        }
    }

    @Nested
    @DisplayName("DeleteVoucherUseCase — soft-delete")
    class SoftDelete {

        private DeleteVoucherUseCase deleteUseCase;

        @org.junit.jupiter.api.BeforeEach
        void initDelete() {
            deleteUseCase = new DeleteVoucherUseCase(voucherRepository);
        }

        @Test
        @DisplayName("Soft-delete: voucher → status = INACTIVE, KHÔNG remove khỏi DB")
        void softDelete() {
            Voucher v = Voucher.reconstitute(10L, "X", VoucherType.PERCENT, BigDecimal.TEN,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());

            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            deleteUseCase.execute(new DeleteVoucherInput(10L, 5L, staffRole));

            assertThat(v.getStatus()).isEqualTo(VoucherStatus.INACTIVE);
            verify(voucherRepository).save(v);
        }

        @Test
        @DisplayName("Voucher không tồn tại → VoucherNotFound")
        void notFound() {
            when(voucherRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deleteUseCase.execute(
                    new DeleteVoucherInput(99L, 5L, staffRole)))
                    .isInstanceOf(VoucherNotFoundException.class);
        }

        @Test
        @DisplayName("MEMBER → CourseAccessDenied")
        void memberDenied() {
            assertThatThrownBy(() -> deleteUseCase.execute(
                    new DeleteVoucherInput(10L, 5L, memberRole)))
                    .isInstanceOf(CourseAccessDeniedException.class);
        }

        @Test
        @DisplayName("Soft-delete idempotent: gọi 2 lần vẫn INACTIVE")
        void idempotent() {
            Voucher v = Voucher.reconstitute(10L, "X", VoucherType.PERCENT, BigDecimal.TEN,
                    VoucherStatus.INACTIVE,  // đã INACTIVE
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());

            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            deleteUseCase.execute(new DeleteVoucherInput(10L, 5L, staffRole));
            assertThat(v.getStatus()).isEqualTo(VoucherStatus.INACTIVE);
        }
    }
}
