package com.example.learning_system_spring.domain.model.Voucher;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VoucherUsage {
    private Long id;
    private Long voucherId;
    private Long userId;
    private Long courseId;
    private Long enrollmentId;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private LocalDateTime appliedAt;

    public static VoucherUsage create(Long voucherId, Long userId, Long courseId, Long enrollmentId,
                                      BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal finalPrice) {
        return VoucherUsage.builder()
                .voucherId(voucherId)
                .userId(userId)
                .courseId(courseId)
                .enrollmentId(enrollmentId)
                .originalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .appliedAt(LocalDateTime.now())
                .build();
    }

    public static VoucherUsage reconstitute(Long id, Long voucherId, Long userId, Long courseId, Long enrollmentId,
                                            BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal finalPrice,
                                            LocalDateTime appliedAt) {
        return VoucherUsage.builder()
                .id(id)
                .voucherId(voucherId)
                .userId(userId)
                .courseId(courseId)
                .enrollmentId(enrollmentId)
                .originalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .appliedAt(appliedAt)
                .build();
    }
}
