package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Voucher.GetVouchersInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.exception.CourseAccessDeniedException;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import com.example.learning_system_spring.domain.service.CourseOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetVouchersUseCase {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional(readOnly = true)
    public PageResult<VoucherOutput> execute(GetVouchersInput input) {
        if (input.requesterRole() == null || !CourseOwnershipPolicy.hasFullAccess(input.requesterRole())) {
            throw new CourseAccessDeniedException("Bạn không có quyền xem danh sách voucher.");
        }

        PageResult<Voucher> page = voucherRepository.findAll(input.page(), input.size());
        List<VoucherOutput> items = page.items().stream()
                .map(v -> VoucherOutput.from(v, voucherUsageRepository.countByVoucherId(v.getId())))
                .toList();
        return PageResult.of(page.totalElements(), page.totalPages(), page.page(), page.size(), items);
    }
}
