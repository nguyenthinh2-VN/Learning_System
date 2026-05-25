package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.UpdateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherCodeAlreadyExistsException;
import com.example.learning_system_spring.domain.exception.VoucherImmutableFieldException;
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
 * Unit test cho UpdateVoucherUseCase.
 *
 * Covers:
 *   - Authorization: MEMBER bị chặn, null role trả 403 (FIX-C)
 *   - Voucher not found
 *   - usageLimit validation
 *   - FIX-A: status null bị reject bởi updateSoftFields
 *   - DEC-1: code/type/value sửa được khi usedCount=0, bị chặn khi usedCount>0
 */
@ExtendWith(MockitoExtension.class)
class UpdateVoucherUseCaseTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageRepository voucherUsageRepository;

    @InjectMocks
    private UpdateVoucherUseCase useCase;

    private final Role staffRole  = Role.reconstitute(3L, "STAFF", null);
    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);

    /** Voucher hợp lệ dùng làm base cho các test. */
    private Voucher existingVoucher() {
        return Voucher.reconstitute(10L, "OLD", VoucherType.PERCENT, new BigDecimal("10"),
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 100L, 0,
                new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    /** Helper tạo input hợp lệ không sửa immutable fields. */
    private UpdateVoucherInput validSoftInput(Role role) {
        return new UpdateVoucherInput(
                10L, 5L, role,
                null, null, null,                                          // không sửa code/type/value
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authorization
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Authorization")
    class Authorization {

        @Test
        @DisplayName("MEMBER → CourseAccessDeniedException (không có quyền MANAGE_VOUCHER)")
        void memberDenied() {
            assertThatThrownBy(() -> useCase.execute(validSoftInput(memberRole)))
                    .isInstanceOf(CourseAccessDeniedException.class);
            verify(voucherRepository, never()).findById(any());
        }

        @Test
        @DisplayName("FIX-C: role null → CourseAccessDeniedException (không còn NPE)")
        void nullRoleReturnsFalseNotNpe() {
            assertThatThrownBy(() -> useCase.execute(validSoftInput(null)))
                    .isInstanceOf(CourseAccessDeniedException.class)
                    .isNotInstanceOf(NullPointerException.class);
            verify(voucherRepository, never()).findById(any());
        }

        @Test
        @DisplayName("STAFF → được phép thực thi")
        void staffAllowed() {
            Voucher v = existingVoucher();
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(v));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoucherOutput out = useCase.execute(validSoftInput(staffRole));
            assertThat(out).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Voucher not found
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Voucher not found")
    class NotFound {

        @Test
        @DisplayName("voucherId không tồn tại → VoucherNotFoundException")
        void voucherMissing() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(validSoftInput(staffRole)))
                    .isInstanceOf(VoucherNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // usageLimit validation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("usageLimit validation")
    class UsageLimitValidation {

        @Test
        @DisplayName("usedCount=50, newLimit=30 → VoucherUsageLimitTooLowException")
        void newLimitBelowUsedCount() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, null,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    30L, 0, new HashSet<>());   // 30 < 50 → reject

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherUsageLimitTooLowException.class);
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("newLimit=0 (unlimited) → cho phép kể cả khi usedCount > 0")
        void zeroLimitMeansUnlimited() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, null,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, 0, new HashSet<>());    // 0 = unlimited

            VoucherOutput out = useCase.execute(input);
            assertThat(out.usageLimit()).isZero();
        }

        @Test
        @DisplayName("newLimit = usedCount (boundary) → KHÔNG reject")
        void exactlyUsedCountIsAllowed() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(50L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, null,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    50L, 0, new HashSet<>());   // 50 == 50 → pass (condition: newLimit < usedCount)

            useCase.execute(input);
            verify(voucherRepository).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX-A: status null bị reject
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FIX-A: status null bị reject")
    class StatusNullFixed {

        @Test
        @DisplayName("Input status=null → IllegalArgumentException từ updateSoftFields (không còn state corrupt)")
        void statusNullRejected() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, null,
                    null,                       // status = null → phải bị reject
                    VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status");
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("Input scope=null → IllegalArgumentException từ updateSoftFields")
        void scopeNullRejected() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, null,
                    VoucherStatus.ACTIVE,
                    null,                       // scope = null → phải bị reject
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scope");
            verify(voucherRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEC-1: Immutable fields (code / type / value)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DEC-1: Immutable fields (code / type / value)")
    class ImmutableFields {

        @Test
        @DisplayName("usedCount=0, gửi newCode → được phép sửa")
        void changeCodeWhenNoUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.existsByCode("NEWCODE")).thenReturn(false);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    "newcode", null, null,      // muốn đổi code → "NEWCODE" sau normalize
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            VoucherOutput out = useCase.execute(input);
            assertThat(out.code()).isEqualTo("NEWCODE");
        }

        @Test
        @DisplayName("usedCount=0, gửi newType=FIXED → được phép sửa")
        void changeTypeWhenNoUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, VoucherType.FIXED, null,   // đổi type
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            VoucherOutput out = useCase.execute(input);
            assertThat(out.type()).isEqualTo(VoucherType.FIXED);
        }

        @Test
        @DisplayName("usedCount=0, gửi newValue=50 → được phép sửa")
        void changeValueWhenNoUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, new BigDecimal("50"),   // đổi value
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            VoucherOutput out = useCase.execute(input);
            assertThat(out.value()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("usedCount>0, gửi newCode → VoucherImmutableFieldException (field=code)")
        void changeCodeWhenHasUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(5L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    "NEWCODE", null, null,      // muốn đổi code nhưng đã có usage
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherImmutableFieldException.class)
                    .hasMessageContaining("code");
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("usedCount>0, gửi newType → VoucherImmutableFieldException (field=type)")
        void changeTypeWhenHasUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(1L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, VoucherType.FIXED, null,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherImmutableFieldException.class)
                    .hasMessageContaining("type");
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("usedCount>0, gửi newValue → VoucherImmutableFieldException (field=value)")
        void changeValueWhenHasUsage() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(1L);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    null, null, new BigDecimal("99"),
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherImmutableFieldException.class)
                    .hasMessageContaining("value");
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("usedCount=0, newCode trùng với voucher khác → VoucherCodeAlreadyExistsException")
        void changeCodeDuplicate() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.existsByCode("TAKEN")).thenReturn(true);

            UpdateVoucherInput input = new UpdateVoucherInput(
                    10L, 5L, staffRole,
                    "taken", null, null,        // normalize → "TAKEN", đã tồn tại
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0, new HashSet<>());

            assertThatThrownBy(() -> useCase.execute(input))
                    .isInstanceOf(VoucherCodeAlreadyExistsException.class);
            verify(voucherRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không gửi immutable fields (tất cả null) → code/type/value giữ nguyên")
        void noImmutableChange_keepsOriginal() {
            when(voucherRepository.findById(10L)).thenReturn(Optional.of(existingVoucher()));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VoucherOutput out = useCase.execute(validSoftInput(staffRole));

            assertThat(out.code()).isEqualTo("OLD");
            assertThat(out.type()).isEqualTo(VoucherType.PERCENT);
            assertThat(out.value()).isEqualByComparingTo("10");
        }
    }
}
