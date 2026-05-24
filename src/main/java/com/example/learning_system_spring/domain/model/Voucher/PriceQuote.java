package com.example.learning_system_spring.domain.model.Voucher;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object kết quả định giá. Bất biến (immutable).
 * Các invariant:
 *   - 0 <= discountAmount <= originalPrice
 *   - finalPrice = originalPrice - discountAmount
 *   - 0 <= finalPrice <= originalPrice
 */
public record PriceQuote(
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        boolean voucherApplied,
        String voucherCode,
        VoucherType voucherType
) {
    public static final int MONEY_SCALE = 2;

    public PriceQuote {
        if (originalPrice == null) throw new IllegalArgumentException("originalPrice must not be null");
        if (discountAmount == null) throw new IllegalArgumentException("discountAmount must not be null");
        if (finalPrice == null) throw new IllegalArgumentException("finalPrice must not be null");

        // Chuẩn hóa scale = 2 cho mọi giá trị tiền.
        originalPrice = originalPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        discountAmount = discountAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        finalPrice = finalPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // Sanity invariants.
        if (originalPrice.signum() < 0) throw new IllegalArgumentException("originalPrice must be >= 0");
        if (discountAmount.signum() < 0) throw new IllegalArgumentException("discountAmount must be >= 0");
        if (discountAmount.compareTo(originalPrice) > 0)
            throw new IllegalArgumentException("discountAmount must be <= originalPrice");
        if (finalPrice.signum() < 0) throw new IllegalArgumentException("finalPrice must be >= 0");
        if (finalPrice.compareTo(originalPrice) > 0)
            throw new IllegalArgumentException("finalPrice must be <= originalPrice");
    }

    public static PriceQuote noDiscount(BigDecimal originalPrice) {
        return new PriceQuote(originalPrice, BigDecimal.ZERO, originalPrice, false, null, null);
    }

    public static PriceQuote withVoucher(BigDecimal originalPrice, BigDecimal discountAmount,
                                         String voucherCode, VoucherType voucherType) {
        BigDecimal finalPrice = originalPrice.subtract(discountAmount);
        return new PriceQuote(originalPrice, discountAmount, finalPrice, true, voucherCode, voucherType);
    }
}
