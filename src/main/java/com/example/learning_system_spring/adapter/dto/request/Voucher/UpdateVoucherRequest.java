package com.example.learning_system_spring.adapter.dto.request.Voucher;

import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
