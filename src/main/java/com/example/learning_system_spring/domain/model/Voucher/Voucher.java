package com.example.learning_system_spring.domain.model.Voucher;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Voucher {
    private Long id;
    private String code;
    private VoucherType type;
    private BigDecimal value;
    private VoucherStatus status;
    private VoucherScope scope;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Long usageLimit;
    private Integer usagePerUser;
    @Builder.Default
    private Set<Long> applicableCourseIds = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Voucher create(String code, VoucherType type, BigDecimal value,
                                 VoucherScope scope, LocalDateTime validFrom, LocalDateTime validTo,
                                 BigDecimal minOrderAmount, BigDecimal maxDiscount,
                                 Long usageLimit, Integer usagePerUser, Set<Long> applicableCourseIds) {
        validateNew(code, type, value, scope, validFrom, validTo, applicableCourseIds);

        LocalDateTime now = LocalDateTime.now();
        return Voucher.builder()
                .code(normalizeCode(code))
                .type(type)
                .value(value)
                .status(VoucherStatus.ACTIVE)
                .scope(scope)
                .validFrom(validFrom)
                .validTo(validTo)
                .minOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO)
                .maxDiscount(maxDiscount != null ? maxDiscount : BigDecimal.ZERO)
                .usageLimit(usageLimit != null ? usageLimit : 0L)
                .usagePerUser(usagePerUser != null ? usagePerUser : 0)
                .applicableCourseIds(applicableCourseIds != null ? new HashSet<>(applicableCourseIds) : new HashSet<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Voucher reconstitute(Long id, String code, VoucherType type, BigDecimal value,
                                       VoucherStatus status, VoucherScope scope,
                                       LocalDateTime validFrom, LocalDateTime validTo,
                                       BigDecimal minOrderAmount, BigDecimal maxDiscount,
                                       Long usageLimit, Integer usagePerUser,
                                       Set<Long> applicableCourseIds,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return Voucher.builder()
                .id(id)
                .code(code)
                .type(type)
                .value(value)
                .status(status)
                .scope(scope)
                .validFrom(validFrom)
                .validTo(validTo)
                .minOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO)
                .maxDiscount(maxDiscount != null ? maxDiscount : BigDecimal.ZERO)
                .usageLimit(usageLimit != null ? usageLimit : 0L)
                .usagePerUser(usagePerUser != null ? usagePerUser : 0)
                .applicableCourseIds(applicableCourseIds != null ? new HashSet<>(applicableCourseIds) : new HashSet<>())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static String normalizeCode(String code) {
        if (code == null) return null;
        return code.trim().toUpperCase();
    }

    private static void validateNew(String code, VoucherType type, BigDecimal value,
                                    VoucherScope scope, LocalDateTime validFrom, LocalDateTime validTo,
                                    Set<Long> applicableCourseIds) {
        if (code == null || code.trim().isEmpty())
            throw new IllegalArgumentException("Voucher code không được để trống");
        if (type == null) throw new IllegalArgumentException("Voucher type không được null");
        if (value == null || value.signum() <= 0)
            throw new IllegalArgumentException("Voucher value phải > 0");
        if (validFrom == null || validTo == null)
            throw new IllegalArgumentException("validFrom và validTo bắt buộc");
        if (validFrom.isAfter(validTo))
            throw new IllegalArgumentException("validFrom phải <= validTo");
        if (scope == null) throw new IllegalArgumentException("scope không được null");
        if (scope == VoucherScope.SPECIFIC_COURSES
                && (applicableCourseIds == null || applicableCourseIds.isEmpty()))
            throw new IllegalArgumentException("Scope SPECIFIC_COURSES yêu cầu applicableCourseIds không rỗng");
        if (scope == VoucherScope.ALL_COURSES
                && applicableCourseIds != null && !applicableCourseIds.isEmpty())
            throw new IllegalArgumentException("Scope ALL_COURSES không được kèm applicableCourseIds");
    }

    public boolean isActive() { return status == VoucherStatus.ACTIVE; }
    public boolean isInactive() { return status == VoucherStatus.INACTIVE; }

    public void deactivate() {
        this.status = VoucherStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cập nhật các field "soft" — không thay đổi code / type / value.
     * Việc cấm thay đổi 3 field đó khi đã có usage được kiểm soát ở UseCase.
     */
    public void updateSoftFields(VoucherStatus newStatus, LocalDateTime newValidFrom, LocalDateTime newValidTo,
                                 BigDecimal newMinOrderAmount, BigDecimal newMaxDiscount,
                                 Long newUsageLimit, Integer newUsagePerUser,
                                 VoucherScope newScope, Set<Long> newApplicableCourseIds) {
        if (newValidFrom == null || newValidTo == null)
            throw new IllegalArgumentException("validFrom và validTo bắt buộc");
        if (newValidFrom.isAfter(newValidTo))
            throw new IllegalArgumentException("validFrom phải <= validTo");
        if (newScope == VoucherScope.SPECIFIC_COURSES
                && (newApplicableCourseIds == null || newApplicableCourseIds.isEmpty()))
            throw new IllegalArgumentException("Scope SPECIFIC_COURSES yêu cầu applicableCourseIds không rỗng");
        if (newScope == VoucherScope.ALL_COURSES
                && newApplicableCourseIds != null && !newApplicableCourseIds.isEmpty())
            throw new IllegalArgumentException("Scope ALL_COURSES không được kèm applicableCourseIds");

        this.status = newStatus;
        this.validFrom = newValidFrom;
        this.validTo = newValidTo;
        this.minOrderAmount = newMinOrderAmount != null ? newMinOrderAmount : BigDecimal.ZERO;
        this.maxDiscount = newMaxDiscount != null ? newMaxDiscount : BigDecimal.ZERO;
        this.usageLimit = newUsageLimit != null ? newUsageLimit : 0L;
        this.usagePerUser = newUsagePerUser != null ? newUsagePerUser : 0;
        this.scope = newScope;
        this.applicableCourseIds = newApplicableCourseIds != null
                ? new HashSet<>(newApplicableCourseIds) : new HashSet<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Áp dụng cho course này không? Dùng cho VoucherValidator sau khi đã pass status/time check.
     */
    public boolean appliesTo(Long courseId) {
        if (scope == VoucherScope.ALL_COURSES) return true;
        return applicableCourseIds.contains(courseId);
    }
}
