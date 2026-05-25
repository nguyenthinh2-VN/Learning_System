package com.example.learning_system_spring.domain.service;

import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain service: tính giá khóa học sau khi áp voucher.
 * KHÔNG phụ thuộc Spring, JPA, hay framework nào — class thuần Java.
 *
 * Invariants được enforce ở mọi đầu vào hợp lệ:
 *   - 0 <= discountAmount <= originalPrice
 *   - finalPrice = originalPrice - discountAmount
 *   - 0 <= finalPrice <= originalPrice
 *   - Mọi BigDecimal có scale = 2, rounding = HALF_UP
 *
 * Pure function: cùng đầu vào (originalPrice, voucher snapshot) → cùng kết quả.
 * Không phụ thuộc thời gian, random, hay state ngoài.
 */
public class PricingEngine {

    public static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public PriceQuote compute(BigDecimal originalPrice, Voucher voucher) {
        if (originalPrice == null) {
            throw new IllegalArgumentException("originalPrice must not be null");
        }
        if (originalPrice.signum() < 0) {
            throw new IllegalArgumentException("originalPrice must be >= 0");
        }

        BigDecimal normalizedOriginal = originalPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // Không có voucher → không discount.
        if (voucher == null) {
            return PriceQuote.noDiscount(normalizedOriginal);
        }

        // Khóa học miễn phí thì voucher không có ý nghĩa.
        if (normalizedOriginal.signum() == 0) {
            return PriceQuote.noDiscount(normalizedOriginal);
        }

        BigDecimal discount = computeDiscount(normalizedOriginal, voucher);

        // Đảm bảo discount không vượt originalPrice (paranoia).
        if (discount.compareTo(normalizedOriginal) > 0) {
            discount = normalizedOriginal;
        }

        // FIX-D: Precision-loss guard — nếu discount = 0 sau làm tròn HALF_UP,
        //   coi như voucher không áp dụng được (Option A).
        //   Tránh trạng thái voucherApplied=true nhưng discount=0 gây nhầm lẫn cho user
        //   và tiêu thụ lượt dùng voucher mà không mang lại lợi ích.
        if (discount.signum() == 0) {
            return PriceQuote.noDiscount(normalizedOriginal);
        }

        return PriceQuote.withVoucher(normalizedOriginal, discount, voucher.getCode(), voucher.getType());
    }

    private BigDecimal computeDiscount(BigDecimal originalPrice, Voucher voucher) {
        if (voucher.getType() == VoucherType.PERCENT) {
            BigDecimal raw = originalPrice
                    .multiply(voucher.getValue())
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal maxDiscount = voucher.getMaxDiscount();
            if (maxDiscount != null && maxDiscount.signum() > 0) {
                return raw.min(maxDiscount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            return raw;
        }
        // FIXED
        return voucher.getValue().min(originalPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
