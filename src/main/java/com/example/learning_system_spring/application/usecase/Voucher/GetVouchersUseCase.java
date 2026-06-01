package com.example.learning_system_spring.application.usecase.Voucher;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Voucher.GetVouchersInput;
import com.example.learning_system_spring.application.dto.Voucher.VoucherOutput;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
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
        // Authz: enforce ở controller bằng @PreAuthorize("hasAuthority('MANAGE_VOUCHER')").

        PageResult<Voucher> page = voucherRepository.findAll(input.page(), input.size());
        List<VoucherOutput> items = page.items().stream()
                .map(v -> VoucherOutput.from(v, voucherUsageRepository.countByVoucherId(v.getId())))
                .toList();
        return PageResult.of(page.totalElements(), page.totalPages(), page.page(), page.size(), items);
    }
}
