package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.adapter.repository.jpa.VoucherEntity.VoucherJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface JpaVoucherRepository extends JpaRepository<VoucherJpaEntity, Long> {

    Optional<VoucherJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherJpaEntity v WHERE v.id = :id")
    Optional<VoucherJpaEntity> findByIdForUpdate(@Param("id") Long id);

    // OPT-1: Gộp findByCode + findByIdForUpdate thành 1 query duy nhất cho luồng checkout.
    // Tránh 2 round-trip DB (findByCode lấy id, rồi findByIdForUpdate lấy lại với lock).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherJpaEntity v WHERE v.code = :code")
    Optional<VoucherJpaEntity> findByCodeForUpdate(@Param("code") String code);

    Page<VoucherJpaEntity> findAll(Pageable pageable);
}
