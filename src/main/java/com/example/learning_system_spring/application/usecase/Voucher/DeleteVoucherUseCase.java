package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.DeleteVoucherInput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-delete voucher: set status = INACTIVE. Bảo toàn lịch sử Voucher_Usage.
 */
@Service
@RequiredArgsConstructor
public class DeleteVoucherUseCase {

    private final VoucherRepository voucherRepository;

    @Transactional
    public void execute(DeleteVoucherInput input) {
        if (!CourseOwnershipPolicy.hasFullAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền xóa voucher.");
        }
        Voucher voucher = voucherRepository.findById(input.voucherId())
                .orElseThrow(() -> new VoucherNotFoundException(input.voucherId()));
        voucher.deactivate();
        voucherRepository.save(voucher);
    }
}
