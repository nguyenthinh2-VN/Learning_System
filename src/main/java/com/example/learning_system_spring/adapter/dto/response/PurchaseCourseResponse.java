package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Voucher.PurchaseCourseOutput;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PurchaseCourseResponse {
    private Long enrollmentId;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private BigDecimal paidPrice;
    private boolean voucherApplied;
    private String voucherCode;

    public static PurchaseCourseResponse from(PurchaseCourseOutput o) {
        return PurchaseCourseResponse.builder()
                .enrollmentId(o.enrollmentId())
                .originalPrice(o.originalPrice())
                .discountAmount(o.discountAmount())
                .finalPrice(o.finalPrice())
                .paidPrice(o.paidPrice())
                .voucherApplied(o.voucherApplied())
                .voucherCode(o.voucherCode())
                .build();
    }
}
