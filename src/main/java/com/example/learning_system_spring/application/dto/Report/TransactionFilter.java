package com.example.learning_system_spring.application.dto.Report;

import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;

import java.time.LocalDate;

/**
 * Bộ lọc giao dịch cho admin. Mọi field null = bỏ qua điều kiện đó.
 *
 * @param keyword   match username / email / referenceCode (LIKE, không phân biệt hoa thường)
 * @param source    lọc theo nguồn giao dịch
 * @param status    lọc theo trạng thái
 * @param direction "CREDIT" | "DEBIT" — DEBIT = source PURCHASE, CREDIT = các source còn lại
 * @param from      ngày bắt đầu (inclusive, theo createdAt)
 * @param to        ngày kết thúc (inclusive — BE quy đổi sang nửa mở [from, to+1))
 */
public record TransactionFilter(
        String keyword,
        TxSource source,
        TxStatus status,
        String direction,
        LocalDate from,
        LocalDate to
) {}
