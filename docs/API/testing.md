# Testing với Postman/Insomnia

[← Quay lại mục lục](./README.md)

---

## 13. Testing với Postman/Insomnia

### Authentication Flow:
1. **Register** → Lấy thông tin user (không có token)
2. **Login** → Lấy JWT token
3. **Gửi token** trong header `Authorization: Bearer <token>` cho các API protected

### Example Headers:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNRU0yQjRBMUQiLCJ1c2VySWQiOjEsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInJvbGUiOiJNRU1CRVIiLCJpc0ludGVybmFsIjpmYWxzZSwiaWF0IjoxNzE1Nzg4MjAwLCJleHAiOjE3MTU4NzQ2MDB9.xxxx
```

### Testing Scenarios:
1. **MEMBER (External)**: Xem courses public, nạp tiền, quote giá với voucher, mua khóa học (có / không voucher).
2. **MEMBER (Internal)**: Mua khóa học luôn 0đ; voucher bị bỏ qua; quote luôn `internalDiscount = true`.
3. **INSTRUCTOR**: Tạo course (mặc định ẩn) → thêm sections / lessons → chờ admin duyệt. Sau publish, không sửa được giá. Xem course của mình qua `/api/v1/instructor/courses`.
4. **STAFF**: Toàn quyền course / section / lesson, duyệt course (`PUBLISH_COURSE`), khóa giá (`LOCK_COURSE_PRICE`), quản lý voucher (`MANAGE_VOUCHER`).
5. **ADMIN_USER**: Quản lý user, course (CRUD) nhưng KHÔNG có quyền section / lesson, KHÔNG duyệt course, KHÔNG quản lý voucher.
6. **SUPER_ADMIN**: Toàn quyền hệ thống bao gồm `USE_VOUCHER` (mua test) và `MANAGE_VOUCHER`.

### Voucher / Course Approval flow gợi ý test:
1. STAFF login → `POST /api/v1/admin/vouchers` tạo voucher `WELCOME50`.
2. INSTRUCTOR login → `POST /api/v1/courses` tạo course (course ở DRAFT, không xuất hiện ở public list).
3. STAFF → `GET /api/v1/admin/courses/pending` thấy course chờ duyệt.
4. STAFF → `POST /api/v1/admin/courses/{id}/publish` để công khai. Course xuất hiện ở `GET /api/v1/courses`.
5. MEMBER login → `POST /api/v1/courses/{id}/quote` với `{ "voucherCode": "WELCOME50" }` xem giá preview.
6. MEMBER → `POST /api/v1/courses/{id}/purchase` với `{ "voucherCode": "WELCOME50" }` để mua thực sự. Server tính lại giá độc lập, ghi `Voucher_Usage` và audit log.
7. Kiểm tra `logs/purchase_ledger.jsonl` thấy 2 dòng JSONL: `PURCHASE_COMPLETED` (hoặc `VOUCHER_APPLIED`).

### WebSocket Top-up flow (dev):
1. Login → lấy JWT token.
2. Kết nối WebSocket với token (xem [wallet.md](./wallet.md) mục 14.4).
3. `POST /api/v1/wallet/top-up/init` → nhận `referenceCode`.
4. `POST /api/v1/webhook/mock?ref={referenceCode}` → cộng tiền.
5. FE nhận event `WALLET_UPDATED` → cập nhật số dư realtime.
