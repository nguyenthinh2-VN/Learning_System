package com.example.learning_system_spring.application.repository.Voucher;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.domain.model.Voucher.Voucher;

import java.util.Optional;

public interface VoucherRepository {

    Voucher save(Voucher voucher);

    Optional<Voucher> findById(Long id);

    Optional<Voucher> findByCode(String normalizedCode);

    /**
     * Load voucher với pessimistic write lock — bắt buộc gọi trong transaction.
     * Dùng cho luồng checkout để serialize concurrent purchases trên cùng voucher.
     */
    Optional<Voucher> findByIdForUpdate(Long id);

    boolean existsByCode(String normalizedCode);

    PageResult<Voucher> findAll(int page, int size);
}
