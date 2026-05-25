package com.example.learning_system_spring.adapter.dto.request.Voucher;

import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UpdateVoucherRequest {

    // ── Immutable fields (nullable — null = giữ nguyên, non-null = muốn sửa) ──
    // Chỉ được sửa khi voucher chưa có usage; UseCase sẽ chặn nếu đã có.

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$",
             message = "code chỉ được chứa chữ cái, số, dấu gạch dưới/ngang, tối đa 32 ký tự")
    private String code;

    private VoucherType type;

    @Positive(message = "value phải > 0")
    private BigDecimal value;

    // ── Soft fields (bắt buộc) ──

    @NotNull
    private VoucherStatus status;

    @NotNull
    private VoucherScope scope;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;

    @DecimalMin(value = "0.0")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0")
    private BigDecimal maxDiscount;

    private Long usageLimit;

    private Integer usagePerUser;

    private Set<Long> applicableCourseIds;
}
