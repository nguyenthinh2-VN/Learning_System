# Changelog — Learning System Spring

Lịch sử các thay đổi theo từng session phát triển.

---

## 2026-05-26 — Wallet Top-up + WebSocket + My Enrollments + Lesson Access Control

### Tính năng mới

#### Wallet Top-up (Open/Closed Pattern)
- `PaymentGateway` interface port — không bao giờ sửa khi thêm provider mới.
- `MockPaymentGateway` — active khi `payment.provider=mock`, trả `displayType=MESSAGE` hướng dẫn test.
- `InitTopUpUseCase` — tạo pending `WalletTransaction` + gọi gateway.
- `CompleteTopUpUseCase` — **dùng chung** cho mọi provider: verify pending → cộng tiền → trả `Result` để push WS.
- `AdminTopUpUseCase` — admin cộng tiền trực tiếp (COMPLETED ngay).
- `MockWebhookController` — `POST /api/v1/webhook/mock?ref=...` để test flow (chỉ active khi mock).
- `WalletController` — `POST /api/v1/wallet/top-up/init`.
- `AdminUserController` — thêm `POST /api/v1/admin/users/{userId}/top-up` (SUPER_ADMIN only).
- Bảng `wallet_transactions` — SQL migration tại `docs/sql/wallet_transactions.sql`.

**Để ghép VietQR sau:** chỉ tạo `VietQrGateway` + `VietQrWebhookController` + đổi `payment.provider=vietqr`. Không sửa code cũ.

#### WebSocket Realtime
- `WebSocketConfig` — STOMP over SockJS, JWT interceptor trên CONNECT frame.
- `WalletNotificationService` — push `WALLET_UPDATED` event tới `/user/queue/wallet` của đúng user.
- FE subscribe: `client.subscribe('/user/queue/wallet', handler)` sau khi login.
- Payload: `{ event, userId, newBalance, addedAmount, source, referenceCode, note, timestamp }`.
- `source`: `MOCK` | `VIETQR` | `ADMIN`.

#### My Enrollments
- `GET /api/v1/users/me/enrollments?page=0&size=20` — lấy danh sách khóa học đã mua.
- `GetMyEnrollmentsUseCase` — validate page/size, trả `PageResult<MyEnrollmentOutput>`.
- `EnrollmentRepository.findByUserId()` — sort `enrolledAt DESC`, Spring Page → `PageResult`.
- `PageResult.map()` — thêm method transform items.

#### Lesson Access Control (siết chặt)
- `LessonAuthorizationService.authorizeView()` — thêm tham số `isEnrolled`.
- Ma trận quyền xem lesson:
  - `SUPER_ADMIN`, `STAFF` → luôn được phép.
  - `INSTRUCTOR` → chỉ được phép nếu sở hữu course.
  - `MEMBER` → chỉ được phép nếu `isEnrolled = true`.
  - `ADMIN_USER` → luôn từ chối (không thuộc luồng học tập).
- `GetLessonsUseCase` — inject `EnrollmentRepository`, gọi `authorizeView` trước khi trả lesson.
- `GetLessonsInput` — thêm `requesterId` + `requesterRole`.
- `CourseLessonController.getLessons` — truyền requesterId/role từ JWT vào use case.

### Files đã sửa
| File | Thay đổi |
|------|---------|
| `EnrollmentRepository` (port) | Thêm `findByUserId()` |
| `JpaEnrollmentRepository` | Thêm `findByUserId(Pageable)` |
| `EnrollmentRepositoryImpl` | Implement `findByUserId` với sort DESC |
| `GetLessonsInput` | Thêm `requesterId`, `requesterRole` |
| `GetLessonsUseCase` | Inject `EnrollmentRepository`, gọi `authorizeView` |
| `LessonAuthorizationService` | Sửa `authorizeView` nhận `isEnrolled` |
| `CourseLessonController` | Truyền requesterId/role vào `GetLessonsInput` |
| `UserController` | Thêm `GET /me/enrollments` |
| `AdminUserController` | Thêm `POST /{userId}/top-up` |
| `SecurityConfig` | Permit `/ws/**`, `/api/v1/webhook/**` |
| `PageResult` | Thêm `map()` method |
| `application-dev.yaml` | Thêm payment + wallet config |
| `pom.xml` | Thêm `spring-boot-starter-websocket`, `spring-boot-starter-test` |

### Files mới tạo
- `domain/model/Wallet/` — `WalletTransaction`, `TxStatus`, `TxSource`
- `adapter/repository/jpa/WalletEntity/WalletTransactionJpaEntity`
- `adapter/repository/JpaWalletTransactionRepository`, `WalletTransactionRepositoryImpl`
- `application/port/PaymentGateway`, `PaymentInitResult`
- `application/repository/Wallet/WalletTransactionRepository`
- `application/dto/Wallet/InitTopUpOutput`, `AdminTopUpOutput`
- `application/dto/User/MyEnrollmentOutput`
- `application/usecase/Wallet/InitTopUpUseCase`, `CompleteTopUpUseCase`, `AdminTopUpUseCase`
- `application/usecase/User/GetMyEnrollmentsUseCase`
- `adapter/dto/request/Wallet/InitTopUpRequest`, `AdminTopUpRequest`
- `adapter/dto/response/MyEnrollmentResponse`
- `adapter/controller/WalletController`, `MockWebhookController`
- `infrastructure/payment/MockPaymentGateway`
- `infrastructure/config/WebSocketConfig`
- `infrastructure/service/WalletNotificationService`
- `docs/sql/wallet_transactions.sql`

### Tests mới
- `WalletTransactionTest` — domain model (12 tests)
- `WalletNotificationServiceTest` — publisher (5 tests)
- `CompleteTopUpUseCaseTest` — use case dùng chung (5 tests)
- `AdminTopUpUseCaseTest` — admin top-up (6 tests)
- `WalletTopUpFlowTest` — end-to-end flow publisher→subscriber (4 flows)
- `GetMyEnrollmentsUseCaseTest` — pagination + validation (10 tests)
- `LessonAuthorizationServiceTest` — ma trận quyền xem lesson (8 tests)
- `GetLessonsUseCaseTest` — use case với enrollment check (7 tests)

---

## 2026-05-25 — Voucher Bug Fixes

Chi tiết tại `docs/fix-summary-2026-05-25.md`.

- FIX-A: Voucher null status/scope guard.
- FIX-C: `CourseOwnershipPolicy` null-guard.
- FIX-D: `PricingEngine` precision-loss — fold về `noDiscount` khi discount = 0.
- DEC-1: Cho sửa `code`/`type`/`value` khi `usedCount == 0`.
- OPT-1: Gộp 2 query voucher trong checkout thành 1.
- OPT-2: Xóa `userRepository.save(user)` dư trong nhánh internal.

---

## 2026-05-24 — Voucher Pricing & Course Approval

Chi tiết tại `docs/plan/plan-voucher-pricing.md` và `docs/plan/plan-course-approval.md`.

### Course Approval
- Thêm `published`, `price_locked`, `published_at`, `published_by` vào `courses`.
- 3 use case: `PublishCourseUseCase`, `UnpublishCourseUseCase`, `UpdateCoursePriceUseCase`.
- 4 scope cho `GetCourseListUseCase`: `PUBLIC`, `PENDING`, `ALL`, `INSTRUCTOR`.
- 2 controller mới: `AdminCourseController`, `InstructorCourseController`.
- 2 permission: `PUBLISH_COURSE`, `LOCK_COURSE_PRICE`.

### Voucher Pricing
- 6 domain model Voucher + 12 exceptions.
- 2 pure domain service: `PricingEngine`, `VoucherValidator`.
- 6 use case Voucher (4 CRUD + `QuotePricingUseCase` + `ApplyVoucherCheckoutUseCase`).
- Anti-tampering 4 mặt + pessimistic lock `User → Course → Voucher`.
- 2 permission: `MANAGE_VOUCHER`, `USE_VOUCHER`.
- Audit: `VOUCHER_APPLIED`, `VOUCHER_REJECTED` vào `purchase_ledger.jsonl`.
