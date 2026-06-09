package com.example.learning_system_spring.adapter.controller;

import com.example.learning_system_spring.adapter.dto.response.AdminTransactionItemResponse;
import com.example.learning_system_spring.adapter.dto.response.ApiResponse;
import com.example.learning_system_spring.adapter.dto.response.RevenueReportResponse;
import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Report.AdminTransactionItemOutput;
import com.example.learning_system_spring.application.dto.Report.RevenueSummaryOutput;
import com.example.learning_system_spring.application.dto.Report.TransactionFilter;
import com.example.learning_system_spring.application.usecase.Report.GetAdminTransactionsUseCase;
import com.example.learning_system_spring.application.usecase.Report.GetRevenueReportUseCase;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Báo cáo & giao dịch toàn hệ thống cho Admin Portal.
 * Dữ liệu tài chính nhạy cảm → enforce theo permission động (VIEW_TRANSACTION / VIEW_REVENUE).
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final GetAdminTransactionsUseCase getAdminTransactionsUseCase;
    private final GetRevenueReportUseCase getRevenueReportUseCase;

    /**
     * Danh sách giao dịch của mọi user (lọc + phân trang, mới nhất trước).
     *
     * GET /api/v1/admin/transactions?keyword=&source=&status=&direction=&from=&to=&page=0&size=20
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('VIEW_TRANSACTION')")
    public ResponseEntity<ApiResponse<PageResult<AdminTransactionItemResponse>>> listTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionFilter filter = new TransactionFilter(
                keyword,
                parseSource(source),
                parseStatus(status),
                normalizeDirection(direction),
                from,
                to);

        PageResult<AdminTransactionItemOutput> result =
                getAdminTransactionsUseCase.execute(filter, page, size);
        PageResult<AdminTransactionItemResponse> response =
                result.map(AdminTransactionItemResponse::from);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Báo cáo doanh thu: số liệu tổng + chuỗi thời gian theo ngày/tháng.
     *
     * GET /api/v1/admin/reports/revenue?granularity=DAY|MONTH&from=&to=
     */
    @GetMapping("/reports/revenue")
    @PreAuthorize("hasAuthority('VIEW_REVENUE')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> revenueReport(
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        RevenueSummaryOutput output = getRevenueReportUseCase.execute(granularity, from, to);
        return ResponseEntity.ok(ApiResponse.success(RevenueReportResponse.from(output)));
    }

    // ─── Parse helpers (giá trị rỗng/sai → null = bỏ lọc) ────────────
    private TxSource parseSource(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return TxSource.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("source không hợp lệ: " + s);
        }
    }

    private TxStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return TxStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status không hợp lệ: " + s);
        }
    }

    private String normalizeDirection(String d) {
        if (d == null || d.isBlank()) return null;
        String up = d.trim().toUpperCase();
        if (!up.equals("CREDIT") && !up.equals("DEBIT")) {
            throw new IllegalArgumentException("direction phải là CREDIT hoặc DEBIT");
        }
        return up;
    }
}
