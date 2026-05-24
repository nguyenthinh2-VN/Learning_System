package com.example.learning_system_spring.application.dto.Voucher;

import com.example.learning_system_spring.domain.model.Voucher.PriceQuote;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuotePricingOutput(
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        boolean voucherApplied,
        String voucherCode,
        VoucherType voucherType,
        boolean internalDiscount,
        LocalDateTime quotedAt
) {
    public static QuotePricingOutput from(PriceQuote quote, boolean internalDiscount) {
        return new QuotePricingOutput(
                quote.originalPrice(),
                quote.discountAmount(),
                quote.finalPrice(),
                quote.voucherApplied(),
                quote.voucherCode(),
                quote.voucherType(),
                internalDiscount,
                LocalDateTime.now()
        );
    }
}
