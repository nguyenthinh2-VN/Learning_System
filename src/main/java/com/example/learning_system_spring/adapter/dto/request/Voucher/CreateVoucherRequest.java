package com.example.learning_system_spring.adapter.dto.request.Voucher;

import com.example.learning_system_spring.domain.model.Voucher.VoucherScope;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 4, max = 32, message = "Mã voucher phải có độ dài 4-32 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã voucher chỉ chứa A-Z, a-z, 0-9, _, -")
    private String code;

    @NotNull(message = "Loại voucher không được để trống")
    private VoucherType type;

    @NotNull(message = "Giá trị voucher không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị voucher phải > 0")
    private BigDecimal value;

    @NotNull(message = "Scope không được để trống")
    private VoucherScope scope;

    @NotNull(message = "validFrom không được để trống")
    private LocalDateTime validFrom;

    @NotNull(message = "validTo không được để trống")
    private LocalDateTime validTo;

    @DecimalMin(value = "0.0", message = "minOrderAmount phải >= 0")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "maxDiscount phải >= 0")
    private BigDecimal maxDiscount;

    private Long usageLimit;

    private Integer usagePerUser;

    private Set<Long> applicableCourseIds;
}
