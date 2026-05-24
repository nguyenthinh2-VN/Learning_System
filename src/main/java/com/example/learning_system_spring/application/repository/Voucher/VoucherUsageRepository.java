package com.example.learning_system_spring.application.repository.Voucher;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;

public interface VoucherUsageRepository {

    VoucherUsage save(VoucherUsage usage);

    long countByVoucherId(Long voucherId);

    long countByVoucherIdAndUserId(Long voucherId, Long userId);

    PageResult<VoucherUsage> findByVoucherId(Long voucherId, int page, int size);
}
