package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity.VoucherUsageJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaVoucherUsageRepository extends JpaRepository<VoucherUsageJpaEntity, Long> {

    long countByVoucherId(Long voucherId);

    long countByVoucherIdAndUserId(Long voucherId, Long userId);

    Page<VoucherUsageJpaEntity> findByVoucherId(Long voucherId, Pageable pageable);
}
