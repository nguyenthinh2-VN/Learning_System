package com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity;

import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "voucher_usages",
        uniqueConstraints = {
                // Một enrollment chỉ tiêu thụ tối đa 1 lượt voucher (chống race condition).
                @UniqueConstraint(name = "uk_voucher_enrollment", columnNames = {"voucher_id", "enrollment_id"})
        },
        indexes = {
                @Index(name = "idx_voucher_usage_voucher", columnList = "voucher_id"),
                @Index(name = "idx_voucher_usage_user", columnList = "user_id"),
                @Index(name = "idx_voucher_usage_voucher_user", columnList = "voucher_id, user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class VoucherUsageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "original_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public VoucherUsage toDomain() {
        return VoucherUsage.reconstitute(
                this.id, this.voucherId, this.userId, this.courseId, this.enrollmentId,
                this.originalPrice, this.discountAmount, this.finalPrice, this.appliedAt);
    }

    public static VoucherUsageJpaEntity fromDomain(VoucherUsage u) {
        VoucherUsageJpaEntity e = new VoucherUsageJpaEntity();
        e.id = u.getId();
        e.voucherId = u.getVoucherId();
        e.userId = u.getUserId();
        e.courseId = u.getCourseId();
        e.enrollmentId = u.getEnrollmentId();
        e.originalPrice = u.getOriginalPrice();
        e.discountAmount = u.getDiscountAmount();
        e.finalPrice = u.getFinalPrice();
        e.appliedAt = u.getAppliedAt();
        return e;
    }
}
