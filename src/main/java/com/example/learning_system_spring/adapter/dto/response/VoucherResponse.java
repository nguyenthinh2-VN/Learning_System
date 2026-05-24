package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private String type;
    private BigDecimal value;
    private String status;
    private String scope;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Long usageLimit;
    private Integer usagePerUser;
    private Set<Long> applicableCourseIds;
    private long usedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VoucherResponse from(VoucherOutput o) {
        if (o == null) return null;
        return VoucherResponse.builder()
                .id(o.id())
                .code(o.code())
                .type(o.type() != null ? o.type().name() : null)
                .value(o.value())
                .status(o.status() != null ? o.status().name() : null)
                .scope(o.scope() != null ? o.scope().name() : null)
                .validFrom(o.validFrom())
                .validTo(o.validTo())
                .minOrderAmount(o.minOrderAmount())
                .maxDiscount(o.maxDiscount())
                .usageLimit(o.usageLimit())
                .usagePerUser(o.usagePerUser())
                .applicableCourseIds(o.applicableCourseIds())
                .usedCount(o.usedCount())
                .createdAt(o.createdAt())
                .updatedAt(o.updatedAt())
                .build();
    }
}
