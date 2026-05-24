package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Voucher.QuotePricingOutput;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class QuotePricingResponse {
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private boolean voucherApplied;
    private String voucherCode;
    private String voucherType;
    private boolean internalDiscount;
    private LocalDateTime quotedAt;

    public static QuotePricingResponse from(QuotePricingOutput o) {
        if (o == null) return null;
        return QuotePricingResponse.builder()
                .originalPrice(o.originalPrice())
                .discountAmount(o.discountAmount())
                .finalPrice(o.finalPrice())
                .voucherApplied(o.voucherApplied())
                .voucherCode(o.voucherCode())
                .voucherType(o.voucherType() != null ? o.voucherType().name() : null)
                .internalDiscount(o.internalDiscount())
                .quotedAt(o.quotedAt())
                .build();
    }
}
