package com.example.learning_system_spring.application.usecase.Report;

import com.example.learning_system_spring.application.dto.Report.RevenuePointOutput;
import com.example.learning_system_spring.application.dto.Report.RevenueSummaryOutput;
import com.example.learning_system_spring.application.repository.Report.AdminReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Báo cáo doanh thu cho admin dashboard: số liệu tổng + chuỗi thời gian theo ngày/tháng.
 *
 * - Doanh thu = tx PURCHASE COMPLETED (tiền ra khỏi ví học viên).
 * - Số khóa bán ra = COUNT enrollments (gồm cả vé 0đ của Internal Member).
 * - Mặc định khoảng thời gian: DAY → 30 ngày gần nhất; MONTH → 12 tháng gần nhất.
 */
@Service
@RequiredArgsConstructor
public class GetRevenueReportUseCase {

    private final AdminReportRepository adminReportRepository;

    @Transactional(readOnly = true)
    public RevenueSummaryOutput execute(String granularity, LocalDate from, LocalDate to) {
        String g = normalizeGranularity(granularity);

        LocalDate today = LocalDate.now();
        LocalDate effectiveTo = (to != null) ? to : today;
        LocalDate effectiveFrom = (from != null) ? from : defaultFrom(g, effectiveTo);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from không được sau to");
        }

        LocalDateTime fromTs = effectiveFrom.atStartOfDay();
        LocalDateTime toTs = effectiveTo.plusDays(1).atStartOfDay(); // nửa mở, bao trọn ngày cuối

        BigDecimal totalRevenue = adminReportRepository.sumRevenue(fromTs, toTs);
        BigDecimal totalTopUp = adminReportRepository.sumTopUp(fromTs, toTs);
        long coursesSold = adminReportRepository.countCoursesSold(fromTs, toTs);
        long paidPurchaseCount = adminReportRepository.countPaidPurchases(fromTs, toTs);
        List<RevenuePointOutput> series = adminReportRepository.revenueSeries(g, fromTs, toTs);

        return new RevenueSummaryOutput(
                totalRevenue,
                coursesSold,
                paidPurchaseCount,
                totalTopUp,
                g,
                effectiveFrom,
                effectiveTo,
                series);
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "DAY";
        }
        String g = granularity.trim().toUpperCase();
        if (!g.equals("DAY") && !g.equals("MONTH")) {
            throw new IllegalArgumentException("granularity phải là DAY hoặc MONTH");
        }
        return g;
    }

    private LocalDate defaultFrom(String granularity, LocalDate to) {
        return "MONTH".equals(granularity)
                ? to.minusMonths(11).withDayOfMonth(1) // 12 tháng gần nhất
                : to.minusDays(29);                    // 30 ngày gần nhất
    }
}
