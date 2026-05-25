package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.UpdateVoucherInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherCodeAlreadyExistsException;
import com.example.learning_system_spring.domain.exception.VoucherImmutableFieldException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.exception.VoucherUsageLimitTooLowException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.model.Voucher.VoucherStatus;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateVoucherUseCase {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional
    public VoucherOutput execute(UpdateVoucherInput input) {
        if (!CourseOwnershipPolicy.hasFullAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền sửa voucher.");
        }

        Voucher voucher = voucherRepository.findById(input.voucherId())
                .orElseThrow(() -> new VoucherNotFoundException(input.voucherId()));

        long usedCount = voucherUsageRepository.countByVoucherId(voucher.getId());

        // DEC-1: Cho sửa code/type/value khi chưa có usage; chặn khi đã có usage
        boolean wantsImmutableChange = input.newCode() != null
                || input.newType() != null
                || input.newValue() != null;

        if (wantsImmutableChange) {
            if (usedCount > 0) {
                // Xác định field đầu tiên bị vi phạm để báo lỗi rõ ràng
                String violatedField = input.newCode() != null ? "code"
                        : input.newType() != null ? "type" : "value";
                throw new VoucherImmutableFieldException(violatedField);
            }
            // Kiểm tra code mới không trùng (nếu có đổi code)
            if (input.newCode() != null) {
                String normalizedNew = Voucher.normalizeCode(input.newCode());
                if (!normalizedNew.equals(voucher.getCode())
                        && voucherRepository.existsByCode(normalizedNew)) {
                    throw new VoucherCodeAlreadyExistsException(normalizedNew);
                }
            }
            voucher.updateImmutableFields(input.newCode(), input.newType(), input.newValue());
        }

        // Nếu admin set usageLimit < usedCount (và usageLimit khác 0) → reject
        Long newLimit = input.usageLimit();
        if (newLimit != null && newLimit > 0 && newLimit < usedCount) {
            throw new VoucherUsageLimitTooLowException(usedCount, newLimit);
        }

        // updateSoftFields tự enforce ràng buộc validFrom/validTo và scope
        voucher.updateSoftFields(
                input.status(),
                input.validFrom(),
                input.validTo(),
                input.minOrderAmount(),
                input.maxDiscount(),
                input.usageLimit(),
                input.usagePerUser(),
                input.scope(),
                input.applicableCourseIds());

        Voucher saved = voucherRepository.save(voucher);
        return VoucherOutput.from(saved, usedCount);
    }
}
