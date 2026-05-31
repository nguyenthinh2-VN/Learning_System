package com.example.learning_system_spring.application.dto.Report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo doanh thu tổng hợp cho admin dashboard.
 *
 * @param totalRevenue      tổng doanh thu (tx PURCHASE COMPLETED) trong [from, to]
 * @param coursesSold       tổng số khóa bán ra (COUNT enrollments) trong [from, to] — gồm cả vé 0đ
 * @param paidPurchaseCount số giao dịch mua có doanh thu (tx PURCHASE COMPLETED)
 * @param totalTopUp        tổng tiền nạp ví COMPLETED (MOCK+VIETQR+ADMIN) — tham khảo, KHÔNG phải doanh thu
 * @param granularity       "DAY" | "MONTH"
 * @param from              ngày bắt đầu khoảng báo cáo
 * @param to                ngày kết thúc khoảng báo cáo
 * @param series            chuỗi thời gian doanh thu
 */
public record RevenueSummaryOutput(
        BigDecimal totalRevenue,
        long coursesSold,
        long paidPurchaseCount,
        BigDecimal totalTopUp,
        String granularity,
        LocalDate from,
        LocalDate to,
        List<RevenuePointOutput> series
) {}
