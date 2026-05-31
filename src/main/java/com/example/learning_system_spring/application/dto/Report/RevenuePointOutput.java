package com.example.learning_system_spring.application.dto.Report;

import java.math.BigDecimal;

/**
 * Một điểm trên biểu đồ doanh thu.
 *
 * @param period  "2026-05-31" (granularity=DAY) hoặc "2026-05" (granularity=MONTH)
 * @param revenue tổng doanh thu (SUM amount của tx PURCHASE COMPLETED) trong kỳ
 * @param count   số giao dịch mua có doanh thu trong kỳ
 */
public record RevenuePointOutput(
        String period,
        BigDecimal revenue,
        long count
) {}
