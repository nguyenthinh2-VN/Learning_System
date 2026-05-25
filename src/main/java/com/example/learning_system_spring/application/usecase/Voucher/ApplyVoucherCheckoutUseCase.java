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
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUseDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;
import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import com.example.learning_system_spring.infrastructure.service.PurchaseLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mua khóa học (có hoặc không có voucher). Đây là entry point DUY NHẤT cho luồng purchase.
 *
 * Anti-tampering & Concurrency:
 *   1. Chỉ nhận courseId (path) + voucherCode (body). Mọi field giá khác bị ignore.
 *   2. Server đọc lại giá từ DB tại thời điểm purchase, không tin preview cũ.
 *   3. Giữ pessimistic lock trên User → Course → Voucher (thứ tự cố định chống deadlock).
 *   4. Validate voucher LẦN NỮA sau khi giữ lock (kể cả preview đã pass).
 *   5. UNIQUE (voucherId, enrollmentId) ở DB chống race tạo 2 usage cho cùng enrollment.
 *
 * Internal member: paidPrice = 0, voucher bị ignore, không tạo VoucherUsage.
 * Role nội bộ (INSTRUCTOR/STAFF/ADMIN_USER) gửi voucherCode → 403 VOUCHER_USE_DENIED.
 */
@Service
@RequiredArgsConstructor
public class ApplyVoucherCheckoutUseCase {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final PricingEngine pricingEngine;
    private final VoucherValidator voucherValidator;
    private final PurchaseLedgerService purchaseLedgerService;

    @Transactional
    public PurchaseCourseOutput execute(PurchaseCourseInput input) {
        // [1] User lock (PESSIMISTIC_WRITE) — đầu tiên trong thứ tự User → Course → Voucher
        User user = userRepository.findByIdForUpdate(input.requesterId())
                .orElseThrow(() -> new UserNotFoundException(input.requesterId()));

        // [2] Course lock
        Course course = courseRepository.findByIdForUpdate(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        if (!course.isPublished()) {
            throw new CourseNotPublishedException(input.courseId());
        }

        // [3] Already enrolled?
        if (enrollmentRepository.existsByUserIdAndCourseId(input.requesterId(), input.courseId())) {
            throw new AlreadyEnrolledException(input.requesterId(), input.courseId());
        }

        if (course.isFull()) {
            throw new IllegalStateException("Khóa học đã đầy, không thể đăng ký thêm.");
        }

        BigDecimal originalPrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;

        // [4] Internal member → bypass voucher, paidPrice = 0
        if (input.isInternal()) {
            return processInternal(user, course, input, originalPrice);
        }

        // [5] Role nội bộ gửi voucherCode → reject
        boolean hasVoucher = input.voucherCode() != null && !input.voucherCode().trim().isEmpty();
        if (hasVoucher && input.requesterRole() != null && !input.requesterRole().isMember()
                && !input.requesterRole().isSuperAdmin()) {
            throw new VoucherUseDeniedException("Role của bạn không được phép sử dụng voucher.");
        }

        if (!hasVoucher) {
            return processWithoutVoucher(user, course, input, originalPrice);
        }

        return processWithVoucher(user, course, input, originalPrice);
    }

    private PurchaseCourseOutput processInternal(User user, Course course, PurchaseCourseInput input,
                                                 BigDecimal originalPrice) {
        course.enroll();
        // OPT-2: Bỏ userRepository.save(user) dư — internal member không thay đổi balance,
        //        JPA dirty checking sẽ no-op nhưng gọi thừa gây nhầm lẫn khi đọc code.
        courseRepository.save(course);

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.create(input.requesterId(), input.courseId(), BigDecimal.ZERO));

        purchaseLedgerService.logPurchase(input.requesterId(), input.courseId(), BigDecimal.ZERO,
                enrollment.getEnrolledAt());

        return new PurchaseCourseOutput(enrollment.getId(),
                originalPrice,
                originalPrice,            // discount = full
                BigDecimal.ZERO,           // final = 0
                BigDecimal.ZERO,           // paid = 0
                false, null);
    }

    private PurchaseCourseOutput processWithoutVoucher(User user, Course course, PurchaseCourseInput input,
                                                       BigDecimal originalPrice) {
        BigDecimal paidPrice = originalPrice;
        deductBalanceOrThrow(user, paidPrice);
        course.enroll();

        userRepository.save(user);
        courseRepository.save(course);

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.create(input.requesterId(), input.courseId(), paidPrice));

        purchaseLedgerService.logPurchase(input.requesterId(), input.courseId(), paidPrice,
                enrollment.getEnrolledAt());

        return new PurchaseCourseOutput(enrollment.getId(),
                originalPrice,
                BigDecimal.ZERO,
                originalPrice,
                paidPrice,
                false, null);
    }

    private PurchaseCourseOutput processWithVoucher(User user, Course course, PurchaseCourseInput input,
                                                    BigDecimal originalPrice) {
        // [6] Voucher lock (PESSIMISTIC_WRITE) — sau User và Course
        // OPT-1: Dùng findByCodeForUpdate thay vì findByCode + findByIdForUpdate (gộp 2 query → 1)
        String normalizedCode = Voucher.normalizeCode(input.voucherCode());
        Voucher locked = voucherRepository.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> new VoucherNotFoundException(normalizedCode));

        // [7] Đếm lại usedCount BÊN TRONG transaction sau khi đã giữ lock
        long globalUsed = voucherUsageRepository.countByVoucherId(locked.getId());
        long perUserUsed = voucherUsageRepository.countByVoucherIdAndUserId(locked.getId(), input.requesterId());

        // [8] Re-validate voucher tại thời điểm checkout (không tin preview cũ)
        voucherValidator.validate(locked, input.courseId(), originalPrice,
                LocalDateTime.now(), globalUsed, perUserUsed);

        // [9] Tính lại giá ở server, không tin client
        PriceQuote quote = pricingEngine.compute(originalPrice, locked);
        BigDecimal paidPrice = quote.finalPrice();

        deductBalanceOrThrow(user, paidPrice);
        course.enroll();

        userRepository.save(user);
        courseRepository.save(course);

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.create(input.requesterId(), input.courseId(), paidPrice));

        VoucherUsage usage = VoucherUsage.create(
                locked.getId(), input.requesterId(), input.courseId(), enrollment.getId(),
                quote.originalPrice(), quote.discountAmount(), quote.finalPrice());
        voucherUsageRepository.save(usage);

        purchaseLedgerService.logVoucherApplied(input.requesterId(), input.courseId(), locked.getId(),
                locked.getCode(), quote.originalPrice(), quote.discountAmount(), quote.finalPrice(),
                enrollment.getId(), enrollment.getEnrolledAt());

        return new PurchaseCourseOutput(enrollment.getId(),
                quote.originalPrice(),
                quote.discountAmount(),
                quote.finalPrice(),
                paidPrice,
                true, locked.getCode());
    }

    private void deductBalanceOrThrow(User user, BigDecimal amount) {
        if (amount.signum() <= 0) return;
        try {
            user.deductBalance(amount);
        } catch (IllegalStateException e) {
            throw new InsufficientBalanceException();
        }
    }
}
