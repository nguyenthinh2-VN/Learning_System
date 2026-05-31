package com.example.learning_system_spring.adapter.repository;

import com.example.learning_system_spring.application.dto.PageResult;
import com.example.learning_system_spring.application.dto.Report.AdminTransactionItemOutput;
import com.example.learning_system_spring.application.dto.Report.RevenuePointOutput;
import com.example.learning_system_spring.application.dto.Report.TransactionFilter;
import com.example.learning_system_spring.application.repository.Report.AdminReportRepository;
import com.example.learning_system_spring.domain.model.Wallet.TxSource;
import com.example.learning_system_spring.domain.model.Wallet.TxStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter read-side cho báo cáo admin.
 * - Danh sách + tổng hợp dùng JPQL động qua EntityManager.
 * - Chuỗi thời gian dùng native query (MySQL DATE_FORMAT) vì JPQL không hỗ trợ group theo ngày/tháng.
 */
@Repository
@RequiredArgsConstructor
public class AdminReportRepositoryImpl implements AdminReportRepository {

    private final EntityManager em;

    // ─── Danh sách giao dịch (join user) ─────────────────────────────
    @Override
    public PageResult<AdminTransactionItemOutput> searchTransactions(TransactionFilter f, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE t.userId = u.id");
        Map<String, Object> params = new LinkedHashMap<>();
        buildConditions(f, where, params);

        // Count tổng (cho phân trang)
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(t) FROM WalletTransactionJpaEntity t, UserJpaEntity u" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        // Lấy trang dữ liệu — select projection (entity + thông tin user)
        TypedQuery<Object[]> dataQuery = em.createQuery(
                "SELECT t.id, t.userId, u.username, u.email, t.referenceCode, t.amount, " +
                        "t.status, t.source, t.note, t.createdAt, t.completedAt " +
                        "FROM WalletTransactionJpaEntity t, UserJpaEntity u" + where +
                        " ORDER BY t.createdAt DESC, t.id DESC", Object[].class);
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult(page * size);
        dataQuery.setMaxResults(size);

        List<AdminTransactionItemOutput> items = new ArrayList<>();
        for (Object[] r : dataQuery.getResultList()) {
            TxSource source = (TxSource) r[7];
            String direction = (source == TxSource.PURCHASE) ? "DEBIT" : "CREDIT";
            items.add(new AdminTransactionItemOutput(
                    (Long) r[0],            // id
                    (Long) r[1],            // userId
                    (String) r[2],          // username
                    (String) r[3],          // email
                    (String) r[4],          // referenceCode
                    (BigDecimal) r[5],      // amount
                    direction,
                    (TxStatus) r[6],        // status
                    source,
                    (String) r[8],          // note
                    (LocalDateTime) r[9],   // createdAt
                    (LocalDateTime) r[10])); // completedAt
        }

        int totalPages = (int) Math.ceil((double) total / size);
        return PageResult.of(total, totalPages, page, size, items);
    }

    /** Build điều kiện WHERE động + nạp params. */
    private void buildConditions(TransactionFilter f, StringBuilder where, Map<String, Object> params) {
        if (f == null) return;

        if (f.keyword() != null && !f.keyword().isBlank()) {
            where.append(" AND (LOWER(u.username) LIKE :kw OR LOWER(u.email) LIKE :kw " +
                    "OR LOWER(t.referenceCode) LIKE :kw)");
            params.put("kw", "%" + f.keyword().trim().toLowerCase() + "%");
        }
        if (f.source() != null) {
            where.append(" AND t.source = :source");
            params.put("source", f.source());
        }
        if (f.status() != null) {
            where.append(" AND t.status = :status");
            params.put("status", f.status());
        }
        if (f.direction() != null && !f.direction().isBlank()) {
            if ("DEBIT".equalsIgnoreCase(f.direction())) {
                where.append(" AND t.source = :purchaseSrc");
            } else {
                where.append(" AND t.source <> :purchaseSrc");
            }
            params.put("purchaseSrc", TxSource.PURCHASE);
        }
        if (f.from() != null) {
            where.append(" AND t.createdAt >= :from");
            params.put("from", f.from().atStartOfDay());
        }
        if (f.to() != null) {
            where.append(" AND t.createdAt < :toExclusive");
            params.put("toExclusive", f.to().plusDays(1).atStartOfDay());
        }
    }

    // ─── Tổng hợp ────────────────────────────────────────────────────
    @Override
    public BigDecimal sumRevenue(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        TypedQuery<BigDecimal> q = em.createQuery(
                "SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionJpaEntity t " +
                        "WHERE t.status = :status AND t.source = :source " +
                        "AND t.createdAt >= :from AND t.createdAt < :to", BigDecimal.class);
        q.setParameter("status", TxStatus.COMPLETED);
        q.setParameter("source", TxSource.PURCHASE);
        q.setParameter("from", fromInclusive);
        q.setParameter("to", toExclusive);
        return q.getSingleResult();
    }

    @Override
    public BigDecimal sumTopUp(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        TypedQuery<BigDecimal> q = em.createQuery(
                "SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionJpaEntity t " +
                        "WHERE t.status = :status AND t.source IN :sources " +
                        "AND t.createdAt >= :from AND t.createdAt < :to", BigDecimal.class);
        q.setParameter("status", TxStatus.COMPLETED);
        q.setParameter("sources", List.of(TxSource.MOCK, TxSource.VIETQR, TxSource.ADMIN));
        q.setParameter("from", fromInclusive);
        q.setParameter("to", toExclusive);
        return q.getSingleResult();
    }

    @Override
    public long countCoursesSold(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(e) FROM EnrollmentJpaEntity e " +
                        "WHERE e.enrolledAt >= :from AND e.enrolledAt < :to", Long.class);
        q.setParameter("from", fromInclusive);
        q.setParameter("to", toExclusive);
        return q.getSingleResult();
    }

    @Override
    public long countPaidPurchases(LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(t) FROM WalletTransactionJpaEntity t " +
                        "WHERE t.status = :status AND t.source = :source " +
                        "AND t.createdAt >= :from AND t.createdAt < :to", Long.class);
        q.setParameter("status", TxStatus.COMPLETED);
        q.setParameter("source", TxSource.PURCHASE);
        q.setParameter("from", fromInclusive);
        q.setParameter("to", toExclusive);
        return q.getSingleResult();
    }

    // ─── Chuỗi thời gian (native) ────────────────────────────────────
    @Override
    public List<RevenuePointOutput> revenueSeries(String granularity, LocalDateTime fromInclusive,
                                                  LocalDateTime toExclusive) {
        String pattern = "MONTH".equalsIgnoreCase(granularity) ? "%Y-%m" : "%Y-%m-%d";

        String sql = "SELECT DATE_FORMAT(created_at, :pattern) AS period, " +
                "SUM(amount) AS revenue, COUNT(*) AS cnt " +
                "FROM wallet_transactions " +
                "WHERE status = 'COMPLETED' AND source = 'PURCHASE' " +
                "AND created_at >= :fromTs AND created_at < :toTs " +
                "GROUP BY period ORDER BY period";

        Query q = em.createNativeQuery(sql);
        q.setParameter("pattern", pattern);
        q.setParameter("fromTs", fromInclusive);
        q.setParameter("toTs", toExclusive);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<RevenuePointOutput> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String period = (String) r[0];
            BigDecimal revenue = r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString());
            long count = ((Number) r[2]).longValue();
            result.add(new RevenuePointOutput(period, revenue, count));
        }
        return result;
    }
}
