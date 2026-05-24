# Kế hoạch: Voucher Pricing & Management

> **Tham chiếu requirements**: `.kiro/specs/voucher-pricing/requirements.md` (12 requirements + 19 correctness properties).

## 1. Tổng quan yêu cầu

Xây dựng tính năng **Voucher** cho hệ thống mua khóa học, gồm 2 luồng nghiệp vụ chính:

1. **Quote (Preview)** — Client gửi `courseId` + `voucherCode`, server tính lại giá hoàn toàn từ DB rồi trả về. Không khóa voucher, không trừ tiền.
2. **Checkout** — Mua khóa học có dùng voucher. Server **luôn tính lại giá từ đầu** (server-side recomputation), khóa voucher (pessimistic lock), trừ tiền ví, ghi `Voucher_Usage`.

**Trọng tâm thiết kế — chống giả mạo (anti-tampering):**

| Mối lo | Cách giải quyết |
|--------|-----------------|
| Tampering `courseId` (đổi ID rẻ hơn) | Server chỉ tin `courseId` từ path, luôn `findById` cứng và lấy giá hiện hành từ DB |
| Tampering `price` / `finalPrice` từ client | DTO chỉ khai báo `voucherCode`. Mọi field giá khác bị bỏ qua khi deserialize |
| Replay (dùng quote cũ để checkout) | Không có `quoteToken`. Mỗi checkout tính lại độc lập |
| Race condition (voucher còn 1 lượt) | `@Lock(PESSIMISTIC_WRITE)` trên row voucher + UNIQUE `(voucherId, enrollmentId)` |

**Phân quyền:**

| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Quản lý voucher (CRUD) | [ ] | [ ] | [x] | [ ] | [x] |
| Xem chi tiết & danh sách voucher (admin panel) | [ ] | [ ] | [x] | [ ] | [x] |
| Áp dụng voucher khi quote / checkout | [x] | [ ] | [ ] | [ ] | [x] |

> **Lưu ý:** `INSTRUCTOR` và `ADMIN_USER` không có quyền mua khóa học → không có `USE_VOUCHER`. `ADMIN_USER` chuyên trách user management, không quản lý monetization → không có `MANAGE_VOUCHER` (thuộc về STAFF / SUPER_ADMIN).

---

## 2. Cập nhật Permission Matrix

Thêm 2 permission mới vào `docs/permission-matrix.md`:

| # | Permission | Mô tả | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|---|-----------|-------|--------|------------|-------|------------|-------------|
| 16 | `MANAGE_VOUCHER` | Tạo / sửa / xóa / xem voucher trong panel admin | [ ] | [ ] | [x] | [ ] | [x] |
| 17 | `USE_VOUCHER` | Áp dụng voucher khi quote / mua khóa học | [x] | [ ] | [ ] | [ ] | [x] |

**Cập nhật `DataInitializer`:** seed thêm 2 permission, gán đúng role.

---

## 3. API Endpoints

### 3A. Admin Voucher (CRUD) — yêu cầu `MANAGE_VOUCHER`

| Method | URL | Mô tả |
|--------|-----|-------|
| `POST` | `/api/v1/admin/vouchers` | Tạo voucher mới |
| `GET` | `/api/v1/admin/vouchers` | Danh sách voucher có phân trang |
| `GET` | `/api/v1/admin/vouchers/{id}` | Chi tiết voucher (kèm `usedCount`, `effectiveStatus`) |
| `PUT` | `/api/v1/admin/vouchers/{id}` | Cập nhật voucher (ràng buộc Req. 2.9 nếu đã có usage) |
| `DELETE` | `/api/v1/admin/vouchers/{id}` | Soft-delete (set `status = INACTIVE`) |
| `GET` | `/api/v1/admin/vouchers/{id}/usages` | Danh sách lượt dùng của voucher |

### 3B. Quote (Preview) — yêu cầu `USE_VOUCHER`

| Method | URL | Mô tả |
|--------|-----|-------|
| `POST` | `/api/v1/courses/{courseId}/quote` | Tính giá xem trước, không tiêu hao lượt dùng |

Body: `{ "voucherCode": "WELCOME50" }` (nullable). Mọi field giá khác bị bỏ qua.

### 3C. Checkout — yêu cầu đăng nhập (mở rộng từ `PurchaseCourseUseCase` hiện có)

| Method | URL | Mô tả |
|--------|-----|-------|
| `POST` | `/api/v1/courses/{courseId}/purchase` | Mua khóa học, voucherCode là tùy chọn |

Body: `{ "voucherCode": "WELCOME50" }` hoặc `{}` để mua không voucher.

---

## 4. Mô hình dữ liệu (DB Schema)

### 4A. Bảng `vouchers`

```sql
CREATE TABLE vouchers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,            -- Lưu UPPERCASE chuẩn hóa
    type ENUM('PERCENT','FIXED') NOT NULL,
    value DECIMAL(19,2) NOT NULL,                -- > 0
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    scope ENUM('ALL_COURSES','SPECIFIC_COURSES') NOT NULL,
    min_order_amount DECIMAL(19,2) DEFAULT 0,    -- ≥ 0
    max_discount DECIMAL(19,2) DEFAULT 0,        -- ≥ 0, chỉ dùng khi PERCENT, 0 = không giới hạn
    usage_limit BIGINT DEFAULT 0,                -- 0 = không giới hạn
    usage_per_user INT DEFAULT 0,                -- 0 = không giới hạn
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_voucher_status_validto (status, valid_to)
);
```

### 4B. Bảng `voucher_courses` (mapping cho `scope = SPECIFIC_COURSES`)

```sql
CREATE TABLE voucher_courses (
    voucher_id BIGINT NOT NULL,
    course_id  BIGINT NOT NULL,
    PRIMARY KEY (voucher_id, course_id),
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE CASCADE
);
```

### 4C. Bảng `voucher_usages`

```sql
CREATE TABLE voucher_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_id BIGINT NOT NULL,
    original_price DECIMAL(19,2) NOT NULL,
    discount_amount DECIMAL(19,2) NOT NULL,
    final_price DECIMAL(19,2) NOT NULL,
    applied_at DATETIME NOT NULL,
    UNIQUE KEY uk_voucher_enrollment (voucher_id, enrollment_id),
    INDEX idx_voucher_usage_voucher (voucher_id),
    INDEX idx_voucher_usage_user (user_id),
    FOREIGN KEY (voucher_id)    REFERENCES vouchers(id),
    FOREIGN KEY (user_id)       REFERENCES users(id),
    FOREIGN KEY (course_id)     REFERENCES courses(id),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id)
);
```

> UNIQUE `(voucher_id, enrollment_id)` ngăn race condition tạo 2 voucher_usage cho cùng 1 enrollment ở mức DB (Req. 8.4).

---

## 5. Các file cần tạo mới

### 5A. Domain Layer — Model

```
domain/model/Voucher/
  ├── Voucher.java                       (NEW) — id, code, type, value, status, validFrom/To, scope,
  │                                              minOrderAmount, maxDiscount, usageLimit, usagePerUser,
  │                                              applicableCourseIds, createdAt, updatedAt
  ├── VoucherType.java                   (NEW) — enum PERCENT, FIXED
  ├── VoucherStatus.java                 (NEW) — enum ACTIVE, INACTIVE
  ├── VoucherScope.java                  (NEW) — enum ALL_COURSES, SPECIFIC_COURSES
  ├── VoucherUsage.java                  (NEW) — id, voucherId, userId, courseId, enrollmentId,
  │                                              originalPrice, discountAmount, finalPrice, appliedAt
  └── PriceQuote.java                    (NEW) — value object: originalPrice, discountAmount, finalPrice,
                                                 voucherApplied, voucherCode (nullable), voucherType (nullable)
```

### 5B. Domain Exceptions

```
domain/exception/
  ├── VoucherNotFoundException.java                  (NEW) — 404
  ├── VoucherInactiveException.java                  (NEW) — 400
  ├── VoucherNotYetActiveException.java              (NEW) — 400
  ├── VoucherExpiredException.java                   (NEW) — 400
  ├── VoucherUsageLimitReachedException.java         (NEW) — 400 (quote) / 409 (checkout)
  ├── VoucherUsagePerUserExceededException.java      (NEW) — 400
  ├── VoucherMinOrderNotMetException.java            (NEW) — 400
  ├── VoucherNotApplicableException.java             (NEW) — 400 (course không nằm trong scope)
  ├── VoucherAccessDeniedException.java              (NEW) — 403
  ├── VoucherUseDeniedException.java                 (NEW) — 403
  ├── VoucherUsageLimitTooLowException.java          (NEW) — 400 (admin update)
  ├── VoucherDateRangeInvalidException.java          (NEW) — 400
  ├── VoucherPercentOutOfRangeException.java         (NEW) — 400
  ├── VoucherScopeMismatchException.java             (NEW) — 400
  └── AlreadyEnrolledException.java                  (REUSE/NEW) — 400, dùng chung cho purchase
```

### 5C. Domain Service (Pure, không phụ thuộc Spring)

```
domain/service/
  ├── PricingEngine.java                 (NEW) — pure function compute(originalPrice, voucher) → PriceQuote
  └── VoucherValidator.java              (NEW) — validate(voucher, user, course, now, usedCount, perUserCount)
                                                 ném đúng 1 trong các *Exception ở trên theo thứ tự cố định
```

**Quan trọng:** `PricingEngine` và `VoucherValidator` là pure Java class — không Spring annotation, không JPA, dễ test bằng property-based testing (xem Correctness Properties trong requirements.md).

### 5D. Application Layer — Repository Interface

```
application/repository/Voucher/
  ├── VoucherRepository.java             (NEW)
  └── VoucherUsageRepository.java        (NEW)
```

**`VoucherRepository`:**
```java
Optional<Voucher> findByCode(String normalizedCode);     // code đã uppercase
Optional<Voucher> findById(Long id);
Optional<Voucher> findByIdForUpdate(Long id);            // pessimistic write lock
Voucher save(Voucher voucher);
PageResult<Voucher> findAll(int page, int size);
void softDelete(Long id);                                // set status = INACTIVE
boolean existsByCode(String normalizedCode);
```

**`VoucherUsageRepository`:**
```java
VoucherUsage save(VoucherUsage usage);
long countByVoucherId(Long voucherId);
long countByVoucherIdAndUserId(Long voucherId, Long userId);
PageResult<VoucherUsage> findByVoucherId(Long voucherId, int page, int size);
PageResult<VoucherUsage> findByUserId(Long userId, int page, int size);
boolean existsByVoucherIdAndEnrollmentId(Long voucherId, Long enrollmentId);
```

### 5E. Application Layer — DTOs

```
application/dto/Voucher/
  ├── CreateVoucherInput.java            (NEW) — record
  ├── UpdateVoucherInput.java            (NEW) — record
  ├── DeleteVoucherInput.java            (NEW) — record
  ├── GetVouchersInput.java              (NEW) — record(page, size, requesterRole)
  ├── GetVouchersOutput.java             (NEW) — PageResult<VoucherOutput>
  ├── GetVoucherDetailInput.java         (NEW) — record
  ├── VoucherOutput.java                 (NEW) — record(id, code, type, value, status, effectiveStatus,
  │                                                     validFrom, validTo, scope, minOrderAmount,
  │                                                     maxDiscount, usageLimit, usagePerUser,
  │                                                     applicableCourseIds, usedCount, createdAt, updatedAt)
  ├── QuotePricingInput.java             (NEW) — record(courseId, voucherCode, requesterId, requesterRole, isInternal)
  ├── QuotePricingOutput.java            (NEW) — record(originalPrice, discountAmount, finalPrice,
  │                                                     voucherApplied, voucherCode, voucherType,
  │                                                     internalDiscount, quotedAt)
  ├── ApplyVoucherCheckoutInput.java     (NEW) — record(courseId, voucherCode, requesterId, requesterRole, isInternal)
  ├── ApplyVoucherCheckoutOutput.java    (NEW) — record(enrollmentId, originalPrice, discountAmount,
  │                                                     finalPrice, paidPrice, voucherApplied, voucherCode)
  ├── GetVoucherUsagesInput.java         (NEW) — record(voucherId, page, size)
  ├── GetVoucherUsagesOutput.java        (NEW) — PageResult<VoucherUsageOutput>
  └── VoucherUsageOutput.java            (NEW) — record(id, voucherId, userId, courseId, enrollmentId,
                                                        originalPrice, discountAmount, finalPrice, appliedAt)
```

### 5F. Application Layer — UseCases

```
application/usecase/Voucher/
  ├── CreateVoucherUseCase.java          (NEW)
  ├── UpdateVoucherUseCase.java          (NEW)
  ├── DeleteVoucherUseCase.java          (NEW) — soft-delete
  ├── GetVouchersUseCase.java            (NEW) — danh sách + phân trang
  ├── GetVoucherDetailUseCase.java       (NEW) — kèm usedCount + effectiveStatus
  ├── GetVoucherUsagesUseCase.java       (NEW) — lịch sử dùng voucher
  ├── QuotePricingUseCase.java           (NEW) — @Transactional(readOnly = true)
  └── ApplyVoucherCheckoutUseCase.java   (NEW) — @Transactional, thay thế PurchaseCourseUseCase cũ
```

> **Quyết định**: `ApplyVoucherCheckoutUseCase` thay thế hoàn toàn `PurchaseCourseUseCase` hiện có (xử lý cả case không voucher khi `voucherCode = null`). `PurchaseCourseUseCase` cũ sẽ bị xóa để tránh có 2 entry point khác nhau cho việc mua khóa học.

### 5G. Adapter Layer — JPA Entity

```
adapter/repository/jpa/VoucherEntity/
  ├── VoucherJpaEntity.java              (NEW) — @Entity, có toDomain() + fromDomain()
  ├── VoucherUsageJpaEntity.java         (NEW) — @Entity, có UNIQUE (voucher_id, enrollment_id)
  └── VoucherCourseJpaEntity.java        (NEW) — @Entity, mapping cho SPECIFIC_COURSES (composite PK)
```

### 5H. Adapter Layer — Repository Impl

```
adapter/repository/
  ├── JpaVoucherRepository.java          (NEW) — Spring Data interface, có @Lock(PESSIMISTIC_WRITE)
  ├── JpaVoucherUsageRepository.java     (NEW)
  ├── VoucherRepositoryImpl.java         (NEW) — implements application.repository.Voucher.VoucherRepository
  └── VoucherUsageRepositoryImpl.java    (NEW) — implements VoucherUsageRepository
```

### 5I. Adapter Layer — Request/Response DTOs

```
adapter/dto/request/Voucher/
  ├── CreateVoucherRequest.java          (NEW) — Bean validation đầy đủ
  ├── UpdateVoucherRequest.java          (NEW)
  ├── QuotePricingRequest.java           (NEW) — CHỈ field voucherCode (nullable, regex ^[A-Za-z0-9_-]+$, 0–32 ký tự)
  └── PurchaseCourseRequest.java         (NEW) — CHỈ field voucherCode (giống QuotePricingRequest)

adapter/dto/response/
  ├── VoucherResponse.java               (NEW) — đầy đủ field cho admin
  ├── VoucherListResponse.java           (NEW)
  ├── VoucherUsageResponse.java          (NEW)
  ├── QuotePricingResponse.java          (NEW)
  └── PurchaseCourseResponse.java        (NEW hoặc REPLACE response cũ)
```

### 5J. Adapter Layer — Controller

```
adapter/controller/
  ├── Admin/AdminVoucherController.java          (NEW) — /api/v1/admin/vouchers/**
  └── Course/                                     
      ├── CourseQuoteController.java             (NEW) — /api/v1/courses/{courseId}/quote
      └── CourseController.java                  (UPDATE) — endpoint /purchase nay hỗ trợ voucherCode
```

### 5K. Infrastructure — Exception & Audit

**`ErrorCode` enum — thêm các mã:**
```
VOUCHER_NOT_FOUND, VOUCHER_INACTIVE, VOUCHER_NOT_YET_ACTIVE, VOUCHER_EXPIRED,
VOUCHER_USAGE_LIMIT_REACHED, VOUCHER_USAGE_PER_USER_EXCEEDED, VOUCHER_MIN_ORDER_NOT_MET,
VOUCHER_NOT_APPLICABLE, VOUCHER_ACCESS_DENIED, VOUCHER_USE_DENIED,
VOUCHER_USAGE_LIMIT_TOO_LOW, VOUCHER_DATE_RANGE_INVALID, VOUCHER_PERCENT_OUT_OF_RANGE,
VOUCHER_SCOPE_MISMATCH, ALREADY_ENROLLED
```

**`GlobalExceptionHandler` — thêm 14+ handler tương ứng.**

**`PurchaseLedgerService` (mở rộng):**
- Thêm event `VOUCHER_APPLIED` — log đầy đủ `voucherCode`, `originalPrice`, `discountAmount`, `finalPrice`.
- Thêm event `VOUCHER_REJECTED` — log lý do từ chối.

---

## 6. Luồng xử lý chi tiết

### 6A. POST /api/v1/courses/{courseId}/quote (Preview giá)

```
Request → CourseQuoteController
  → parse JWT → lấy requesterId, requesterRole, isInternal
  → QuotePricingRequest.toInput(courseId, JWT info)  // bỏ qua mọi field giá nếu client gửi
  → QuotePricingUseCase.execute(input)  // @Transactional(readOnly = true)
    → if (isInternal):
        return PriceQuote{ originalPrice = course.price, discountAmount = course.price,
                           finalPrice = 0, internalDiscount = true }
    → CourseRepository.findById(courseId)  // ném CourseNotFoundException nếu không có
    → if (voucherCode == null || blank):
        return PriceQuote{ originalPrice, 0, originalPrice, voucherApplied = false }
    → normalizedCode = voucherCode.trim().toUpperCase()
    → VoucherRepository.findByCode(normalizedCode)  // ném VoucherNotFoundException nếu không có
    → usedCount = VoucherUsageRepository.countByVoucherId(voucherId)
    → perUserCount = VoucherUsageRepository.countByVoucherIdAndUserId(voucherId, requesterId)
    → VoucherValidator.validate(voucher, user, course, now, usedCount, perUserCount)
    → PricingEngine.compute(course.price, voucher) → PriceQuote
    → return PriceQuote (không ghi DB)
  → 200 OK, body QuotePricingResponse + quotedAt = now (UTC)
```

### 6B. POST /api/v1/courses/{courseId}/purchase (Checkout)

```
Request → CourseController.purchase(courseId, body)
  → parse JWT
  → PurchaseCourseRequest.toInput(courseId, JWT info)  // bỏ qua mọi field giá
  → ApplyVoucherCheckoutUseCase.execute(input)  // @Transactional
    [1] User lock     → UserRepository.findByIdForUpdate(requesterId)  // PESSIMISTIC_WRITE
    [2] Course load   → CourseRepository.findById(courseId)            // ném 404 nếu không có
    [3] Đã enroll?    → EnrollmentRepository.existsByUserIdAndCourseId → AlreadyEnrolledException
    [4] Internal user → if (user.isInternal):
                          paidPrice = 0, voucher bị bỏ qua
                          → tạo Enrollment, ghi audit, KHÔNG tạo VoucherUsage
                          → return early
    [5] No voucher    → if (voucherCode null/blank):
                          paidPrice = course.price
                          → kiểm tra balance, trừ tiền, tạo Enrollment, audit log → return
    [6] Voucher path  → normalizedCode = voucherCode.trim().toUpperCase()
                       → Voucher voucher = VoucherRepository.findByIdForUpdate(...)  // PESSIMISTIC_WRITE
                       → usedCount   = VoucherUsageRepository.countByVoucherId(...)
                       → perUserCnt  = VoucherUsageRepository.countByVoucherIdAndUserId(...)
                       → VoucherValidator.validate(...)  // double-check sau khi lock
                       → PriceQuote pq = PricingEngine.compute(course.price, voucher)
                       → if (user.balance < pq.finalPrice) → InsufficientBalanceException
                       → user.deductBalance(pq.finalPrice)  // BigDecimal
                       → UserRepository.save(user)
                       → enrollment = EnrollmentRepository.save(new Enrollment(userId, courseId, paidPrice = pq.finalPrice))
                       → VoucherUsageRepository.save(new VoucherUsage(voucherId, userId, courseId,
                                                                       enrollmentId, originalPrice,
                                                                       discountAmount, finalPrice, now))
                       → PurchaseLedgerService.append({event: "VOUCHER_APPLIED", ...})
                       → return ApplyVoucherCheckoutOutput
  → 200 OK
```

### 6C. POST /api/v1/admin/vouchers (Tạo voucher)

```
Request → AdminVoucherController
  → @PreAuthorize("hasAuthority('MANAGE_VOUCHER')")
  → CreateVoucherRequest validation (Bean Validation)
  → CreateVoucherUseCase.execute(input)
    → if (validFrom > validTo) → VoucherDateRangeInvalidException
    → if (type == PERCENT && (value <= 0 || value > 100)) → VoucherPercentOutOfRangeException
    → if (type == FIXED && value > config.fixed.max-value) → IllegalArgumentException
    → if (scope == SPECIFIC_COURSES):
        if (applicableCourseIds empty) → VoucherScopeMismatchException
        verify mọi courseId tồn tại
    → if (scope == ALL_COURSES && applicableCourseIds not empty) → VoucherScopeMismatchException
    → normalizedCode = code.trim().toUpperCase()
    → if (VoucherRepository.existsByCode(normalizedCode)) → DuplicateVoucherCodeException
    → Voucher.create(...) → VoucherRepository.save(...)
  → 201 Created, body VoucherResponse
```

### 6D. PUT /api/v1/admin/vouchers/{id} (Cập nhật voucher) — Req. 2.9, 2.10

```
→ load voucher + usedCount
→ if (usedCount > 0):
    chỉ cho phép sửa: validTo, status, usageLimit (chỉ tăng), applicableCourseIds, minOrderAmount, maxDiscount
    block: code, type, value
→ if (usageLimit < usedCount) → VoucherUsageLimitTooLowException
→ save
```

---

## 7. Thứ tự implement (12 bước)

1. **Domain models** — `Voucher`, `VoucherType`, `VoucherStatus`, `VoucherScope`, `VoucherUsage`, `PriceQuote`
2. **Domain exceptions** — 14+ exception classes
3. **Domain services** — `PricingEngine`, `VoucherValidator` (pure Java, có thể viết test ngay)
4. **Application repository interfaces** — `VoucherRepository`, `VoucherUsageRepository`
5. **Application DTOs** — toàn bộ Input/Output records
6. **Application UseCases** — Voucher CRUD trước (`CreateVoucherUseCase`, ...), sau đó `QuotePricingUseCase` và `ApplyVoucherCheckoutUseCase`
7. **Adapter JPA entities** — `VoucherJpaEntity`, `VoucherUsageJpaEntity`, `VoucherCourseJpaEntity` (+ liquibase/jpa schema gen)
8. **Adapter Spring Data interfaces** — `JpaVoucherRepository` (có `@Lock(PESSIMISTIC_WRITE)`), `JpaVoucherUsageRepository`
9. **Adapter repository impls** — `VoucherRepositoryImpl`, `VoucherUsageRepositoryImpl`
10. **Adapter request/response DTOs + controllers** — `AdminVoucherController`, `CourseQuoteController`, update `CourseController.purchase`
11. **Infrastructure** — `ErrorCode` mới, `GlobalExceptionHandler` mới, mở rộng `PurchaseLedgerService` (event `VOUCHER_APPLIED`, `VOUCHER_REJECTED`)
12. **DataInitializer** — seed `MANAGE_VOUCHER`, `USE_VOUCHER` permissions + gán role; cập nhật `docs/permission-matrix.md`, `docs/api-docs.md`

---

## 8. Quyết định thiết kế cần xác nhận

- [ ] **Refactor `PurchaseCourseUseCase` hiện có**: Xóa hẳn (gộp vào `ApplyVoucherCheckoutUseCase`) hay giữ làm fallback "no voucher path"? Khuyến nghị **xóa**, gộp 1 use case duy nhất với `voucherCode` tùy chọn để tránh 2 entry point.
- [ ] **`SecurityConfig` — `@PreAuthorize` vs check trong UseCase**: Dùng `@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")` ở controller hay check role trong use case như Section/Lesson? Khuyến nghị `@PreAuthorize` cho admin endpoints để chặn sớm; use case vẫn là phòng tuyến cuối.
- [ ] **`USE_VOUCHER` permission check**: Người dùng không có `USE_VOUCHER` mà gửi `voucherCode` → reject (HTTP 403) hay silently bỏ qua voucher? Khuyến nghị **reject 403** với mã `VOUCHER_USE_DENIED` để rõ ràng (Req. 10.5).
- [ ] **`maxDiscount = 0` semantics**: `0` nghĩa là không giới hạn (như `usageLimit = 0`) hay là giới hạn = 0 (vô hiệu giảm giá)? Đề xuất **0 = không giới hạn** cho consistent với `usageLimit`/`usagePerUser`.
- [ ] **Course chưa published**: Nếu áp dụng feature ẩn/hiện khóa học (xem `plan-course-approval.md`), Quote và Checkout có cần check `course.published`? Khuyến nghị: Quote KHÔNG cho phép preview course chưa publish, Checkout cũng từ chối → ném `CourseNotPublishedException`.
- [ ] **Currency**: Dùng `BigDecimal` đơn vị VND như hiện có? Có cần `currency` field cho voucher không? Đề xuất **không**, dùng đơn vị mặc định của hệ thống (VND).
- [ ] **Time zone**: `validFrom` / `validTo` lưu UTC hay local? Đề xuất `LocalDateTime` UTC (consistent với `applied_at`).

---

## 9. Ghi chú kỹ thuật

- **Pure Domain Services**: `PricingEngine` và `VoucherValidator` không được phụ thuộc Spring/JPA. Inject vào use case bằng `new` hoặc qua `@Bean` ở config — KHÔNG dùng `@Service` ở domain layer.
- **BigDecimal scale**: Thống nhất scale = 2, rounding = HALF_UP cho mọi phép tính tiền (cấu hình ở `PricingEngine`).
- **Pessimistic Lock**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên `JpaVoucherRepository.findByIdForUpdate(Long id)` và đã có sẵn trên `User`. Bảo đảm thứ tự lock User → Voucher để tránh deadlock.
- **Audit log JSONL**: Mở rộng `PurchaseLedgerService` thay vì viết service mới. Field `event` phân biệt `PURCHASE_COMPLETED` / `VOUCHER_APPLIED` / `VOUCHER_REJECTED`.
- **Consistency với Quote**: `QuotePricingUseCase` và `ApplyVoucherCheckoutUseCase` chia sẻ cùng `PricingEngine` + `VoucherValidator` instance để đảm bảo Property #12 (Quote-Checkout price parity).
- **Reuse existing patterns**: Follow chính xác layer convention của Section/Lesson Management — DTO record, UseCase 1 method, exception ở domain, mapping ở repository impl.
- **Property-based testing** (cho phase code): Pricing engine có 8 property + 3 metamorphic test (xem requirements.md mục Correctness Properties). Race condition test với 2–3 thread thay vì PBT.
- **Course Approval cross-feature**: Khi triển khai chung với `plan-course-approval.md`, thêm bước check `course.published` trước khi quote/checkout. Ném `CourseNotPublishedException` (HTTP 400 hoặc 404).

---

## 10. Files mới (tổng cộng ~50 files)

```
src/main/java/com/example/learning_system_spring/
├── domain/
│   ├── model/Voucher/
│   │   ├── Voucher.java                          (NEW)
│   │   ├── VoucherType.java                      (NEW)
│   │   ├── VoucherStatus.java                    (NEW)
│   │   ├── VoucherScope.java                     (NEW)
│   │   ├── VoucherUsage.java                     (NEW)
│   │   └── PriceQuote.java                       (NEW)
│   ├── exception/
│   │   ├── VoucherNotFoundException.java         (NEW)
│   │   ├── VoucherInactiveException.java         (NEW)
│   │   ├── VoucherNotYetActiveException.java     (NEW)
│   │   ├── VoucherExpiredException.java          (NEW)
│   │   ├── VoucherUsageLimitReachedException.java(NEW)
│   │   ├── VoucherUsagePerUserExceededException.java(NEW)
│   │   ├── VoucherMinOrderNotMetException.java   (NEW)
│   │   ├── VoucherNotApplicableException.java    (NEW)
│   │   ├── VoucherAccessDeniedException.java     (NEW)
│   │   ├── VoucherUseDeniedException.java        (NEW)
│   │   ├── VoucherUsageLimitTooLowException.java (NEW)
│   │   ├── VoucherDateRangeInvalidException.java (NEW)
│   │   ├── VoucherPercentOutOfRangeException.java(NEW)
│   │   ├── VoucherScopeMismatchException.java    (NEW)
│   │   └── AlreadyEnrolledException.java         (NEW)
│   └── service/
│       ├── PricingEngine.java                    (NEW)
│       └── VoucherValidator.java                 (NEW)
├── application/
│   ├── dto/Voucher/
│   │   └── ... (~14 record files)
│   ├── repository/Voucher/
│   │   ├── VoucherRepository.java                (NEW)
│   │   └── VoucherUsageRepository.java           (NEW)
│   └── usecase/Voucher/
│       └── ... (8 use case files)
├── adapter/
│   ├── repository/jpa/VoucherEntity/
│   │   ├── VoucherJpaEntity.java                 (NEW)
│   │   ├── VoucherUsageJpaEntity.java            (NEW)
│   │   └── VoucherCourseJpaEntity.java           (NEW)
│   ├── repository/
│   │   ├── JpaVoucherRepository.java             (NEW)
│   │   ├── JpaVoucherUsageRepository.java        (NEW)
│   │   ├── VoucherRepositoryImpl.java            (NEW)
│   │   └── VoucherUsageRepositoryImpl.java       (NEW)
│   ├── dto/request/Voucher/
│   │   └── ... (4 request DTOs)
│   ├── dto/response/
│   │   └── ... (5 response DTOs)
│   └── controller/
│       ├── Admin/AdminVoucherController.java     (NEW)
│       └── Course/CourseQuoteController.java     (NEW)
│       └── Course/CourseController.java          (UPDATE: hỗ trợ voucherCode trong /purchase)
└── infrastructure/
    ├── exception/
    │   ├── ErrorCode.java                        (UPDATE — thêm ~15 mã)
    │   └── GlobalExceptionHandler.java           (UPDATE — thêm ~14 handler)
    ├── service/
    │   └── PurchaseLedgerService.java            (UPDATE — event VOUCHER_APPLIED/REJECTED)
    └── config/
        └── DataInitializer.java                  (UPDATE — seed 2 permission, gán role)

docs/
├── plan-voucher-pricing.md                       (THIS FILE)
├── permission-matrix.md                          (UPDATE)
└── api-docs.md                                   (UPDATE — thêm section Voucher)
```
