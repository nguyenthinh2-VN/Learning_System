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
| 2.1 | Auth & User Management | 3 | `docs/api-docs.md` §1 |
| 2.2 | Roles & Permissions (RBAC) | — | `docs/permission-matrix.md` |
| 2.3 | Course Management (CRUD) | 6 | `docs/api-docs.md` §6–7 |
| 2.4 | Course Approval & Visibility | 7 | `docs/api-docs.md` §10 |
| 2.5 | Course Section Management | 4 | `docs/api-docs.md` §8 |
| 2.6 | Course Lesson Management | 4 | `docs/api-docs.md` §9 |
| 2.7 | Voucher Pricing & Management | 6 | `docs/api-docs.md` §11–13 |
| 2.8 | Wallet Top-up + WebSocket | 4 | `docs/api-docs.md` §14 |
| 2.9 | My Enrollments | 1 | `docs/api-docs.md` §15 |
| 2.10 | Lesson Access Control | — | Kiểm tra enrollment trước khi trả `contentUrl` |

**Tổng: ~35 endpoints.** Xem đầy đủ tại `docs/api-docs.md`.

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

---

## 4. Database Schema

Chi tiết cột và index: `docs/architecture-directory-tree.md`

| Bảng | Mô tả |
|------|-------|
| `users` | Tài khoản, `balance`, `is_internal` |
| `roles` / `permissions` / `role_permissions` | RBAC n-n, 19 permissions seed sẵn |
| `courses` | + 4 cột approval: `published`, `price_locked`, `published_at`, `published_by` |
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

## 8. Trạng thái hiện tại (2026-05-26)

**Đã hoàn thiện trong session hôm nay:**
- Wallet Top-up (Mock gateway + WebSocket push)
- Admin top-up endpoint
- My Enrollments (`GET /api/v1/users/me/enrollments`)
- Lesson access control (MEMBER phải enrolled mới xem được)

**Chưa làm (next steps):**
- Ghép VietQR thật (chỉ cần thêm `VietQrGateway` + `VietQrWebhookController`)
- Voucher detail endpoint (`GET /admin/vouchers/{id}`)
- Progress tracking
- Notification system

Chi tiết thay đổi hôm nay: `docs/changelog.md`
