package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.QuotePricingInput;
import com.example.learning_system_spring.application.dto.Voucher.QuotePricingOutput;
import com.example.learning_system_spring.application.repository.Course.CourseRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseNotFoundException;
import com.example.learning_system_spring.domain.exception.CourseNotPublishedException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUseDeniedException;
import com.example.learning_system_spring.domain.model.Course;
import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.service.PricingEngine;
import com.example.learning_system_spring.domain.service.VoucherValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tính giá xem trước (preview) — read-only, không tiêu thụ voucher.
 *
 * Anti-tampering: server tự đọc giá từ DB theo courseId (path), KHÔNG nhận giá từ client.
 * Internal Member luôn finalPrice = 0, voucher bị ignore.
 * Role không phải MEMBER + có voucherCode → 403 VOUCHER_USE_DENIED.
 */
@Service
@RequiredArgsConstructor
public class QuotePricingUseCase {

    private final CourseRepository courseRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final PricingEngine pricingEngine;
    private final VoucherValidator voucherValidator;

    @Transactional(readOnly = true)
    public QuotePricingOutput execute(QuotePricingInput input) {
        Course course = courseRepository.findById(input.courseId())
                .orElseThrow(() -> new CourseNotFoundException(input.courseId()));

        if (!course.isPublished()) {
            throw new CourseNotPublishedException(input.courseId());
        }

        BigDecimal originalPrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;

        // Internal member → luôn 0đ, bỏ qua voucher.
        if (input.isInternal()) {
            PriceQuote quote = new PriceQuote(originalPrice, originalPrice, BigDecimal.ZERO, false, null, null);
            return QuotePricingOutput.from(quote, true);
        }

        boolean hasVoucher = input.voucherCode() != null && !input.voucherCode().trim().isEmpty();

        // Role không phải MEMBER mà gửi voucher → 403.
        if (hasVoucher && input.requesterRole() != null && !input.requesterRole().isMember()
                && !input.requesterRole().isSuperAdmin()) {
            throw new VoucherUseDeniedException("Role của bạn không được phép sử dụng voucher.");
        }

        if (!hasVoucher) {
            return QuotePricingOutput.from(pricingEngine.compute(originalPrice, null), false);
        }

        // Voucher path
        String normalizedCode = Voucher.normalizeCode(input.voucherCode());
        Voucher voucher = voucherRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new VoucherNotFoundException(normalizedCode));

        long globalUsed = voucherUsageRepository.countByVoucherId(voucher.getId());
        long perUserUsed = input.requesterId() != null
                ? voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), input.requesterId())
                : 0L;

        voucherValidator.validate(voucher, input.courseId(), originalPrice,
                LocalDateTime.now(), globalUsed, perUserUsed);

        PriceQuote quote = pricingEngine.compute(originalPrice, voucher);
        return QuotePricingOutput.from(quote, false);
    }
}
