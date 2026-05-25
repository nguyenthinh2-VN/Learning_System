# Decision Log — Learning System

Tài liệu này ghi lại các quyết định quan trọng cần AI agent sau này biết.

---

## 2026-05-24 — Voucher Pricing Bug Fixes

### DECISION: Fix priority order

| Priority | Fix | Status |
|----------|-----|--------|
| P0 | FIX-A: Voucher null status/scope guard | Chờ user quyết migration timing |
| P0 | FIX-C: CourseOwnershipPolicy null-guard | Chờ user quyết |
| P1 | FIX-D: Precision-loss voucherApplied=true mà discount=0 | Chờ user chọn Option A/B/C |
| P2 | DEC-1: Update voucher có cho sửa code/type/value? | Chờ team product |
| P2 | DEC-2: Enforce FIXED max-value? | Chờ team product |
| P2 | DEC-3: Quote chặn non-MEMBER? | Chờ team product |
| P3 | OPT-1: Gộp 2 query voucher | Backlog |
| P3 | OPT-2: Bỏ save user dư | Backlog |

### DECISION: Migration script cho FIX-A

```sql
UPDATE vouchers SET status = 'INACTIVE' WHERE status IS NULL;
```

Chạy trước khi deploy FIX-A.

### DECISION: 5 câu hỏi cần xác nhận trước khi fix

1. FIX-D Option: A (fold về no-discount), B (giữ nguyên), C (reject)?
2. DEC-1: Cho sửa code/type/value khi chưa có usage hay never editable?
3. DEC-2: Enforce FIXED max-value 100M VND không?
4. DEC-3: Quote endpoint chỉ MEMBER hay cho non-MEMBER preview?
5. Migration timing: chạy ngay deploy hay đợi maintenance window?

→ Khi có câu trả lời, thực thi theo thứ tự P0 → P1 → P2.

---

## 2026-05-24 — Test Bug Report Summary

### 214 tests / 0 failures

**Bug thật (cần fix):**
- Bug-A: Voucher.updateSoftFields chấp nhận status/scope null → state corrupt.
- Bug-C: CourseOwnershipPolicy.hasFullAccess(null) NPE → 500 thay vì 403.
- Bug-D: PricingEngine precision-loss → voucherApplied=true mà discount=0.

**Spec mismatch (cần team quyết):**
- DEC-1: UpdateVoucher không cho sửa code/type/value cả khi chưa có usage (stricter hơn spec).
- DEC-2: FIXED voucher max-value không được enforce.
- DEC-3: Quote cho non-MEMBER không kèm voucher: 2 spec lệch nhau.

**Performance observation:**
- OPT-1: Voucher load 2 query trong checkout.
- OPT-2: userRepository.save(user) dư trong nhánh internal member.

Chi tiết đầy đủ: `docs/test-bug-report.md`.

---

## 2026-05-24 — Documentation Map

Khi onboard AI agent mới, đọc theo thứ tự:

1. `docs/project-overview.md` — kiến trúc tổng thể, 8 feature đã hoàn thiện, quy tắc khi code tiếp.
2. `docs/permission-matrix.md` — 5 roles × 19 permissions.
3. `docs/api-docs.md` — reference endpoint.
4. `docs/plan-*.md` — kế hoạch từng feature.
5. `.kiro/specs/<feature>/requirements.md` — requirements EARS.

---

## 2026-05-24 — Decision: Entry point purchase duy nhất

`ApplyVoucherCheckoutUseCase` là entry point DUY NHẤT cho luồng purchase (gộp cả có/không voucher).

`PurchaseCourseUseCase` cũ đã bị xóa.

---

## 2026-05-24 — Decision: Pure domain service đăng ký bean

`PricingEngine`, `VoucherValidator`, `CourseOwnershipPolicy` là class thuần Java, KHÔNG dùng `@Service`/`@Component`.

Đăng ký bean qua `infrastructure/config/DomainServiceConfig.java` (`@Bean`).

---

## 2026-05-24 — Decision: Anti-tampering rules

1. Server LUÔN đọc giá từ DB theo `courseId` ở path.
2. DTO request chỉ khai báo `voucherCode`, KHÔNG khai báo field giá.
3. Mỗi `/purchase` tính lại giá độc lập, không có "quote token".
4. Pessimistic lock thứ tự: User → Course → Voucher.

---

## 2026-05-24 — Decision: Internal Member luôn 0đ

Internal Member (`isInternal = true`) luôn `paidPrice = 0`, voucher bị bỏ qua, không tạo `VoucherUsage`.

---

## 2026-05-24 — Decision: Audit log JSONL

Mọi event tài chính ghi qua `PurchaseLedgerService` vào `logs/purchase_ledger.jsonl`:
- `PURCHASE_COMPLETED`
- `VOUCHER_APPLIED`
- `VOUCHER_REJECTED`

---

## 2026-05-24 — Decision: Course Approval Workflow

- Course mới mặc định `published = false`, `priceLocked = false`.
- STAFF/SUPER_ADMIN duyệt qua `POST /api/v1/admin/courses/{id}/publish`.
- Khi publish, `priceLocked = true`, INSTRUCTOR không sửa được giá.

---

## 2026-05-24 — Decision: Permission Matrix

5 roles × 19 permissions. 4 permission mới:
- `PUBLISH_COURSE` (STAFF, SUPER_ADMIN)
- `LOCK_COURSE_PRICE` (STAFF, SUPER_ADMIN)
- `MANAGE_VOUCHER` (STAFF, SUPER_ADMIN)
- `USE_VOUCHER` (MEMBER, SUPER_ADMIN)

---

## 2026-05-24 — Decision: Test framework

- JUnit Jupiter 6.0.3 + Mockito 5.20.0 + AssertJ 3.27.7 (transitive qua spring-boot-starter-data-jpa-test).
- Pom.xml KHÔNG có spring-boot-starter-test riêng.

---

## 2026-05-24 — Decision: Bug-A root cause

`Voucher.updateSoftFields` không validate `status`/`scope` null → voucher có thể rơi vào "limbo state".

Fix: thêm null-check ở `Voucher.updateSoftFields` + strict ACTIVE check ở `VoucherValidator`.

---

## 2026-05-24 — Decision: Bug-C root cause

`CourseOwnershipPolicy.hasFullAccess(null)` ném NPE thay vì return false.

Fix: null-guard cho 4 method (hasFullAccess, hasFullCourseAccess, isInstructorOwner, isAdmin).

---

## 2026-05-24 — Decision: Bug-D root cause

PricingEngine PERCENT precision-loss → discount = 0 sau HALF_UP scale 2 nhưng `voucherApplied = true`.

Fix: fold về `noDiscount` khi `discountAmount.signum() == 0`.

---

## 2026-05-24 — Decision: OPT-1 — Performance

Voucher load 2 query trong checkout (findByCode + findByIdForUpdate) → gộp thành 1 query findByCodeForUpdate với @Lock.

---

## 2026-05-24 — Decision: OPT-2 — Cleanup

Bỏ `userRepository.save(user)` dư trong nhánh internal member của ApplyVoucherCheckoutUseCase.
