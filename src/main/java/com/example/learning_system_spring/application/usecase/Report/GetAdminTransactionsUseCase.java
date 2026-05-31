package com.example.learning_system_spring.application.usecase.Report;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Report.AdminTransactionItemOutput;
import com.example.learning_system_spring.application.dto.Report.TransactionFilter;
import com.example.learning_system_spring.application.repository.Report.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin xem giao dịch toàn hệ thống (mọi user), có lọc + phân trang.
 * Chỉ đọc — không thay đổi dữ liệu.
 */
@Service
@RequiredArgsConstructor
public class GetAdminTransactionsUseCase {

    private final AdminReportRepository adminReportRepository;

    @Transactional(readOnly = true)
    public PageResult<AdminTransactionItemOutput> execute(TransactionFilter filter, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page phải >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size phải trong khoảng [1, 100]");
        }
        if (filter != null && filter.from() != null && filter.to() != null
                && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("from không được sau to");
        }
        return adminReportRepository.searchTransactions(filter, page, size);
    }
}
