package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.UpdateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUsageLimitTooLowException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adversarial test cho UpdateVoucherUseCase.
 *
 * Phơi bày:
 *   - Bug-A: status null trong input → updateSoftFields nhận null không reject → voucher state corrupt.
 *   - Bug observation: spec yêu cầu chỉ chặn sửa code/type/value khi đã có usage,
 *     nhưng impl: 3 field này không có trong UpdateVoucherInput → KHÔNG BAO GIỜ sửa được, kể cả chưa có usage.
 *     Đây là "stricter than spec" — an toàn hơn nhưng không khớp 100%.
 */
@ExtendWith(MockitoExtension.class)
class UpdateVoucherUseCaseTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageRepository voucherUsageRepository;

    @InjectMocks
    private UpdateVoucherUseCase useCase;

    private final Role staffRole = Role.reconstitute(3L, "STAFF", null);
    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);

    private Voucher existingVoucher() {
        return Voucher.reconstitute(10L, "OLD", VoucherType.PERCENT, new BigDecimal("10"),
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 100L, 0,
                new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("Authorization")
    class Authorization {

        @Test
        @DisplayName("MEMBER → CourseAccessDenied")
        void memberDenied() {
            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, memberRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(CourseAccessDeniedException.class);
            verify(voucherRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Bug-C symptom: role null → NullPointerException (KHÔNG phải 403)")
        void nullRoleNpe() {
            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, null,  // role null
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            // CourseOwnershipPolicy.hasFullAccess(null) ném NPE — leaks ra ngoài
            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Voucher not found")
    class NotFound {
        @Test
        void voucherMissing() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.empty());

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("usageLimit too low")
    class UsageLimitTooLow {

        @Test
        @DisplayName("usedCount = 50, set limit = 30 → VoucherUsageLimitTooLow")
        void newLimitBelowUsedCount() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    30L, 0, new HashSet<>());  // limit = 30 < usedCount = 50

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherUsageLimitTooLowException.class);
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("Set limit = 0 (unlimited) → cho phép kể cả khi usedCount > 0")
        void zeroLimitMeansUnlimited() {
            Voucher v = existingVoucher();
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, 0, new HashSet<>());  // 0 = unlimited

            VoucherOutput out = useCase.execute(input);
            assertThat(out.usageLimit()).isZero();
        }

        @Test
        @DisplayName("Set limit = usedCount → KHÔNG reject (boundary inclusive)")
        void exactlyUsedCount() {
            Voucher v = existingVoucher();
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    50L, 0, new HashSet<>());

            // Code: newLimit < usedCount → reject. 50 < 50 false → pass.
            useCase.execute(input);
        }
    }

    @Nested
    @DisplayName("Bug-A: status null không bị reject")
    class StatusNullBug {

        @Test
        @DisplayName("Input status = null → updateSoftFields chấp nhận → voucher status null (state corrupt)")
        void statusNullAccepted() {
            Voucher v = existingVoucher();
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null,  // BUG: status null
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, 0, new HashSet<>());

            VoucherOutput out = useCase.execute(input);
            // Voucher đã rơi vào state corrupted: status = null
            assertThat(out.status()).isNull();
            // → Sau đó nếu voucher này được áp dụng, VoucherValidator.isInactive() = false → bypass check
            // → Bug-A: voucher null status có thể được dùng như voucher ACTIVE.
            //
            // Defense-in-depth khuyến nghị: thêm check `if (input.status() == null)` ở UseCase
            // hoặc Voucher.updateSoftFields nên throw IllegalArgumentException khi newStatus null.
        }
    }

    @Nested
    @DisplayName("Bug observation: code/type/value KHÔNG có trong DTO update")
    class ImmutableFieldsObservation {

        @Test
        @DisplayName("Spec yêu cầu chặn sửa code/type/value khi có usage. Impl: 3 field không có trong DTO → không bao giờ sửa được")
        void code_type_value_neverChangeable() {
            Voucher v = existingVoucher();
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            VoucherOutput out = useCase.execute(input);
            // code, type, value KHÔNG đổi vì DTO không có. Đây là implementation choice
            // an toàn hơn spec — nhưng nếu admin muốn sửa code khi voucher chưa có usage,
            // không thể làm được. Cần xem business có chấp nhận không.
            assertThat(out.code()).isEqualTo("OLD");
            assertThat(out.type()).isEqualTo(VoucherType.PERCENT);
            assertThat(out.value()).isEqualByComparingTo("10");
        }
    }
}
