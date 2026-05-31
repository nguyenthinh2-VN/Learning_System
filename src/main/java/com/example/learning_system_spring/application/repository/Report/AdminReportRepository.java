package com.example.learning_system_spring.application.repository.Report;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Report.AdminTransactionItemOutput;
import com.example.learning_system_spring.application.dto.Report.RevenuePointOutput;
import com.example.learning_system_spring.application.dto.Report.TransactionFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Port đọc (read-side) cho báo cáo admin — tách khỏi WalletTransactionRepository (write-side).
 * Tổng hợp dữ liệu toàn hệ thống: giao dịch mọi user, doanh thu, số khóa bán ra.
 *
 * Quy ước thời gian: các tham số khoảng dùng nửa mở [fromInclusive, toExclusive).
 */
public interface AdminReportRepository {

    /** Danh sách giao dịch toàn hệ thống (kèm username/email), lọc động + phân trang, mới nhất trước. */
    PageResult<AdminTransactionItemOutput> searchTransactions(TransactionFilter filter, int page, int size);

    /** Tổng doanh thu = SUM(amount) tx COMPLETED + source=PURCHASE trong [from, to). */
    BigDecimal sumRevenue(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /** Tổng nạp ví = SUM(amount) tx COMPLETED + source IN (MOCK,VIETQR,ADMIN) trong [from, to). */
    BigDecimal sumTopUp(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /** Số khóa bán ra = COUNT(enrollments) theo enrolledAt trong [from, to) (gồm cả vé 0đ). */
    long countCoursesSold(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /** Số giao dịch mua có doanh thu = COUNT tx COMPLETED + source=PURCHASE trong [from, to). */
    long countPaidPurchases(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /**
     * Chuỗi thời gian doanh thu, group theo ngày hoặc tháng.
     *
     * @param granularity "DAY" → period "yyyy-MM-dd"; "MONTH" → period "yyyy-MM"
     */
    List<RevenuePointOutput> revenueSeries(String granularity, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
