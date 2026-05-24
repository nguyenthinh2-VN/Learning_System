package com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity;

import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "vouchers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voucher_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_voucher_status_validto", columnList = "status, valid_to")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class VoucherJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoucherType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoucherStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VoucherScope scope;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "min_order_amount", precision = 19, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    private Long usageLimit = 0L;

    @Column(name = "usage_per_user")
    private Integer usagePerUser = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "voucher_courses",
            joinColumns = @JoinColumn(name = "voucher_id"),
            indexes = @Index(name = "idx_voucher_courses_voucher", columnList = "voucher_id")
    )
    @Column(name = "course_id", nullable = false)
    private Set<Long> applicableCourseIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Voucher toDomain() {
        return Voucher.reconstitute(
                this.id, this.code, this.type, this.value,
                this.status, this.scope, this.validFrom, this.validTo,
                this.minOrderAmount, this.maxDiscount,
                this.usageLimit, this.usagePerUser,
                this.applicableCourseIds,
                this.createdAt, this.updatedAt);
    }

    public static VoucherJpaEntity fromDomain(Voucher v) {
        VoucherJpaEntity e = new VoucherJpaEntity();
        e.id = v.getId();
        e.code = v.getCode();
        e.type = v.getType();
        e.value = v.getValue();
        e.status = v.getStatus();
        e.scope = v.getScope();
        e.validFrom = v.getValidFrom();
        e.validTo = v.getValidTo();
        e.minOrderAmount = v.getMinOrderAmount();
        e.maxDiscount = v.getMaxDiscount();
        e.usageLimit = v.getUsageLimit();
        e.usagePerUser = v.getUsagePerUser();
        e.applicableCourseIds = v.getApplicableCourseIds() != null
                ? new HashSet<>(v.getApplicableCourseIds()) : new HashSet<>();
        e.createdAt = v.getCreatedAt();
        e.updatedAt = v.getUpdatedAt();
        return e;
    }
}
