# Fix Summary — 2026-05-25

Tổng kết tất cả thay đổi đã thực hiện trong session này.
Compile: ✅ 0 errors.

---

## Các fix đã hoàn thành

### FIX-A (P0) — Voucher null status/scope guard

**Vấn đề:** `Voucher.updateSoftFields()` không validate `newStatus` và `newScope` null → voucher rơi vào "limbo state": `isInactive()` trả `false` (null ≠ INACTIVE) nhưng `appliesTo()` cũng trả `false` → bypass validator.

**Files đã sửa:**

- `domain/model/Voucher/Voucher.java`
  - Thêm null-check cho `newStatus` và `newScope` ở đầu `updateSoftFields()`, ném `IllegalArgumentException`.

- `domain/service/VoucherValidator.java`
  - Đổi check status từ `voucher.isInactive()` sang strict `voucher.getStatus() != VoucherStatus.ACTIVE`.
  - Bắt được cả trường hợp `status = null` hoặc giá trị enum không mong đợi trong tương lai.

**Migration script (chạy ngay deploy):**
```sql
UPDATE vouchers SET status = 'INACTIVE' WHERE status IS NULL;
```

---

### FIX-C (P0) — CourseOwnershipPolicy null-guard

**Vấn đề:** `hasFullAccess(null)`, `hasFullCourseAccess(null)`, `isInstructorOwner(course, id, null)` gọi `role.isStaff()` không null-check → NPE → HTTP 500 thay vì 403.

**Files đã sửa:**

- `domain/service/CourseOwnershipPolicy.java`
  - Thêm `if (role == null) return false;` cho 3 method: `hasFullAccess`, `hasFullCourseAccess`, `isInstructorOwner`.
  - `isAdmin()` delegate sang `hasFullCourseAccess()` nên tự được bảo vệ.
  - `canViewUnpublished()` đã có null-guard từ trước → không đổi.

---

### FIX-D (P1) — PricingEngine precision-loss

**Vấn đề:** Voucher PERCENT giá trị nhỏ × originalPrice nhỏ → `discountAmount = 0` sau HALF_UP scale 2, nhưng `voucherApplied = true` → user mất lượt dùng voucher mà không được giảm giá.

**Quyết định:** Option A — fold về `noDiscount` khi discount = 0.

**Files đã sửa:**

- `domain/service/PricingEngine.java`
  - Thêm guard sau khi tính discount: `if (discount.signum() == 0) return PriceQuote.noDiscount(normalizedOriginal);`
  - Kết quả: `voucherApplied = false`, không tạo `VoucherUsage`, user trả giá gốc.

---

### DEC-1 (P2) — Cho sửa code/type/value khi chưa có usage

**Quyết định:** Implement theo spec — cho sửa khi `usedCount == 0`, chặn khi `usedCount > 0`.

**Files đã sửa:**

- `domain/model/Voucher/Voucher.java`
  - Thêm method `updateImmutableFields(String newCode, VoucherType newType, BigDecimal newValue)`.
  - Mỗi param nullable — null = giữ nguyên. Validate code không rỗng, value > 0.

- `application/dto/Voucher/UpdateVoucherInput.java`
  - Thêm 3 field nullable: `newCode`, `newType`, `newValue`.

- `application/usecase/Voucher/UpdateVoucherUseCase.java`
  - Thêm logic: nếu bất kỳ field immutable nào non-null → kiểm tra `usedCount`.
  - `usedCount > 0` → ném `VoucherImmutableFieldException` với tên field vi phạm.
  - `usedCount == 0` → kiểm tra code mới không trùng (nếu đổi code) → gọi `updateImmutableFields()`.

- `adapter/dto/request/Voucher/UpdateVoucherRequest.java`
  - Thêm 3 field nullable: `code` (với `@Pattern` validation), `type`, `value` (với `@Positive`).

- `adapter/controller/AdminVoucherController.java`
  - Truyền `req.getCode()`, `req.getType()`, `req.getValue()` vào `UpdateVoucherInput`.

**Behavior:**
| Tình huống | Kết quả |
|-----------|---------|
| Gửi `code`/`type`/`value` mới, chưa có usage | Cho phép sửa |
| Gửi `code`/`type`/`value` mới, đã có usage | 400 `VOUCHER_IMMUTABLE_FIELD` |
| Không gửi `code`/`type`/`value` (null) | Giữ nguyên, chỉ sửa soft fields |

---

### OPT-1 (P3) — Gộp 2 query voucher trong checkout

**Vấn đề:** `processWithVoucher` gọi `findByCode` (lấy id) rồi `findByIdForUpdate` (lấy lại với lock) → 2 round-trip DB.

**Files đã sửa:**

- `adapter/repository/JpaVoucherRepository.java`
  - Thêm `findByCodeForUpdate(@Param("code") String code)` với `@Lock(PESSIMISTIC_WRITE)`.

- `application/repository/Voucher/VoucherRepository.java`
  - Thêm `Optional<Voucher> findByCodeForUpdate(String normalizedCode)` vào interface.

- `adapter/repository/VoucherRepositoryImpl.java`
  - Implement `findByCodeForUpdate()` delegate sang `jpa.findByCodeForUpdate()`.

- `application/usecase/Voucher/ApplyVoucherCheckoutUseCase.java`
  - Đổi `findByCode + findByIdForUpdate` → `findByCodeForUpdate` trong `processWithVoucher`.

---

### OPT-2 (P3) — Xóa userRepository.save(user) dư trong nhánh internal

**Vấn đề:** `processInternal` gọi `userRepository.save(user)` dù internal member không thay đổi balance → JPA dirty checking no-op nhưng code dư gây nhầm lẫn.

**Files đã sửa:**

- `application/usecase/Voucher/ApplyVoucherCheckoutUseCase.java`
  - Xóa `userRepository.save(user)` trong `processInternal`.

---

## Quyết định không implement

| Mã | Lý do |
|----|-------|
| DEC-2 (Enforce FIXED max-value) | Admin tự chịu trách nhiệm — không cần enforce |
| DEC-3 (Chặn non-MEMBER ở /quote) | Không có nghiệp vụ ẩn giá — giữ nguyên cho all xem |

---

## Tổng hợp files đã thay đổi

| File | Loại thay đổi |
|------|--------------|
| `domain/model/Voucher/Voucher.java` | FIX-A + DEC-1 |
| `domain/service/VoucherValidator.java` | FIX-A |
| `domain/service/PricingEngine.java` | FIX-D |
| `domain/service/CourseOwnershipPolicy.java` | FIX-C |
| `application/usecase/Voucher/ApplyVoucherCheckoutUseCase.java` | OPT-1 + OPT-2 |
| `application/dto/Voucher/UpdateVoucherInput.java` | DEC-1 |
| `application/usecase/Voucher/UpdateVoucherUseCase.java` | DEC-1 |
| `application/repository/Voucher/VoucherRepository.java` | OPT-1 |
| `adapter/repository/JpaVoucherRepository.java` | OPT-1 |
| `adapter/repository/VoucherRepositoryImpl.java` | OPT-1 |
| `adapter/dto/request/Voucher/UpdateVoucherRequest.java` | DEC-1 |
| `adapter/controller/AdminVoucherController.java` | DEC-1 |

---

## Trạng thái DECISION_LOG sau session này

| Mã | Trạng thái |
|----|-----------|
| FIX-A | ✅ Done |
| FIX-C | ✅ Done |
| FIX-D | ✅ Done (Option A) |
| DEC-1 | ✅ Done (cho sửa khi chưa có usage) |
| DEC-2 | ⏭ Bỏ qua (admin tự chịu trách nhiệm) |
| DEC-3 | ⏭ Bỏ qua (giữ nguyên — cho all preview) |
| OPT-1 | ✅ Done |
| OPT-2 | ✅ Done |
| Migration | ✅ Script sẵn sàng — chạy ngay deploy |
