package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity.VoucherJpaEntity;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Voucher.VoucherRepository;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VoucherRepositoryImpl implements VoucherRepository {

    private final JpaVoucherRepository jpa;

    @Override
    public Voucher save(Voucher voucher) {
        VoucherJpaEntity entity = VoucherJpaEntity.fromDomain(voucher);
        VoucherJpaEntity saved = jpa.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Voucher> findById(Long id) {
        return jpa.findById(id).map(VoucherJpaEntity::toDomain);
    }

    @Override
    public Optional<Voucher> findByCode(String normalizedCode) {
        return jpa.findByCode(normalizedCode).map(VoucherJpaEntity::toDomain);
    }

    @Override
    public Optional<Voucher> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(VoucherJpaEntity::toDomain);
    }

    @Override
    public Optional<Voucher> findByCodeForUpdate(String normalizedCode) {
        return jpa.findByCodeForUpdate(normalizedCode).map(VoucherJpaEntity::toDomain);
    }

    @Override
    public boolean existsByCode(String normalizedCode) {
        return jpa.existsByCode(normalizedCode);
    }

    @Override
    public PageResult<Voucher> findAll(int page, int size) {
        Page<VoucherJpaEntity> pageEntity = jpa.findAll(PageRequest.of(page, size));
        List<Voucher> items = pageEntity.getContent().stream().map(VoucherJpaEntity::toDomain).toList();
        return PageResult.of(
                pageEntity.getTotalElements(),
                pageEntity.getTotalPages(),
                pageEntity.getNumber(),
                pageEntity.getSize(),
                items);
    }
}
