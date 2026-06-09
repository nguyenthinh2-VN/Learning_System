package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.Voucher.DeleteVoucherInput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.domain.exception.VoucherNotFoundException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
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
        // Authz: enforce ở controller bằng @PreAuthorize("hasAuthority('MANAGE_VOUCHER')").
        Voucher voucher = voucherRepository.findById(input.voucherId())
                .orElseThrow(() -> new VoucherNotFoundException(input.voucherId()));
        voucher.deactivate();
        voucherRepository.save(voucher);
    }
}
