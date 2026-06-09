# Kế hoạch: Admin Dashboard Doanh thu + Xem giao dịch toàn hệ thống

**Ngày lập:** 2026-05-31
**Phạm vi:** API tổng hợp (read-side) + UI cho Admin Portal
**Nguyên tắc thiết kế:** Tách read-model khỏi write-model (CQRS nhẹ), không sửa domain/write code đang chạy
**Trạng thái:** ✅ Đã implement (BE compile OK, FE build OK)

---

## 1. Mục tiêu

`AdminOverviewPage` hiện trống (doanh thu để placeholder "Cần BE endpoint"). Dữ liệu giao dịch
2 chiều đã có trong `wallet_transactions`, enrollment đã có `paid_price`. Cần bổ sung **3 năng lực**:

1. **Xem giao dịch toàn hệ thống** — admin xem mọi giao dịch của mọi user (lọc + phân trang + tên user).
2. **Biểu đồ doanh thu theo ngày/tháng** — time series từ giao dịch `PURCHASE` (tiền ra khỏi ví học viên).
3. **Tổng số khóa bán ra** — đếm enrollment (chính xác hơn đếm tx vì Internal Member mua 0đ không tạo tx).

---

## 2. Định nghĩa nghiệp vụ (chốt trước khi code)

| Khái niệm | Nguồn dữ liệu | Ghi chú |
|---|---|---|
| **Doanh thu** | `SUM(amount)` của tx `status=COMPLETED AND source=PURCHASE` | Đây là tiền ra khỏi ví học viên = doanh thu nền tảng |
| **Nạp ví (KHÔNG phải doanh thu)** | tx `source IN (MOCK, VIETQR, ADMIN)` | Chỉ là tiền nạp vào ví, không tính vào doanh thu |
| **Số khóa bán ra** | `COUNT(enrollments)` | Bao gồm cả Internal Member mua 0đ |
| **Số khóa bán có doanh thu** | `COUNT(tx PURCHASE COMPLETED)` | Loại giao dịch 0đ; hiển thị phụ |
| **Direction** | PURCHASE → `DEBIT`, còn lại → `CREDIT` | Tái dùng logic `TransactionItemOutput` |

> **Lưu ý 0đ:** `WalletTransaction.createPurchase()` ném lỗi nếu `amount <= 0` → Internal Member mua 0đ
> **không** tạo `wallet_transactions`. Vì vậy "số khóa bán ra" lấy từ `enrollments`, doanh thu lấy từ tx PURCHASE.

---

## 3. Phân quyền

Dữ liệu tài chính toàn hệ thống là nhạy cảm. Đồng nhất với "Admin cộng tiền" (`SUPER_ADMIN`):

| Endpoint | Role gate |
|---|---|
| `GET /api/v1/admin/transactions` | `@PreAuthorize("hasRole('SUPER_ADMIN')")` |
| `GET /api/v1/admin/reports/revenue` | `@PreAuthorize("hasRole('SUPER_ADMIN')")` |

> **Quyết định cần xác nhận:** chỉ `SUPER_ADMIN`, hay mở thêm cho `STAFF`? Theo permission-matrix,
> `VIEW_REPORT` thuộc INSTRUCTOR + SUPER_ADMIN, nhưng "toàn hệ thống" nên giữ `SUPER_ADMIN`.
> INSTRUCTOR (chỉ xem của mình) là phạm vi khác — ngoài kế hoạch này.

---

## 4. Schema — KHÔNG cần migration

- Entity `WalletTransactionJpaEntity.source` là `@Enumerated(STRING) @Column(length=16)` → Hibernate
  (`ddl-auto: update`) tạo cột `VARCHAR(16)`, **không** phải MySQL ENUM. Giá trị `PURCHASE` đã được ghi
  bình thường khi tính năng mua khóa chạy.
- File tham chiếu `src/main/resources/sql/wallet_transactions.sql` ghi `ENUM('MOCK','VIETQR','ADMIN')` đã
  **lỗi thời** (thiếu PURCHASE) — chỉ là tài liệu, không ảnh hưởng runtime. *(Tùy chọn)* cập nhật cho khớp.
- *(Tùy chọn, tối ưu)* thêm index hỗ trợ aggregation/lọc:
  ```sql
  CREATE INDEX idx_wallet_tx_source_status_created ON wallet_transactions(source, status, created_at);
  ```

---

## 5. Backend — Thiết kế read-side (CQRS nhẹ)

Không nhét query báo cáo vào `WalletTransactionRepository` (write-side). Tạo **port đọc riêng**.

### 5.1. DTO (application/dto/Report/)

```java
// AdminTransactionItemOutput.java — 1 dòng giao dịch kèm thông tin user
public record AdminTransactionItemOutput(
    Long id, Long userId, String username, String email,
    String referenceCode, BigDecimal amount, String direction,
    TxStatus status, TxSource source, String note,
    LocalDateTime createdAt, LocalDateTime completedAt) {}

// TransactionFilter.java — tham số lọc (field null = bỏ qua)
public record TransactionFilter(
    String keyword,      // match username / email / referenceCode
    TxSource source,     // nullable
    TxStatus status,     // nullable
    String direction,    // "CREDIT" | "DEBIT" | null
    LocalDate from,      // nullable
    LocalDate to) {}     // nullable

// RevenuePointOutput.java — 1 điểm trên biểu đồ
public record RevenuePointOutput(String period, BigDecimal revenue, long count) {}
//   period = "2026-05-31" (DAY) hoặc "2026-05" (MONTH)

// RevenueSummaryOutput.java — số liệu tổng + chuỗi thời gian
public record RevenueSummaryOutput(
    BigDecimal totalRevenue,     // SUM PURCHASE COMPLETED trong [from,to]
    long coursesSold,            // COUNT enrollments trong [from,to]
    long paidPurchaseCount,      // COUNT tx PURCHASE COMPLETED
    BigDecimal totalTopUp,       // SUM nạp ví COMPLETED (tham khảo)
    String granularity,          // "DAY" | "MONTH"
    List<RevenuePointOutput> series) {}
```

### 5.2. Port (application/repository/Report/AdminReportRepository.java)

```java
public interface AdminReportRepository {
    PageResult<AdminTransactionItemOutput> searchTransactions(TransactionFilter f, int page, int size);
    BigDecimal sumRevenue(LocalDate from, LocalDate to);          // PURCHASE COMPLETED
    BigDecimal sumTopUp(LocalDate from, LocalDate to);            // MOCK+VIETQR+ADMIN COMPLETED
    long countCoursesSold(LocalDate from, LocalDate to);          // enrollments
    long countPaidPurchases(LocalDate from, LocalDate to);        // PURCHASE COMPLETED count
    List<RevenuePointOutput> revenueSeries(String granularity, LocalDate from, LocalDate to);
}
```

### 5.3. Impl (adapter/repository/AdminReportRepositoryImpl.java)

- `searchTransactions`: JPQL join `WalletTransactionJpaEntity` ↔ `UserJpaEntity` (theo `userId`), build
  điều kiện động (keyword/source/status/direction/from-to). Direction lọc theo `source = PURCHASE`
  (DEBIT) hoặc `source <> PURCHASE` (CREDIT). Sắp xếp `createdAt DESC`, phân trang `Pageable`.
- `revenueSeries`: **native query MySQL** (group theo ngày/tháng):
  ```sql
  -- DAY
  SELECT DATE_FORMAT(created_at,'%Y-%m-%d') p, SUM(amount) rev, COUNT(*) c
  FROM wallet_transactions
  WHERE status='COMPLETED' AND source='PURCHASE' AND created_at >= :from AND created_at < :toExclusive
  GROUP BY p ORDER BY p;
  -- MONTH: DATE_FORMAT(created_at,'%Y-%m')
  ```
  (JPQL không hỗ trợ `DATE_FORMAT` ổn định → dùng native.)
- `sumRevenue/sumTopUp/countPaidPurchases`: JPQL aggregate; `coalesce(SUM,0)`.
- `countCoursesSold`: JPQL count trên `EnrollmentJpaEntity` theo `enrolledAt` trong khoảng.

> FE gửi `from`/`to` dạng ngày; BE quy đổi `to` thành `toExclusive = to.plusDays(1).atStartOfDay()` để
> bao trọn ngày cuối. Mặc định nếu thiếu: 30 ngày gần nhất (DAY) hoặc 12 tháng gần nhất (MONTH).

### 5.4. Use cases (application/usecase/Report/)

| Use case | Làm gì |
|---|---|
| `GetAdminTransactionsUseCase` | Validate page/size (size ≤ 100), gọi `searchTransactions` |
| `GetRevenueReportUseCase` | Validate granularity ∈ {DAY,MONTH}, default khoảng thời gian, gọi các hàm tổng hợp + `revenueSeries`, gói `RevenueSummaryOutput` |

### 5.5. Controller (adapter/controller/AdminReportController.java)

```
GET /api/v1/admin/transactions
  ?keyword=&source=&status=&direction=&from=&to=&page=0&size=20
  → ApiResponse<PageResult<AdminTransactionItemResponse>>

GET /api/v1/admin/reports/revenue
  ?granularity=DAY|MONTH&from=&to=
  → ApiResponse<RevenueReportResponse>
```
Cả hai `@PreAuthorize("hasRole('SUPER_ADMIN')")`.

### 5.6. Response DTO (adapter/dto/response/)

- `AdminTransactionItemResponse` (map enum → String, giống `TransactionItemResponse`).
- `RevenueReportResponse` (+ `RevenuePointResponse`).

---

## 6. Frontend

### 6.1. API client (fe/src/api/adminApi.js)

```js
export const getAdminTransactionsApi = (params = {}) =>
  adminApi.get('/admin/transactions', { params });
export const getRevenueReportApi = (params = {}) =>      // { granularity, from, to }
  adminApi.get('/admin/reports/revenue', { params });
```

### 6.2. Biểu đồ — QUYẾT ĐỊNH cần chọn

| Phương án | Ưu | Nhược |
|---|---|---|
| **A. Component SVG thuần** (`RevenueChart.jsx`, ~80 dòng, Tailwind) | Không thêm dependency, giữ FE gọn | Tự code tooltip/trục |
| **B. `recharts`** (`npm i recharts`, chuẩn shadcn chart) | Đẹp, nhanh, tooltip sẵn | Thêm ~1 dependency |

> **Đề xuất:** Phương án A (bar chart SVG) để giữ triết lý FE lean của dự án; nếu muốn polish nhanh thì B.

### 6.3. Trang & điều hướng

- **Enhance `AdminOverviewPage.jsx`**: thay placeholder doanh thu bằng số thật (chỉ hiện cho
  `SUPER_ADMIN`): card "Doanh thu (30 ngày)", "Khóa bán ra", + `RevenueChart` có toggle Ngày/Tháng.
- **Trang mới `AdminTransactionsPage.jsx`** (`/admin/transactions`): bộ lọc (keyword, source, status,
  direction, from-to) + `DataTable` (reuse). Cột: Thời gian · **Người dùng** (username + email) · Loại
  (source + note) · Trạng thái · Direction · Số tiền (+xanh / −đỏ). Phân trang server-side.
- **`fe/src/features/wallet/transactionColumns.jsx`**: thêm cột `user` cho biến thể admin (giữ
  `SOURCE_LABELS`/`STATUS_LABELS` tái dùng; PURCHASE đã có label).
- **`AdminSidebar.jsx`** nhóm "Tài chính": thêm `{ title: 'Giao dịch', url: '/admin/transactions',
  icon: Receipt, roles: ['SUPER_ADMIN'] }`.
- **`App.jsx`**: thêm `<Route path="/admin/transactions" element={<AdminTransactionsPage />} />` trong
  `AdminLayout`.

---

## 7. Thứ tự implement

```
BE-1  DTO read-model (Report/*) + TransactionFilter
BE-2  Port AdminReportRepository
BE-3  AdminReportRepositoryImpl (JPQL list + native series + aggregates)
BE-4  Use cases: GetAdminTransactionsUseCase, GetRevenueReportUseCase
BE-5  Response DTO + AdminReportController (@PreAuthorize SUPER_ADMIN)
BE-6  Test: mvnw compile + gọi thử 2 endpoint (seed vài purchase)
FE-1  adminApi: getAdminTransactionsApi, getRevenueReportApi
FE-2  RevenueChart.jsx (Phương án A hoặc B)
FE-3  AdminTransactionsPage.jsx + cột user + route + sidebar
FE-4  Enhance AdminOverviewPage (doanh thu thật + chart)
FE-5  Kiểm thử end-to-end + npm run build
DOC   (tùy chọn) docs/API/admin-reports.md, cập nhật endpoints-summary.md, sửa wallet_transactions.sql
```

---

## 8. File cần tạo / sửa

### Backend — tạo mới
| File | Mô tả |
|---|---|
| `application/dto/Report/AdminTransactionItemOutput.java` | record |
| `application/dto/Report/TransactionFilter.java` | record lọc |
| `application/dto/Report/RevenuePointOutput.java` | record điểm |
| `application/dto/Report/RevenueSummaryOutput.java` | record tổng hợp |
| `application/repository/Report/AdminReportRepository.java` | port |
| `adapter/repository/AdminReportRepositoryImpl.java` | impl JPQL + native |
| `application/usecase/Report/GetAdminTransactionsUseCase.java` | use case |
| `application/usecase/Report/GetRevenueReportUseCase.java` | use case |
| `adapter/dto/response/AdminTransactionItemResponse.java` | response |
| `adapter/dto/response/RevenueReportResponse.java` (+ point) | response |
| `adapter/controller/AdminReportController.java` | 2 endpoint |

### Backend — sửa (tùy chọn)
| File | Thay đổi |
|---|---|
| `resources/sql/wallet_transactions.sql` | thêm `PURCHASE` + index (tài liệu) |

### Frontend — tạo mới
| File | Mô tả |
|---|---|
| `fe/src/pages/admin/AdminTransactionsPage.jsx` | trang giao dịch toàn hệ thống |
| `fe/src/features/wallet/RevenueChart.jsx` | biểu đồ doanh thu |

### Frontend — sửa
| File | Thay đổi |
|---|---|
| `fe/src/api/adminApi.js` | 2 hàm API mới |
| `fe/src/pages/admin/AdminOverviewPage.jsx` | doanh thu thật + chart |
| `fe/src/features/wallet/transactionColumns.jsx` | cột user (admin variant) |
| `fe/src/components/layout/AdminSidebar.jsx` | menu "Giao dịch" |
| `fe/src/App.jsx` | route `/admin/transactions` |

---

## 9. Quyết định đã chốt (2026-05-31)

1. **Phân quyền:** chỉ `SUPER_ADMIN` được xem giao dịch + doanh thu toàn hệ thống.
2. **Biểu đồ:** Phương án **B — `recharts`** (`npm i recharts`).
3. **Headline "khóa bán ra":** `COUNT(enrollments)` (gồm cả vé 0đ của Internal Member). Doanh thu vẫn từ tx PURCHASE.
