# Voucher Pricing & Management

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 11. Voucher Pricing & Management

Tính năng voucher cho phép STAFF / SUPER_ADMIN tạo mã giảm giá; MEMBER (External / Internal) áp dụng voucher khi mua khóa học.

**Trọng tâm thiết kế — chống giả mạo (anti-tampering):**

| Mối lo | Cách giải quyết |
|--------|-----------------|
| Tampering `courseId` | Server đọc giá trực tiếp từ DB theo `courseId` ở path |
| Tampering `price` từ client | DTO chỉ khai báo `voucherCode`, mọi field giá bị bỏ qua |
| Replay quote cũ | Mỗi `/purchase` tính lại giá từ đầu, không tin quote |
| Race condition voucher quota | Pessimistic write lock trên hàng voucher + UNIQUE `(voucherId, enrollmentId)` |

**Phân quyền:**

| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Quản lý voucher (CRUD) | ❌ | ❌ | ✅ (`MANAGE_VOUCHER`) | ❌ | ✅ |
| Quote / áp voucher khi mua | ✅ (`USE_VOUCHER`) | ❌ | ❌ | ❌ | ✅ |

> Internal Member (`isInternal = true`): luôn `paidPrice = 0`, voucher bị bỏ qua khi mua. Voucher chỉ có ý nghĩa với External Member.

---

### 11.1. Tạo Voucher (Admin)

**Endpoint:** `POST /api/v1/admin/vouchers`

**Yêu cầu quyền:** `MANAGE_VOUCHER` (Role STAFF, SUPER_ADMIN).

**Request Body:**
```json
{
  "code": "WELCOME50",
  "type": "PERCENT",
  "value": 50,
  "scope": "ALL_COURSES",
  "validFrom": "2026-05-01T00:00:00",
  "validTo": "2026-12-31T23:59:59",
  "minOrderAmount": 100000,
  "maxDiscount": 200000,
  "usageLimit": 1000,
  "usagePerUser": 1,
  "applicableCourseIds": null
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| code | String | Yes | 4–32 ký tự, regex `^[A-Za-z0-9_-]+$`, lưu uppercase, UNIQUE |
| type | Enum | Yes | `PERCENT` hoặc `FIXED` |
| value | BigDecimal | Yes | > 0; nếu `PERCENT` thì 0 < value ≤ 100 |
| scope | Enum | Yes | `ALL_COURSES` hoặc `SPECIFIC_COURSES` |
| validFrom | LocalDateTime | Yes | ISO-8601 |
| validTo | LocalDateTime | Yes | ISO-8601, ≥ validFrom |
| minOrderAmount | BigDecimal | No | ≥ 0, mặc định 0 |
| maxDiscount | BigDecimal | No | ≥ 0, chỉ áp dụng khi `PERCENT`; 0 = không giới hạn |
| usageLimit | Long | No | ≥ 0; 0 = không giới hạn |
| usagePerUser | Integer | No | ≥ 0; 0 = không giới hạn |
| applicableCourseIds | Set\<Long\> | Tùy `scope` | Bắt buộc và không rỗng khi `scope = SPECIFIC_COURSES` |

**Response 201:**
```json
{
  "status": 201,
  "message": "Tạo voucher thành công",
  "data": {
    "id": 1,
    "code": "WELCOME50",
    "type": "PERCENT",
    "value": 50,
    "status": "ACTIVE",
    "scope": "ALL_COURSES",
    "validFrom": "2026-05-01T00:00:00",
    "validTo": "2026-12-31T23:59:59",
    "minOrderAmount": 100000,
    "maxDiscount": 200000,
    "usageLimit": 1000,
    "usagePerUser": 1,
    "applicableCourseIds": null,
    "usedCount": 0,
    "createdAt": "2026-05-24T10:00:00",
    "updatedAt": "2026-05-24T10:00:00"
  },
  "timestamp": "2026-05-24T10:00:00"
}
```

**Response 409 (Code đã tồn tại):**
```json
{
  "code": "VOUCHER_CODE_ALREADY_EXISTS",
  "message": "Voucher code WELCOME50 đã tồn tại.",
  "timestamp": "2026-05-24T10:00:00"
}
```

---

### 11.2. Cập nhật Voucher

**Endpoint:** `PUT /api/v1/admin/vouchers/{id}`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

> **Immutable fields** (`code`, `type`, `value`): có thể gửi để sửa, nhưng chỉ được chấp nhận khi voucher **chưa có bất kỳ `Voucher_Usage` nào** (`usedCount = 0`). Nếu đã có usage → 400 `VOUCHER_IMMUTABLE_FIELD`. Không gửi (để `null`) = giữ nguyên giá trị cũ.
>
> **Soft fields** (`status`, `scope`, `validFrom`, `validTo`, `minOrderAmount`, `maxDiscount`, `usageLimit`, `usagePerUser`, `applicableCourseIds`): luôn được sửa, bắt buộc gửi đầy đủ.

**Request Body:**
```json
{
  "code": "SUMMER50",
  "type": "PERCENT",
  "value": 50,
  "status": "ACTIVE",
  "scope": "ALL_COURSES",
  "validFrom": "2026-06-01T00:00:00",
  "validTo": "2026-12-31T23:59:59",
  "minOrderAmount": 100000,
  "maxDiscount": 200000,
  "usageLimit": 500,
  "usagePerUser": 1,
  "applicableCourseIds": null
}
```

| Field | Type | Required | Ghi chú |
|-------|------|----------|---------|
| code | String | No | Nullable — chỉ sửa được khi `usedCount = 0`; regex `^[A-Za-z0-9_-]{1,32}$` |
| type | Enum | No | Nullable — chỉ sửa được khi `usedCount = 0`; `PERCENT` hoặc `FIXED` |
| value | BigDecimal | No | Nullable — chỉ sửa được khi `usedCount = 0`; > 0 |
| status | Enum | **Yes** | `ACTIVE` hoặc `INACTIVE` |
| scope | Enum | **Yes** | `ALL_COURSES` hoặc `SPECIFIC_COURSES` |
| validFrom | LocalDateTime | **Yes** | ISO-8601 |
| validTo | LocalDateTime | **Yes** | ISO-8601, ≥ validFrom |
| minOrderAmount | BigDecimal | No | ≥ 0 |
| maxDiscount | BigDecimal | No | ≥ 0 |
| usageLimit | Long | No | ≥ 0; 0 = không giới hạn |
| usagePerUser | Integer | No | ≥ 0; 0 = không giới hạn |
| applicableCourseIds | Set\<Long\> | Tùy `scope` | Bắt buộc khi `scope = SPECIFIC_COURSES` |

**Response 200:** Trả về voucher đã cập nhật với `usedCount` và metadata mới.

**Response 400 — usageLimit nhỏ hơn usedCount:**
```json
{
  "code": "VOUCHER_USAGE_LIMIT_TOO_LOW",
  "message": "usageLimit mới (10) không được nhỏ hơn usedCount hiện tại (15).",
  "timestamp": "2026-05-24T11:00:00"
}
```

**Response 400 — sửa field bất biến khi đã có usage:**
```json
{
  "code": "VOUCHER_IMMUTABLE_FIELD",
  "message": "Voucher đã có lượt dùng, không thể thay đổi field: code",
  "timestamp": "2026-05-24T11:00:00"
}
```

**Response 409 — code mới đã tồn tại:**
```json
{
  "code": "VOUCHER_CODE_ALREADY_EXISTS",
  "message": "Voucher code SUMMER50 đã tồn tại.",
  "timestamp": "2026-05-24T11:00:00"
}
```

---

### 11.3. Soft-delete Voucher

**Endpoint:** `DELETE /api/v1/admin/vouchers/{id}`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

> Voucher bị set `status = INACTIVE` (soft-delete) để bảo toàn lịch sử `Voucher_Usage`. Voucher đã có usage tuyệt đối không được xóa cứng.

**Response 200:**
```json
{
  "status": 200,
  "message": "Voucher đã được vô hiệu hóa (soft-delete)",
  "timestamp": "2026-05-24T11:00:00"
}
```

---

### 11.4. Danh sách Voucher (Admin)

**Endpoint:** `GET /api/v1/admin/vouchers`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

**Query Parameters:** `page` (default 0), `size` (default 10).

**Response 200:** Phân trang trả về danh sách voucher kèm `usedCount`.

---

### 11.5. Quote (Preview giá)

**Endpoint:** `POST /api/v1/courses/{courseId}/quote`

**Yêu cầu quyền:** Đăng nhập. Role MEMBER hoặc SUPER_ADMIN nếu gửi `voucherCode` (`USE_VOUCHER` permission). Role khác mà gửi `voucherCode` → 403.

> Read-only, không tiêu thụ voucher. Có thể gọi nhiều lần.

**Request Body (tùy chọn):**
```json
{
  "voucherCode": "WELCOME50"
}
```

> **Anti-tampering:** Body CHỈ chấp nhận `voucherCode`. Mọi field giá khác bị bỏ qua.

**Response 200 — không voucher:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 0.00,
    "finalPrice": 500000.00,
    "voucherApplied": false,
    "voucherCode": null,
    "voucherType": null,
    "internalDiscount": false,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

**Response 200 — voucher hợp lệ:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 200000.00,
    "finalPrice": 300000.00,
    "voucherApplied": true,
    "voucherCode": "WELCOME50",
    "voucherType": "PERCENT",
    "internalDiscount": false,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

**Response 200 — Internal Member (luôn 0đ, voucher bị bỏ qua):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 500000.00,
    "finalPrice": 0.00,
    "voucherApplied": false,
    "voucherCode": null,
    "voucherType": null,
    "internalDiscount": true,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

> `quotedAt` chỉ mang tính thông tin. KHÔNG được dùng làm chứng cứ ràng buộc giá ở luồng checkout — server luôn tính lại tại thời điểm `/purchase`.

---

### 11.6. Checkout với Voucher

Đã được mô tả trong [wallet.md](./wallet.md) (mục 7.2 Mua Khóa Học). Tóm tắt:

- Endpoint: `POST /api/v1/courses/{id}/purchase` với body `{ "voucherCode": "..." }`.
- Server giữ pessimistic lock trên User → Course → Voucher (thứ tự cố định, chống deadlock).
- Validate voucher LẦN NỮA sau khi giữ lock — preview pass không có nghĩa checkout pass.
- UNIQUE `(voucher_id, enrollment_id)` ở DB chống race tạo 2 usage cho cùng enrollment.
- Audit log `event = "VOUCHER_APPLIED"` được ghi vào `logs/purchase_ledger.jsonl`.

---

### Voucher Validation Rules

Validator kiểm tra theo thứ tự cố định (cùng đầu vào → cùng exception):

1. `status = ACTIVE`
2. `validFrom ≤ now`
3. `now ≤ validTo`
4. Scope (course nằm trong `applicableCourseIds` nếu `SPECIFIC_COURSES`)
5. `originalPrice ≥ minOrderAmount`
6. `usedCount < usageLimit` (toàn cục, 0 = không giới hạn)
7. `perUserCount < usagePerUser` (theo user, 0 = không giới hạn)

---

### Pricing Engine Rules

| Voucher Type | Công thức tính `discountAmount` |
|--------------|---------------------------------|
| `null` | 0 (không giảm) |
| `PERCENT` | `min(originalPrice × value / 100, maxDiscount)` (HALF_UP, scale 2) — `maxDiscount = 0` nghĩa không giới hạn |
| `FIXED` | `min(value, originalPrice)` |

Invariants luôn được enforce:
- `0 ≤ discountAmount ≤ originalPrice`
- `finalPrice = originalPrice − discountAmount`
- `0 ≤ finalPrice ≤ originalPrice`
- Mọi BigDecimal `scale = 2`, rounding `HALF_UP`
- `originalPrice = 0` → `discountAmount = 0`, `finalPrice = 0` (voucher vô nghĩa với khóa miễn phí)

---

### Mã lỗi Voucher

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `VOUCHER_NOT_FOUND` | Không tìm thấy voucher với code đã chuẩn hóa |
| 400 | `VOUCHER_INACTIVE` | Voucher đang ở trạng thái INACTIVE |
| 400 | `VOUCHER_NOT_YET_ACTIVE` | Chưa tới thời điểm `validFrom` |
| 400 | `VOUCHER_EXPIRED` | Đã quá `validTo` |
| 400 | `VOUCHER_NOT_APPLICABLE` | Course không nằm trong `applicableCourseIds` |
| 400 | `VOUCHER_MIN_ORDER_NOT_MET` | `originalPrice < minOrderAmount` |
| 409 | `VOUCHER_USAGE_LIMIT_REACHED` | Đã đạt giới hạn lượt dùng tổng |
| 409 | `VOUCHER_USAGE_PER_USER_EXCEEDED` | User đã đạt giới hạn lượt dùng cá nhân |
| 403 | `VOUCHER_USE_DENIED` | Role không được phép sử dụng voucher |
| 409 | `VOUCHER_CODE_ALREADY_EXISTS` | Code đã tồn tại khi tạo voucher |
| 400 | `VOUCHER_USAGE_LIMIT_TOO_LOW` | `usageLimit` mới nhỏ hơn `usedCount` hiện tại |
| 400 | `VOUCHER_IMMUTABLE_FIELD` | Sửa field bất biến (code/type/value) khi voucher đã có usage |
