package com.example.learning_system_spring.adapter.dto.response;

import com.example.learning_system_spring.application.dto.Report.RevenueSummaryOutput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response báo cáo doanh thu cho admin dashboard.
 */
public record RevenueReportResponse(
        BigDecimal totalRevenue,
        long coursesSold,
        long paidPurchaseCount,
        BigDecimal totalTopUp,
        String granularity,
        LocalDate from,
        LocalDate to,
        List<RevenuePointResponse> series
) {
    public record RevenuePointResponse(
            String period,
            BigDecimal revenue,
            long count
    ) {}

    public static RevenueReportResponse from(RevenueSummaryOutput o) {
        List<RevenuePointResponse> series = o.series().stream()
                .map(p -> new RevenuePointResponse(p.period(), p.revenue(), p.count()))
                .toList();
        return new RevenueReportResponse(
                o.totalRevenue(),
                o.coursesSold(),
                o.paidPurchaseCount(),
                o.totalTopUp(),
                o.granularity(),
                o.from(),
                o.to(),
                series);
    }
}
