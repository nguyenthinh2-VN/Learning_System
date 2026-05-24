package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.CreateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherCodeAlreadyExistsException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherType;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreateVoucherUseCase {

    private final VoucherRepository voucherRepository;

    @Transactional
    public VoucherOutput execute(CreateVoucherInput input) {
        if (!CourseOwnershipPolicy.hasFullAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền tạo voucher.");
        }

        // Validate phần trăm phải <= 100
        if (input.type() == VoucherType.PERCENT
                && input.value() != null
                && input.value().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Voucher PERCENT phải có giá trị 0 < value <= 100");
        }

        String normalizedCode = Voucher.normalizeCode(input.code());
        if (voucherRepository.existsByCode(normalizedCode)) {
            throw new VoucherCodeAlreadyExistsException(normalizedCode);
        }

        Voucher voucher = Voucher.create(
                input.code(), input.type(), input.value(),
                input.scope(), input.validFrom(), input.validTo(),
                input.minOrderAmount(), input.maxDiscount(),
                input.usageLimit(), input.usagePerUser(),
                input.applicableCourseIds());

        Voucher saved = voucherRepository.save(voucher);
        return VoucherOutput.from(saved, 0L);
    }
}
