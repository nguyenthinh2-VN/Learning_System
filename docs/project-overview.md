# Learning System — Project Overview

Tài liệu này là **entry point** cho AI Agent hoặc Developer mới. Đọc file này trước, sau đó đi vào file chi tiết theo nhu cầu.

---

## 1. Stack & Kiến trúc

| | |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.x, Spring Security, Spring Data JPA, Spring WebSocket |
| **Database** | MySQL |
| **Auth** | Stateless JWT (24h) |
| **Build** | Maven |
| **Architecture** | Clean Architecture — 4 layer: `domain` → `application` → `adapter` → `infrastructure` |

Quy tắc kiến trúc chi tiết: `.claude/rules/spring-clean-architecture.md`

---

## 2. Tính năng đã hoàn thiện

| # | Tính năng | Endpoints | Chi tiết |
|---|-----------|-----------|---------|
| 2.1 | Auth & User Management | 4 | `docs/api-docs.md` §1, §17 |
| 2.2 | Roles & Permissions (RBAC) | — | `docs/permission-matrix.md` |
| 2.3 | Course Management (CRUD) | 6 | `docs/api-docs.md` §6–7 |
| 2.4 | Course Approval & Visibility | 7 | `docs/api-docs.md` §10 |
| 2.5 | Course Section Management | 4 | `docs/api-docs.md` §8 |
| 2.6 | Course Lesson Management | 4 | `docs/api-docs.md` §9 |
| 2.7 | Voucher Pricing & Management | 6 | `docs/api-docs.md` §11–13 |
| 2.8 | Wallet Top-up + WebSocket | 4 | `docs/api-docs.md` §14 |
| 2.9 | My Enrollments | 1 | `docs/api-docs.md` §15 |
| 2.10 | Lesson Access Control | — | Kiểm tra enrollment trước khi trả `contentUrl` |
| 2.11 | User Profile & Wallet Balance | 1 | `docs/api-docs.md` §16 |
| 2.12 | Admin User Listing | 1 | `docs/api-docs.md` §17 |
| 2.13 | Course `thumbnailUrl` | — | `docs/api-docs.md` §18 |

**Tổng: ~36 endpoints.** Xem đầy đủ tại `docs/api-docs.md`.

---

## 3. Điểm thiết kế quan trọng (cần biết trước khi code)

### 3.1. Authorization
- Mọi ownership check trên Course đi qua `CourseOwnershipPolicy` (static, domain layer).
- `LessonAuthorizationService.authorizeView()` kiểm tra enrollment — MEMBER phải có enrollment row mới xem được `contentUrl`.
- `ADMIN_USER` không có quyền thao tác Section/Lesson (chỉ Course-level).

### 3.2. Wallet & Payment
- `PaymentGateway` interface — Open/Closed: thêm VietQR chỉ cần tạo `VietQrGateway` + `VietQrWebhookController`, không sửa code cũ.
- `CompleteTopUpUseCase` dùng chung cho mọi provider (Mock, VietQR).
- WebSocket push `WALLET_UPDATED` tới `/user/queue/wallet` sau mỗi lần cộng tiền.

### 3.3. Voucher & Checkout
- Entry point duy nhất cho purchase: `ApplyVoucherCheckoutUseCase`.
- Anti-tampering: DTO chỉ nhận `voucherCode`, server tính lại giá từ DB.
- Pessimistic lock thứ tự: `User → Course → Voucher`.

### 3.4. Concurrency
- Mọi API tài chính dùng `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- `wallet_transactions` có UNIQUE `reference_code` — idempotent webhook.

### 3.5. User Profile & Admin User Listing
- `GET /api/v1/users/me/profile` — trả thông tin cá nhân + `balance` cho FE hiển thị header/navbar; kết hợp WebSocket `/user/queue/wallet` để cập nhật số dư realtime.
- `GET /api/v1/admin/users` — listing user có phân trang + tìm kiếm (keyword theo tên/email), gate bằng `@PreAuthorize("hasAnyRole('ADMIN_USER','SUPER_ADMIN')")`.
- Tạo user (`POST /api/v1/admin/users`) gate bằng role `ADMIN_USER`, `SUPER_ADMIN`, `STAFF`.

---

## 4. Database Schema

Chi tiết cột và index: `docs/architecture-directory-tree.md`

| Bảng | Mô tả |
|------|-------|
| `users` | Tài khoản, `balance`, `is_internal` |
| `roles` / `permissions` / `role_permissions` | RBAC n-n, 18 permissions seed sẵn (+ `CREATE_USER` định nghĩa trong matrix nhưng chưa seed) |
| `courses` | + 4 cột approval: `published`, `price_locked`, `published_at`, `published_by`; + `thumbnail_url` (ảnh bìa) |
| `course_sections` | Chương học, `order_index` |
| `course_lessons` | Bài giảng, `content_url`, `order_index` |
| `enrollments` | Lịch sử mua: `user_id`, `course_id`, `paid_price`, `enrolled_at` |
| `vouchers` | PERCENT/FIXED, scope, usage limit |
| `voucher_courses` | Mapping voucher ↔ course (scope = SPECIFIC) |
| `voucher_usages` | UNIQUE `(voucher_id, enrollment_id)` chống race |
| `wallet_transactions` | Audit trail nạp tiền, UNIQUE `reference_code` |

SQL migration mới nhất: `docs/sql/wallet_transactions.sql`

---

## 5. Design Patterns

| Pattern | Nơi áp dụng |
|---------|-------------|
| Strategy | Course authorization (`CourseStrategyFactory`) |
| Strategy | Username generation (`UsernameGeneratorFactory`) |
| Strategy | Payment provider (`PaymentGateway` interface) |
| Policy (Static) | `CourseOwnershipPolicy` — ownership check tập trung |
| Pure Domain Service | `PricingEngine`, `VoucherValidator` — không Spring/JPA |
| Repository | Port/Adapter pattern cho mọi DB access |
| Value Object | `PriceQuote` — kết quả tính giá bất biến |
| Soft-delete | Voucher (`status = INACTIVE`) |

---

## 6. Quy tắc bắt buộc khi phát triển tiếp

1. **Domain model** không có annotation JPA/Spring. JPA Entity ở `adapter/repository/jpa/`.
2. **Pure domain service** (`PricingEngine`, `VoucherValidator`, `CourseOwnershipPolicy`) không dùng `@Service`. Đăng ký bean qua `DomainServiceConfig`.
3. **Constructor injection only** — cấm `@Autowired` field/setter.
4. **Thêm permission mới** → cập nhật 3 nơi: `DataInitializer`, `docs/permission-matrix.md`, `ErrorCode` + `GlobalExceptionHandler`.
5. **Audit log tài chính** → chỉ qua `PurchaseLedgerService`.
6. **Lesson access** → kiểm tra enrollment trong `GetLessonsUseCase`, không bypass qua `@PreAuthorize`.
7. **WebSocket push** → chỉ qua `WalletNotificationService` (best-effort, không throw).

---

## 7. Tài liệu liên quan

| File | Nội dung |
|------|---------|
| `docs/api-docs.md` | Toàn bộ API request/response spec |
| `docs/permission-matrix.md` | Ma trận 5 roles × 19 permissions |
| `docs/architecture-directory-tree.md` | Cây thư mục đầy đủ (cập nhật) |
| `docs/changelog.md` | Lịch sử thay đổi theo session |
| `docs/plan/` | Kế hoạch chi tiết từng feature |
| `docs/sql/` | SQL migration scripts |
| `.claude/rules/spring-clean-architecture.md` | Quy tắc kiến trúc + code examples |

---

## 8. Trạng thái hiện tại (2026-05-28)

**Đã hoàn thiện:**
- Wallet Top-up (Mock gateway + WebSocket push)
- Admin top-up endpoint
- My Enrollments (`GET /api/v1/users/me/enrollments`)
- Lesson access control (MEMBER phải enrolled mới xem được)
- User Profile + số dư ví (`GET /api/v1/users/me/profile`)
- Admin liệt kê người dùng (`GET /api/v1/admin/users` — phân trang + tìm kiếm)
- Course `thumbnailUrl` (ảnh bìa khóa học)

**Chưa làm (next steps):**
- Ghép VietQR thật (chỉ cần thêm `VietQrGateway` + `VietQrWebhookController`)
- Voucher detail endpoint (`GET /admin/vouchers/{id}`)
- Progress tracking
- Notification system

Chi tiết thay đổi: `docs/changelog.md`
