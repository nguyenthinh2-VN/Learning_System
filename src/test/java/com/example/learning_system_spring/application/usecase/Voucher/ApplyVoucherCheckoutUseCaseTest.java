package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.PurchaseCourseInput;
import com.example.learning_system_spring.application.dto.Voucher.PurchaseCourseOutput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.AlreadyEnrolledException;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.CourseNotPublishedException;
import com.example.learning_system_spring.domain.exception.InsufficientBalanceException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUsageLimitReachedException;
import com.example.learning_system_spring.domain.exception.VoucherUseDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.Role;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;
import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import com.example.learning_system_spring.infrastructure.service.PurchaseLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adversarial test cho ApplyVoucherCheckoutUseCase — entry point DUY NHẤT cho purchase.
 *
 * Cover:
 *   - Internal Member luôn 0đ, voucher bị bỏ qua, KHÔNG tạo VoucherUsage.
 *   - Role check: INSTRUCTOR/STAFF/ADMIN_USER gửi voucher → 403.
 *   - AlreadyEnrolled, InsufficientBalance, CourseNotPublished, CourseNotFound, UserNotFound.
 *   - Quote-Checkout price parity (Property #12, anti-tampering chính).
 *   - Race condition: usage limit kiểm lại sau lock.
 *   - Audit log đúng event.
 */
@ExtendWith(MockitoExtension.class)
class ApplyVoucherCheckoutUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private VoucherUsageRepository voucherUsageRepository;
    @Mock private com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository walletTransactionRepository;
    @Mock private PurchaseLedgerService purchaseLedgerService;

    private final PricingEngine pricingEngine = new PricingEngine();
    private final VoucherValidator voucherValidator = new VoucherValidator();

    private ApplyVoucherCheckoutUseCase useCase;

    private final Role memberRole = Role.reconstitute(1L, "MEMBER", null);
    private final Role instructorRole = Role.reconstitute(2L, "INSTRUCTOR", null);

    @BeforeEach
    void setup() {
        useCase = new ApplyVoucherCheckoutUseCase(
                userRepository, courseRepository, enrollmentRepository,
                voucherRepository, voucherUsageRepository,
                walletTransactionRepository,
                pricingEngine, voucherValidator,
                purchaseLedgerService);
    }

    private User userWithBalance(BigDecimal balance) {
        return User.reconstitute(5L, "u", "u@e.com", "p", "n",
                memberRole, false, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    private User internalUser(BigDecimal balance) {
        return User.reconstitute(5L, "u", "u@e.com", "p", "n",
                memberRole, true, balance, LocalDateTime.now(), LocalDateTime.now());
    }

    private Course publishedCourse(Long id, BigDecimal price) {
        return Course.reconstitute(id, "T", "d", 100, 0, price, 99L,
                null, true, true, LocalDateTime.now(), 1L, List.of());
    }

    private Course draftCourse(Long id, BigDecimal price) {
        return Course.reconstitute(id, "T", "d", 100, 0, price, 99L,
                null, false, false, null, null, List.of());
    }

    private Voucher activeVoucher(String code, BigDecimal value) {
        return Voucher.reconstitute(10L, code, VoucherType.PERCENT, value,
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    private Voucher activeVoucherWithLimit(String code, BigDecimal value, Long limit) {
        return Voucher.reconstitute(10L, code, VoucherType.PERCENT, value,
                VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                BigDecimal.ZERO, BigDecimal.ZERO, limit, 0,
                new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());
    }

    private Enrollment fakeEnrollment(Long enrollmentId, BigDecimal paid) {
        return Enrollment.reconstitute(enrollmentId, 5L, 1L, paid, LocalDateTime.now());
    }

    @Nested
    @DisplayName("Pre-checks — User / Course / Enrollment")
    class PreChecks {

        @Test
        @DisplayName("User không tồn tại → UserNotFoundException")
        void userMissing() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Course không tồn tại → CourseNotFoundException")
        void courseMissing() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(
                    Optional.of(userWithBalance(new BigDecimal("1000000"))));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(CourseNotFoundException.class);
        }

        @Test
        @DisplayName("Course chưa publish → CourseNotPublishedException")
        void courseDraft() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(
                    Optional.of(userWithBalance(new BigDecimal("1000000"))));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(
                    Optional.of(draftCourse(1L, new BigDecimal("100"))));

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(CourseNotPublishedException.class);
        }

        @Test
        @DisplayName("Đã enrolled → AlreadyEnrolledException")
        void alreadyEnrolled() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(
                    Optional.of(userWithBalance(new BigDecimal("1000000"))));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(AlreadyEnrolledException.class);

            verify(enrollmentRepository, never()).save(any());
            verify(voucherUsageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Internal Member always 0đ")
    class InternalMemberCheckout {

        @Test
        @DisplayName("Internal Member không gửi voucher → paid 0, KHÔNG tạo VoucherUsage")
        void internalNoVoucher() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(
                    Optional.of(internalUser(new BigDecimal("100"))));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("500000"))));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any())).thenAnswer(inv -> {
                Enrollment e = inv.getArgument(0);
                return Enrollment.reconstitute(123L, e.getUserId(), e.getCourseId(),
                        e.getPaidPrice(), e.getEnrolledAt());
            });

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, true));

            assertThat(out.paidPrice()).isEqualByComparingTo("0");
            assertThat(out.finalPrice()).isEqualByComparingTo("0");
            assertThat(out.voucherApplied()).isFalse();
            verify(voucherUsageRepository, never()).save(any());
            // paidPrice = 0 → KHÔNG ghi giao dịch ví
            verify(walletTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Internal Member gửi voucher → voucher BỊ BỎ QUA, không load voucher, paid = 0")
        void internalIgnoresVoucher() {
            when(userRepository.findByIdForUpdate(5L)).thenReturn(
                    Optional.of(internalUser(BigDecimal.ZERO)));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("500000"))));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(99L, BigDecimal.ZERO));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, "WELCOME50", 5L, memberRole, true));

            assertThat(out.paidPrice()).isEqualByComparingTo("0");
            verify(voucherRepository, never()).findByCode(any());
            verify(voucherRepository, never()).findByIdForUpdate(any());
            verify(voucherUsageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Role permission")
    class RoleCheck {

        @Test
        @DisplayName("INSTRUCTOR gửi voucher → VoucherUseDeniedException, không trừ tiền, không tạo enrollment")
        void instructorDenied() {
            // Set up: user là instructor (giả sử), course publish, balance đủ.
            User instructor = User.reconstitute(5L, "i", "i@e.com", "p", "n",
                    instructorRole, false, new BigDecimal("1000000"),
                    LocalDateTime.now(), LocalDateTime.now());
            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(instructor));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(
                    Optional.of(publishedCourse(1L, new BigDecimal("100"))));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, "X", 5L, instructorRole, false)))
                    .isInstanceOf(VoucherUseDeniedException.class);

            // Không trừ tiền, không enroll, không lưu usage
            verify(enrollmentRepository, never()).save(any());
            verify(voucherUsageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("No voucher path")
    class NoVoucher {

        @Test
        @DisplayName("Member không gửi voucher, balance đủ → mua thành công full giá")
        void memberPayFullPrice() {
            User user = userWithBalance(new BigDecimal("1000000"));
            Course course = publishedCourse(1L, new BigDecimal("500000"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, new BigDecimal("500000")));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false));

            assertThat(out.paidPrice()).isEqualByComparingTo("500000");
            assertThat(out.finalPrice()).isEqualByComparingTo("500000");
            assertThat(user.getBalance()).isEqualByComparingTo("500000");  // 1tr − 500k
            verify(voucherUsageRepository, never()).save(any());
            verify(purchaseLedgerService).logPurchase(eq(5L), eq(1L), any(), any());

            // Ghi giao dịch tiền ra (PURCHASE) vào lịch sử ví
            ArgumentCaptor<com.example.learning_system_spring.domain.model.Wallet.WalletTransaction> txCaptor =
                    ArgumentCaptor.forClass(com.example.learning_system_spring.domain.model.Wallet.WalletTransaction.class);
            verify(walletTransactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getSource())
                    .isEqualTo(com.example.learning_system_spring.domain.model.Wallet.TxSource.PURCHASE);
            assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("500000");
        }

        @Test
        @DisplayName("Balance không đủ → InsufficientBalanceException, không trừ tiền")
        void insufficientBalance() {
            User user = userWithBalance(new BigDecimal("100"));  // cần 500k mà chỉ có 100đ
            Course course = publishedCourse(1L, new BigDecimal("500000"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(InsufficientBalanceException.class);

            assertThat(user.getBalance()).isEqualByComparingTo("100");  // KHÔNG bị trừ
            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Course free (price = 0) + member balance = 0 → mua được, paid = 0")
        void freeCourseZeroBalance() {
            User user = userWithBalance(BigDecimal.ZERO);
            Course course = publishedCourse(1L, BigDecimal.ZERO);

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, BigDecimal.ZERO));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false));

            assertThat(out.paidPrice()).isEqualByComparingTo("0");
            assertThat(user.getBalance()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("With voucher path")
    class WithVoucher {

        @Test
        @DisplayName("Voucher hợp lệ → server tính lại giá, trừ đúng số đã giảm, ghi VoucherUsage và audit log")
        void happyPathWithVoucher() {
            User user = userWithBalance(new BigDecimal("1000000"));
            Course course = publishedCourse(1L, new BigDecimal("1000000"));
            Voucher voucher = activeVoucher("OFF50", new BigDecimal("50"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("OFF50")).thenReturn(Optional.of(voucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(voucher));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, new BigDecimal("500000")));
            when(voucherUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, "off50", 5L, memberRole, false));

            assertThat(out.paidPrice()).isEqualByComparingTo("500000");
            assertThat(out.discountAmount()).isEqualByComparingTo("500000");
            assertThat(out.finalPrice()).isEqualByComparingTo("500000");
            assertThat(out.voucherApplied()).isTrue();
            assertThat(out.voucherCode()).isEqualTo("OFF50");

            // user balance giảm 500k (chứ không phải 1tr)
            assertThat(user.getBalance()).isEqualByComparingTo("500000");

            // VoucherUsage được tạo với đúng giá trị
            ArgumentCaptor<VoucherUsage> usageCaptor = ArgumentCaptor.forClass(VoucherUsage.class);
            verify(voucherUsageRepository).save(usageCaptor.capture());
            VoucherUsage usage = usageCaptor.getValue();
            assertThat(usage.getVoucherId()).isEqualTo(10L);
            assertThat(usage.getUserId()).isEqualTo(5L);
            assertThat(usage.getCourseId()).isEqualTo(1L);
            assertThat(usage.getOriginalPrice()).isEqualByComparingTo("1000000");
            assertThat(usage.getDiscountAmount()).isEqualByComparingTo("500000");
            assertThat(usage.getFinalPrice()).isEqualByComparingTo("500000");

            // Audit log VOUCHER_APPLIED
            verify(purchaseLedgerService).logVoucherApplied(
                    eq(5L), eq(1L), eq(10L), eq("OFF50"),
                    any(), any(), any(), any(), any());
            // Không log PURCHASE_COMPLETED khi có voucher (theo code hiện tại)
            verify(purchaseLedgerService, never()).logPurchase(anyLong(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("Property #12 — Quote-Checkout price parity: cùng input → cùng giá")
        void quoteCheckoutPriceParity() {
            // Test này verify PROPERTY chính của anti-tampering: quote và checkout
            // tính ra cùng giá khi state DB không đổi. Đây là metamorphic property.
            // Setup: course price 1tr, voucher 30% PERCENT.
            User user = userWithBalance(new BigDecimal("10000000"));
            Course course = publishedCourse(1L, new BigDecimal("1000000"));
            Voucher voucher = activeVoucher("OFF30", new BigDecimal("30"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("OFF30")).thenReturn(Optional.of(voucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(voucher));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, new BigDecimal("700000")));
            when(voucherUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseCourseOutput checkout = useCase.execute(
                    new PurchaseCourseInput(1L, "OFF30", 5L, memberRole, false));

            // Tính bằng PricingEngine trực tiếp — phải ra cùng giá
            BigDecimal expected = pricingEngine.compute(new BigDecimal("1000000"), voucher).finalPrice();
            assertThat(checkout.finalPrice()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Anti-tampering: server gọi Pricing_Engine với originalPrice từ DB, KHÔNG từ client")
        void serverComputesFromDbNotClient() {
            // PurchaseCourseInput không có field price. Server chỉ nhận courseId.
            // → Bằng compile-time guarantee, không thể truyền giá từ client.
            // Test này verify courseRepository.findByIdForUpdate được gọi, kết quả giá ra đúng theo course price.
            User user = userWithBalance(new BigDecimal("10000000"));
            Course course = publishedCourse(1L, new BigDecimal("777777"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, new BigDecimal("777777")));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false));

            assertThat(out.originalPrice()).isEqualByComparingTo("777777");
            assertThat(out.paidPrice()).isEqualByComparingTo("777777");
            // Verify đã đọc course từ DB
            verify(courseRepository).findByIdForUpdate(1L);
        }

        @Test
        @DisplayName("Race condition guard: voucher đầy quota → VOUCHER_USAGE_LIMIT_REACHED, KHÔNG enroll")
        void usageLimitReachedRollback() {
            User user = userWithBalance(new BigDecimal("1000000"));
            Course course = publishedCourse(1L, new BigDecimal("100000"));
            Voucher voucher = activeVoucherWithLimit("LIMITED", new BigDecimal("50"), 5L);

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("LIMITED")).thenReturn(Optional.of(voucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(voucher));
            // Mô phỏng giả lập: usedCount đã đầy
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(5L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, "LIMITED", 5L, memberRole, false)))
                    .isInstanceOf(VoucherUsageLimitReachedException.class);

            // Quan trọng: không enroll, không trừ tiền, không tạo usage
            assertThat(user.getBalance()).isEqualByComparingTo("1000000");
            verify(enrollmentRepository, never()).save(any());
            verify(voucherUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Voucher tồn tại nhưng findByIdForUpdate trả empty (lỗi DB) → VoucherNotFound")
        void voucherDisappearsBeforeLock() {
            User user = userWithBalance(new BigDecimal("1000000"));
            Course course = publishedCourse(1L, new BigDecimal("100000"));
            Voucher voucher = activeVoucher("X", new BigDecimal("50"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("X")).thenReturn(Optional.of(voucher));
            // Race: voucher bị xóa giữa findByCode và findByIdForUpdate
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, "X", 5L, memberRole, false)))
                    .isInstanceOf(com.example.learning_system_spring.domain.exception.VoucherNotFoundException.class);

            verify(enrollmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Voucher hợp lệ + balance không đủ giá đã giảm → InsufficientBalance")
        void insufficientForFinalPrice() {
            User user = userWithBalance(new BigDecimal("100"));  // Chỉ có 100đ, course 1tr giảm 50% còn 500k
            Course course = publishedCourse(1L, new BigDecimal("1000000"));
            Voucher voucher = activeVoucher("OFF50", new BigDecimal("50"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("OFF50")).thenReturn(Optional.of(voucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(voucher));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, "OFF50", 5L, memberRole, false)))
                    .isInstanceOf(InsufficientBalanceException.class);

            assertThat(user.getBalance()).isEqualByComparingTo("100");
            verify(voucherUsageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Voucher FIXED giảm > price → final = 0, paid = 0, mua được dù balance = 0")
        void fixedVoucherZerosOutPrice() {
            User user = userWithBalance(BigDecimal.ZERO);
            Course course = publishedCourse(1L, new BigDecimal("100000"));
            Voucher fixedVoucher = Voucher.reconstitute(10L, "FREE100K", VoucherType.FIXED,
                    new BigDecimal("999999"), VoucherStatus.ACTIVE, VoucherScope.ALL_COURSES,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0,
                    new HashSet<>(), LocalDateTime.now(), LocalDateTime.now());

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("FREE100K")).thenReturn(Optional.of(fixedVoucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(fixedVoucher));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, BigDecimal.ZERO));
            when(voucherUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseCourseOutput out = useCase.execute(
                    new PurchaseCourseInput(1L, "FREE100K", 5L, memberRole, false));

            assertThat(out.finalPrice()).isEqualByComparingTo("0");
            assertThat(out.paidPrice()).isEqualByComparingTo("0");
            assertThat(out.discountAmount()).isEqualByComparingTo("100000");
            assertThat(user.getBalance()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Voucher code normalization")
    class CodeNormalization {

        @Test
        @DisplayName("Voucher code lowercase + whitespace được normalize trước khi lookup")
        void codeNormalizedBeforeLookup() {
            User user = userWithBalance(new BigDecimal("1000000"));
            Course course = publishedCourse(1L, new BigDecimal("100"));
            Voucher voucher = activeVoucher("WELCOME50", new BigDecimal("10"));

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(course));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);
            when(voucherRepository.findByCode("WELCOME50")).thenReturn(Optional.of(voucher));
            when(voucherRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(voucher));
            when(voucherUsageRepository.countByVoucherId(10L)).thenReturn(0L);
            when(voucherUsageRepository.countByVoucherIdAndUserId(10L, 5L)).thenReturn(0L);
            when(enrollmentRepository.save(any())).thenReturn(fakeEnrollment(1L, new BigDecimal("90")));
            when(voucherUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new PurchaseCourseInput(1L, "  welcome50  ", 5L, memberRole, false));

            verify(voucherRepository).findByCode("WELCOME50");
        }
    }

    @Nested
    @DisplayName("Course capacity guard")
    class CapacityGuard {

        @Test
        @DisplayName("Course đã full → IllegalStateException, không đụng đến voucher")
        void courseFullRejected() {
            User user = userWithBalance(new BigDecimal("1000000"));
            // Course với maxStudents = 1, enrolledCount = 1 (đầy)
            Course full = Course.reconstitute(1L, "T", "d", 1, 1,
                    new BigDecimal("100"), 99L,
                    null, true, true, LocalDateTime.now(), 1L, List.of());

            when(userRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
            when(courseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(full));
            when(enrollmentRepository.existsByUserIdAndCourseId(5L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> useCase.execute(
                    new PurchaseCourseInput(1L, null, 5L, memberRole, false)))
                    .isInstanceOf(IllegalStateException.class);

            verify(enrollmentRepository, never()).save(any());
        }
    }
}
