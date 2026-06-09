package com.example.learning_system_spring.application.usecase.Cart;

import com.example.learning_system_spring.application.dto.Cart.BulkCheckoutInput;
import com.example.learning_system_spring.application.dto.Cart.BulkCheckoutOutput;
import com.example.learning_system_spring.application.repository.Cart.CartItemRepository;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Course.EnrollmentRepository;
import com.example.learning_system_spring.application.repository.User.UserRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.exception.InsufficientBalanceException;
import com.example.learning_system_spring.domain.exception.UserNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherNotApplicableException;
import com.example.learning_system_spring.domain.exception.VoucherUseDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Enrollment;
import com.example.learning_system_spring.domain.model.User;
import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import com.example.learning_system_spring.infrastructure.service.PurchaseLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulkCheckoutUseCase {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CartItemRepository cartItemRepository;
    private final PricingEngine pricingEngine;
    private final VoucherValidator voucherValidator;
    private final PurchaseLedgerService purchaseLedgerService;

    @Transactional
    public BulkCheckoutOutput execute(BulkCheckoutInput input) {
        Long userId = input.requesterId();
        
        // 1. Lọc duplicate và sort courseIds để tránh deadlock
        List<Long> sortedCourseIds = input.courseIds().stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
                
        if (sortedCourseIds.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        // 2. Lock User (PESSIMISTIC_WRITE)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. Lock Courses theo thứ tự đã sort
        List<Course> lockedCourses = courseRepository.findByIdInOrderByIdForUpdate(sortedCourseIds);

        List<Course> coursesToBuy = new ArrayList<>();
        List<Long> skippedCourseIds = new ArrayList<>();
        BigDecimal originalTotalPrice = BigDecimal.ZERO;

        for (Course course : lockedCourses) {
            // Bỏ qua course chưa publish
            if (!course.isPublished()) {
                skippedCourseIds.add(course.getId());
                continue;
            }
            // Bỏ qua course đã full
            if (course.isFull()) {
                skippedCourseIds.add(course.getId());
                continue;
            }
            // Bỏ qua course đã enroll (không throw exception)
            if (enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                skippedCourseIds.add(course.getId());
                continue;
            }
            
            coursesToBuy.add(course);
            BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            
            // Internal member & course is free for internal
            if (input.isInternal() && course.isFreeForInternal()) {
                // Free course, price adds 0
            } else {
                originalTotalPrice = originalTotalPrice.add(coursePrice);
            }
        }

        if (coursesToBuy.isEmpty()) {
            // Tất cả khóa học đều đã được mua, full hoặc ẩn
            return new BulkCheckoutOutput(
                    Collections.emptyList(), skippedCourseIds,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    false, null
            );
        }

        boolean hasVoucher = input.voucherCode() != null && !input.voucherCode().trim().isEmpty();
        if (hasVoucher && input.requesterRole() != null && !input.requesterRole().isMember()
                && !input.requesterRole().isSuperAdmin()) {
            throw new VoucherUseDeniedException("Role của bạn không được phép sử dụng voucher.");
        }

        if (!hasVoucher) {
            return processWithoutVoucher(user, coursesToBuy, skippedCourseIds, originalTotalPrice, userId);
        }

        return processWithVoucher(user, coursesToBuy, skippedCourseIds, originalTotalPrice, userId, input.voucherCode());
    }

    private BulkCheckoutOutput processWithoutVoucher(User user, List<Course> coursesToBuy, List<Long> skippedCourseIds,
                                                     BigDecimal originalTotalPrice, Long userId) {
        BigDecimal paidPrice = originalTotalPrice;
        deductBalanceOrThrow(user, paidPrice);
        
        List<Long> enrolledCourseIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Course course : coursesToBuy) {
            course.enroll();
            courseRepository.save(course);
            
            BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            // Xử lý giá nội bộ
            if (coursePrice.compareTo(BigDecimal.ZERO) > 0 && 
                user.isInternal() && course.isFreeForInternal()) {
                coursePrice = BigDecimal.ZERO;
            }
            
            Enrollment enrollment = enrollmentRepository.save(
                    Enrollment.create(userId, course.getId(), coursePrice));
            enrolledCourseIds.add(course.getId());
            purchaseLedgerService.logPurchase(userId, course.getId(), coursePrice, enrollment.getEnrolledAt());
        }

        userRepository.save(user);
        
        if (paidPrice.signum() > 0) {
            walletTransactionRepository.save(
                    WalletTransaction.createPurchase(userId, paidPrice,
                            "Mua hàng loạt " + enrolledCourseIds.size() + " khóa học"));
        }
        
        // Clear khỏi giỏ hàng
        cartItemRepository.deleteByUserIdAndCourseIdIn(userId, enrolledCourseIds);

        return new BulkCheckoutOutput(
                enrolledCourseIds, skippedCourseIds,
                originalTotalPrice, BigDecimal.ZERO, originalTotalPrice, paidPrice,
                false, null
        );
    }

    private BulkCheckoutOutput processWithVoucher(User user, List<Course> coursesToBuy, List<Long> skippedCourseIds,
                                                  BigDecimal originalTotalPrice, Long userId, String voucherCode) {
        // [1] Lock Voucher
        String normalizedCode = Voucher.normalizeCode(voucherCode);
        Voucher lockedVoucher = voucherRepository.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> new VoucherNotFoundException(normalizedCode));

        Long globalUsed = voucherUsageRepository.countByVoucherId(lockedVoucher.getId());
        Long perUserUsed = voucherUsageRepository.countByVoucherIdAndUserId(lockedVoucher.getId(), userId);

        // Tìm 1 khóa học hợp lệ để pass qua VoucherValidator (hack để áp dụng cho tổng)
        Long applicableCourseId = null;
        for (Course course : coursesToBuy) {
            if (lockedVoucher.appliesTo(course.getId())) {
                applicableCourseId = course.getId();
                break;
            }
        }
        if (applicableCourseId == null) {
            throw new VoucherNotApplicableException(lockedVoucher.getCode(), null);
        }

        // Validate Voucher
        voucherValidator.validate(lockedVoucher, applicableCourseId, originalTotalPrice,
                LocalDateTime.now(), globalUsed, perUserUsed);

        // Tính giá
        PriceQuote quote = pricingEngine.compute(originalTotalPrice, lockedVoucher);
        BigDecimal paidPrice = quote.finalPrice();

        deductBalanceOrThrow(user, paidPrice);

        List<Long> enrolledCourseIds = new ArrayList<>();
        List<Enrollment> createdEnrollments = new ArrayList<>();
        
        for (Course course : coursesToBuy) {
            course.enroll();
            courseRepository.save(course);
            
            // Tạm chia giá (nếu cần tracking chi tiết, ở đây lưu paidPrice cho Enrollment = giá trị gốc để không phá logic cũ)
            // Hoặc lưu finalPrice / N. Nhưng BulkCheckout thì ta trừ 1 lần, Enrollment lưu course price gốc hoặc 0.
            // Để đơn giản, ta lưu Enrollment giá gốc nhưng Wallet transaction lưu tổng đã giảm.
            BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            if (user.isInternal() && course.isFreeForInternal()) {
                coursePrice = BigDecimal.ZERO;
            }
            
            Enrollment enrollment = enrollmentRepository.save(
                    Enrollment.create(userId, course.getId(), coursePrice));
            enrolledCourseIds.add(course.getId());
            createdEnrollments.add(enrollment);
            
            // Log ledger (chúng ta pass toàn bộ discount vào ledger của khóa học đầu tiên, hoặc tỉ lệ. 
            // PurchaseLedgerService hỗ trợ log từng course, ta log như bình thường)
            purchaseLedgerService.logPurchase(userId, course.getId(), coursePrice, enrollment.getEnrolledAt());
        }

        userRepository.save(user);

        // Tạo 1 VoucherUsage duy nhất đại diện cho cả giỏ hàng (courseId = null hoặc applicableCourseId)
        VoucherUsage usage = VoucherUsage.create(
                lockedVoucher.getId(), userId, applicableCourseId, createdEnrollments.get(0).getId(),
                quote.originalPrice(), quote.discountAmount(), quote.finalPrice());
        voucherUsageRepository.save(usage);

        if (paidPrice.signum() > 0) {
            walletTransactionRepository.save(
                    WalletTransaction.createPurchase(userId, paidPrice,
                            "Mua hàng loạt " + enrolledCourseIds.size() + " khóa học (voucher " + lockedVoucher.getCode() + ")"));
        }

        // Clear khỏi giỏ hàng
        cartItemRepository.deleteByUserIdAndCourseIdIn(userId, enrolledCourseIds);

        return new BulkCheckoutOutput(
                enrolledCourseIds, skippedCourseIds,
                quote.originalPrice(), quote.discountAmount(), quote.finalPrice(), paidPrice,
                true, lockedVoucher.getCode()
        );
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
