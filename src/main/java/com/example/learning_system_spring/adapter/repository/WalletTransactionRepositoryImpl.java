package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.WalletEntity.WalletTransactionJpaEntity;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.repository.Wallet.WalletTransactionRepository;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import com.example.learning_system_spring.domain.model.Wallet.WalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    private final JpaWalletTransactionRepository jpaRepo;

    @Override
    public WalletTransaction save(WalletTransaction tx) {
        WalletTransactionJpaEntity entity = WalletTransactionJpaEntity.fromDomain(tx);
        return jpaRepo.save(entity).toDomain();
    }

    @Override
    public Optional<WalletTransaction> findByReferenceCode(String referenceCode) {
        return jpaRepo.findByReferenceCode(referenceCode)
                .map(WalletTransactionJpaEntity::toDomain);
    }

    @Override
    public Optional<WalletTransaction> findPendingByRefForUpdate(String referenceCode) {
        return jpaRepo.findPendingByRefForUpdate(referenceCode, TxStatus.PENDING)
                .map(WalletTransactionJpaEntity::toDomain);
    }

    @Override
    public PageResult<WalletTransaction> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransactionJpaEntity> jpaPage = jpaRepo.findByUserId(userId, pageable);

        List<WalletTransaction> items = jpaPage.getContent().stream()
                .map(WalletTransactionJpaEntity::toDomain)
                .toList();

        return PageResult.of(
                jpaPage.getTotalElements(),
                jpaPage.getTotalPages(),
                page,
                size,
                items);
    }
}
