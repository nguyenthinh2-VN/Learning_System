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

    /**
     * OPT-1: Load voucher theo code với pessimistic write lock trong 1 query.
     * Dùng thay cho findByCode + findByIdForUpdate trong luồng checkout.
     */
    Optional<Voucher> findByCodeForUpdate(String normalizedCode);

    boolean existsByCode(String normalizedCode);

    PageResult<Voucher> findAll(int page, int size);
}
