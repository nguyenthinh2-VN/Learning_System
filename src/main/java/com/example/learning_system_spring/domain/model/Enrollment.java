package com.example.learning_system_spring.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Enrollment {
    private Long id;
    private Long userId;
    private Long courseId;
    private BigDecimal paidPrice;
    private LocalDateTime enrolledAt;

    public static Enrollment create(Long userId, Long courseId, BigDecimal paidPrice) {
        if (userId == null || courseId == null) {
            throw new IllegalArgumentException("User ID and Course ID must not be null");
        }
        if (paidPrice == null || paidPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid price must not be negative");
        }
        return Enrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .paidPrice(paidPrice)
                .enrolledAt(LocalDateTime.now())
                .build();
    }

    public static Enrollment reconstitute(Long id, Long userId, Long courseId, BigDecimal paidPrice, LocalDateTime enrolledAt) {
        return Enrollment.builder()
                .id(id)
                .userId(userId)
                .courseId(courseId)
                .paidPrice(paidPrice)
                .enrolledAt(enrolledAt)
                .build();
    }
}
