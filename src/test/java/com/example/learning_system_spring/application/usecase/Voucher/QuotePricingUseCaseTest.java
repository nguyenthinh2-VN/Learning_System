package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.QuotePricingInput;
import com.example.learning_system_spring.application.dto.Voucher.QuotePricingOutput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.CourseNotPublishedException;
import com.example.learning_system_spring.domain.exception.VoucherExpiredException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUseDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adversarial test cho QuotePricingUseCase.
 * Cover: anti-tampering, role checks, internal member, course not published, voucher invalid.
 */
@ExtendWith(MockitoExtension.class)
class QuotePricingUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageRepository voucherUsageRepository;

    private final PricingEngine pricingEngine = new PricingEngine();
    private final VoucherValidator voucherValidator = new VoucherValidator();

    @InjectMocks
    private QuotePricingUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);
    private final Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", null);
    private final Role staffRole = Role.reconstitute(3L, "STAFF", null);
    private final Role adminUserRole = Role.reconstitute(4L, "ADMIN_USER", null);
    private final Role superAdminRole = Role.reconstitute(5L, "SUPER_ADMIN", null);

    @BeforeEach
    void wireRealDomainServices() {
        // Re-init useCase với real PricingEngine + VoucherValidator vì @InjectMocks không inject được
        // các bean concrete khác mock — kiểm tra qua reflection sẽ phức tạp, dùng constructor:
        useCase = new QuotePricingUseCase(courseRepository, voucherRepository, voucherUsageRepository,
                pricingEngine, voucherValidator);
    }

    private Course publishedCourse(Long id, BigDecimal price) {
        Course c = Course.reconstitute(id, "T", "d", 100, 0, price, 99L,
                true, true, LocalDateTime.now(), 1L, List.of());
        return c;
    }

    private Course draftCourse(Long id, BigDecimal price) {
        return Course.reconstitute(id, "T", "d", 100, 0, price, 99L,
                false, false, null, null, List.of());
    }

    private Voucher activeVoucher(String code, BigDecimal value) {
        return Voucher.reconstitute(10L, code, VoucherType.PERCENT, value,
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("Course not found / not published")
    class CourseChecks {

        @Test
        @DisplayName("Course không tồn tại → CourseNotFoundException")
        void courseMissing() {
            when(courseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(CourseNotFoundException.class);
        }

        @Test
        @DisplayName("Course chưa publish → CourseNotPublishedException (kể cả MEMBER)")
        void courseDraft() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(draftCourse(1L, new BigDecimal("100"))));

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(CourseNotPublishedException.class);
        }
    }

    @Nested
    @DisplayName("Internal Member always 0đ")
    class InternalMember {

        @Test
        @DisplayName("Internal Member, không gửi voucher → finalPrice = 0, internalDiscount = true")
        void internalNoVoucher() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("500000"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, true));

            assertThat(out.finalPrice()).isEqualByComparingTo("0");
            assertThat(out.internalDiscount()).isTrue();
            assertThat(out.voucherApplied()).isFalse();
            // Quan trọng: KHÔNG đụng vào voucherRepository
            verify(voucherRepository, never()).findByCode(anyString());
        }

        @Test
        @DisplayName("Internal Member gửi voucher → voucher bị bỏ qua, finalPrice vẫn = 0")
        void internalWithVoucher() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("500000"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, "WELCOME50", 5L, memberRole, true));

            assertThat(out.finalPrice()).isEqualByComparingTo("0");
            assertThat(out.internalDiscount()).isTrue();
            assertThat(out.voucherApplied()).isFalse();
            // KHÔNG load voucher (Internal trả về sớm)
            verify(voucherRepository, never()).findByCode(anyString());
        }
    }

    @Nested
    @DisplayName("Role permission for voucher")
    class RolePermission {

        @Test
        @DisplayName("INSTRUCTOR gửi voucherCode → VoucherUseDeniedException")
        void instructorVoucherDenied() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, "WELCOME50", 5L, instructorRole, false)))
                    .isInstanceOf(VoucherUseDeniedException.class);
        }

        @Test
        @DisplayName("STAFF gửi voucherCode → VoucherUseDeniedException")
        void staffVoucherDenied() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, "WELCOME50", 5L, staffRole, false)))
                    .isInstanceOf(VoucherUseDeniedException.class);
        }

        @Test
        @DisplayName("ADMIN_USER gửi voucherCode → VoucherUseDeniedException")
        void adminUserVoucherDenied() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, "WELCOME50", 5L, adminUserRole, false)))
                    .isInstanceOf(VoucherUseDeniedException.class);
        }

        @Test
        @DisplayName("SUPER_ADMIN gửi voucherCode → CHO PHÉP (qua isSuperAdmin check)")
        void superAdminAllowed() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(voucherRepository.findByCode("WELCOME50")).thenReturn(
                    Optional.of(activeVoucher("WELCOME50", new BigDecimal("10"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, "WELCOME50", 5L, superAdminRole, false));

            assertThat(out.voucherApplied()).isTrue();
        }

        @Test
        @DisplayName("MEMBER không gửi voucher → vẫn xem giá được (no permission check khi không có voucher)")
        void memberNoVoucherOk() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("500000"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false));

            assertThat(out.finalPrice()).isEqualByComparingTo("500000.00");
            assertThat(out.voucherApplied()).isFalse();
        }

        @Test
        @DisplayName("Bug-observation: STAFF không gửi voucher → vẫn pass (chỉ chặn khi có voucherCode)")
        void staffNoVoucherAllowed() {
            // Spec mục 10.6: "Price_Preview_Controller SHALL được mount riêng cho voucher preview.
            // Nếu user không phải MEMBER, controller SHALL trả lỗi 403."
            // Hiện tại impl CHỈ chặn khi có voucherCode. Nếu STAFF gọi /quote không kèm voucher,
            // request VẪN PASS và trả giá gốc — không khớp spec voucher-management mục 10.6.
            // Nhưng spec voucher-pricing không yêu cầu chặn. Đây là lệch giữa 2 spec.
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, staffRole, false));

            assertThat(out.finalPrice()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Voucher logic")
    class VoucherLogic {

        @Test
        @DisplayName("voucherCode whitespace-only → coi như null, không áp dụng voucher")
        void whitespaceVoucherCode() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, "   ", 5L, memberRole, false));

            assertThat(out.voucherApplied()).isFalse();
            verify(voucherRepository, never()).findByCode(anyString());
        }

        @Test
        @DisplayName("voucherCode lowercase → normalize uppercase trước khi lookup")
        void lowercaseNormalized() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            // Mock chỉ return khi gọi với UPPERCASE
            when(voucherRepository.findByCode("WELCOME50")).thenReturn(
                    Optional.of(activeVoucher("WELCOME50", new BigDecimal("10"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, "  welcome50  ", 5L, memberRole, false));

            assertThat(out.voucherApplied()).isTrue();
            verify(voucherRepository).findByCode("WELCOME50");
        }

        @Test
        @DisplayName("Voucher không tồn tại → VoucherNotFoundException")
        void voucherNotFound() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(voucherRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, "unknown", 5L, memberRole, false)))
                    .isInstanceOf(VoucherNotFoundException.class);
        }

        @Test
        @DisplayName("Voucher hết hạn → VoucherExpired bubbles up")
        void expiredVoucher() {
            Voucher expired = Voucher.reconstitute(10L, "EXPIRED", VoucherType.PERCENT, BigDecimal.TEN,
                    VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());

            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(voucherRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expired));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);

            assertThatThrownBy(() -> useCase.execute(
                    new QuotePricingInput(1L, "expired", 5L, memberRole, false)))
                    .isInstanceOf(VoucherExpiredException.class);
        }

        @Test
        @DisplayName("Voucher hợp lệ + course price 1tr + 50% → finalPrice 500000")
        void happyPath() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("1000000"))));
            when(voucherRepository.findByCode("OFF50")).thenReturn(
                    Optional.of(activeVoucher("OFF50", new BigDecimal("50"))));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, "off50", 5L, memberRole, false));

            assertThat(out.finalPrice()).isEqualByComparingTo("500000.00");
            assertThat(out.discountAmount()).isEqualByComparingTo("500000.00");
            assertThat(out.voucherCode()).isEqualTo("OFF50");
            assertThat(out.voucherType()).isEqualTo(VoucherType.PERCENT);
        }

        @Test
        @DisplayName("Property #14: gọi /quote nhiều lần → không tạo VoucherUsage")
        void quoteDoesNotConsumeVoucher() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(voucherRepository.findByCode("X")).thenReturn(
                    Optional.of(activeVoucher("X", new BigDecimal("10"))));
            when(voucherUsageRepository.countByVoucherId(anyLong())).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(anyLong(), anyLong())).thenReturn(0L);

            for (int i = 0; i < 5; i++) {
                useCase.execute(new QuotePricingInput(1L, "x", 5L, memberRole, false));
            }
            // KHÔNG bao giờ save VoucherUsage trong /quote flow
            verify(voucherUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Property #13: idempotence — cùng input → cùng kết quả price (quotedAt được phép khác)")
        void idempotentPriceFields() {
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));

            QuotePricingOutput first = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false));
            QuotePricingOutput second = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false));

            assertThat(second.originalPrice()).isEqualByComparingTo(first.originalPrice());
            assertThat(second.discountAmount()).isEqualByComparingTo(first.discountAmount());
            assertThat(second.finalPrice()).isEqualByComparingTo(first.finalPrice());
        }
    }

    @Nested
    @DisplayName("Anti-tampering — server side recomputation")
    class AntiTampering {

        @Test
        @DisplayName("Server LUÔN đọc giá từ DB theo courseId — client KHÔNG truyền giá nào")
        void serverReadsFromDb() {
            // QuotePricingInput chỉ có courseId + voucherCode + role + isInternal.
            // Compile-time guarantee: KHÔNG có field price nào client truyền lên được.
            // Test này verify behavior bằng cách mock course price = 7777 và đảm bảo output trả 7777.
            when(courseRepository.findById(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("7777"))));

            QuotePricingOutput out = useCase.execute(
                    new QuotePricingInput(1L, null, 5L, memberRole, false));

            // Giá luôn từ DB. Không có cách nào client gửi 7777 lên cả.
            assertThat(out.originalPrice()).isEqualByComparingTo("7777.00");
        }
    }
}
