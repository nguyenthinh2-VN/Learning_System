# Admin Reports & Transactions (Doanh thu + Giao dịch toàn hệ thống)

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

> **Quyền:** Toàn bộ endpoint trong tài liệu này yêu cầu role `SUPER_ADMIN`
> (`@PreAuthorize("hasRole('SUPER_ADMIN')")`). Role khác → 403 `ACCESS_DENIED`.

> **Định nghĩa doanh thu:** Doanh thu = tổng giao dịch `source=PURCHASE` + `status=COMPLETED`
> (tiền ra khỏi ví học viên khi mua khóa). Giao dịch nạp ví (`MOCK`/`VIETQR`/`ADMIN`) **không**
> tính vào doanh thu. "Số khóa bán ra" đếm từ bảng `enrollments` (gồm cả vé 0đ của Internal Member,
> vốn không tạo giao dịch ví).

---

## 1. Xem giao dịch toàn hệ thống

**Endpoint:** `GET /api/v1/admin/transactions`

Danh sách giao dịch của **mọi user**, kèm thông tin người dùng, có lọc + phân trang, mới nhất trước.

**Query params (đều tùy chọn):**
| Param | Type | Mô tả |
|-------|------|-------|
| `keyword` | string | Match `username` / `email` / `referenceCode` (không phân biệt hoa thường) |
| `source` | string | `MOCK` \| `VIETQR` \| `ADMIN` \| `PURCHASE` |
| `status` | string | `PENDING` \| `COMPLETED` \| `EXPIRED` \| `FAILED` |
| `direction` | string | `CREDIT` (tiền vào — nạp ví) \| `DEBIT` (tiền ra — mua khóa) |
| `from` | date | `yyyy-MM-dd` — lọc theo `createdAt`, inclusive |
| `to` | date | `yyyy-MM-dd` — inclusive (BE quy đổi sang nửa mở `[from, to+1)`) |
| `page` | int | Mặc định `0` |
| `size` | int | Mặc định `20`, khoảng `[1, 100]` |

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 152,
    "totalPages": 8,
    "page": 0,
    "size": 20,
    "items": [
      {
        "id": 152,
        "userId": 5,
        "username": "MEM2B4A1D",
        "email": "user@example.com",
        "referenceCode": "BUY3F8A2C1B3",
        "amount": 500000.00,
        "direction": "DEBIT",
        "status": "COMPLETED",
        "source": "PURCHASE",
        "note": "Mua khóa: Spring Boot từ A-Z",
        "createdAt": "2026-05-31T10:05:00",
        "completedAt": "2026-05-31T10:05:00"
      }
    ]
  },
  "timestamp": "2026-05-31T10:10:00"
}
```

**Response 400 (param sai):**
```json
{ "code": "BAD_REQUEST", "message": "source không hợp lệ: FOO", "timestamp": "..." }
```

---

## 2. Báo cáo doanh thu

**Endpoint:** `GET /api/v1/admin/reports/revenue`

Số liệu tổng hợp + chuỗi thời gian doanh thu theo ngày hoặc tháng.

**Query params:**
| Param | Type | Mô tả |
|-------|------|-------|
| `granularity` | string | `DAY` (mặc định) \| `MONTH` |
| `from` | date | `yyyy-MM-dd` — tùy chọn. Mặc định: DAY → 30 ngày gần nhất; MONTH → 12 tháng gần nhất |
| `to` | date | `yyyy-MM-dd` — tùy chọn. Mặc định: hôm nay |

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalRevenue": 12500000.00,
    "coursesSold": 37,
    "paidPurchaseCount": 30,
    "totalTopUp": 45000000.00,
    "granularity": "DAY",
    "from": "2026-05-02",
    "to": "2026-05-31",
    "series": [
      { "period": "2026-05-28", "revenue": 1500000.00, "count": 3 },
      { "period": "2026-05-30", "revenue": 500000.00,  "count": 1 },
      { "period": "2026-05-31", "revenue": 2000000.00, "count": 4 }
    ]
  },
  "timestamp": "2026-05-31T10:10:00"
}
```

**Giải thích các field:**
| Field | Mô tả |
|-------|-------|
| `totalRevenue` | Tổng doanh thu (tx PURCHASE COMPLETED) trong `[from, to]` |
| `coursesSold` | Số khóa bán ra = `COUNT(enrollments)` theo `enrolledAt` (gồm vé 0đ) |
| `paidPurchaseCount` | Số giao dịch mua **có doanh thu** (tx PURCHASE COMPLETED) |
| `totalTopUp` | Tổng tiền nạp ví COMPLETED (tham khảo — KHÔNG phải doanh thu) |
| `series[].period` | `yyyy-MM-dd` (DAY) hoặc `yyyy-MM` (MONTH) — chỉ chứa kỳ có giao dịch |
| `series[].revenue` | Doanh thu trong kỳ |
| `series[].count` | Số giao dịch mua có doanh thu trong kỳ |

> **Lưu ý:** `series` chỉ trả về các kỳ **có giao dịch** (không pad kỳ 0đ). FE có thể tự điền các kỳ
> trống nếu cần trục thời gian liền mạch.

**Response 400 (granularity sai):**
```json
{ "code": "BAD_REQUEST", "message": "granularity phải là DAY hoặc MONTH", "timestamp": "..." }
```
