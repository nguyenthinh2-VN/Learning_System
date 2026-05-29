package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.WalletEntity.WalletTransactionJpaEntity;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface JpaWalletTransactionRepository extends JpaRepository<WalletTransactionJpaEntity, Long> {

    Optional<WalletTransactionJpaEntity> findByReferenceCode(String referenceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTransactionJpaEntity t WHERE t.referenceCode = :ref AND t.status = :status")
    Optional<WalletTransactionJpaEntity> findPendingByRefForUpdate(
            @Param("ref") String referenceCode,
            @Param("status") TxStatus status);

    Page<WalletTransactionJpaEntity> findByUserId(Long userId, Pageable pageable);
}
