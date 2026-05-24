package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity.VoucherUsageJpaEntity;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Voucher.VoucherUsageRepository;
import com.example.learning_system_spring.domain.model.Voucher.VoucherUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VoucherUsageRepositoryImpl implements VoucherUsageRepository {

    private final JpaVoucherUsageRepository jpa;

    @Override
    public VoucherUsage save(VoucherUsage usage) {
        VoucherUsageJpaEntity entity = VoucherUsageJpaEntity.fromDomain(usage);
        VoucherUsageJpaEntity saved = jpa.save(entity);
        return saved.toDomain();
    }

    @Override
    public long countByVoucherId(Long voucherId) {
        return jpa.countByVoucherId(voucherId);
    }

    @Override
    public long countByVoucherIdAndUserId(Long voucherId, Long userId) {
        return jpa.countByVoucherIdAndUserId(voucherId, userId);
    }

    @Override
    public PageResult<VoucherUsage> findByVoucherId(Long voucherId, int page, int size) {
        Page<VoucherUsageJpaEntity> p = jpa.findByVoucherId(voucherId, PageRequest.of(page, size));
        List<VoucherUsage> items = p.getContent().stream().map(VoucherUsageJpaEntity::toDomain).toList();
        return PageResult.of(p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize(), items);
    }
}
