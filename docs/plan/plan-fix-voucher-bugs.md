# Kế hoạch fix các bug từ Test Bug Report

> **Tham chiếu**: `docs/test-bug-report.md` (214 tests phơi bày 3 bug thật + 3 spec mismatch + 2 cải thiện phụ).
>
> **Nguyên tắc**: Mỗi fix có (a) mô tả thay đổi, (b) file ảnh hưởng, (c) test sẽ thay đổi/thêm, (d) rủi ro & rollback. Không fix nếu chưa được duyệt.

---

## Tổng quan độ ưu tiên

| Mã | Mô tả ngắn | Loại | Risk | Effort | Ưu tiên |
|----|-----------|------|------|--------|---------|
| FIX-A | `Voucher` chấp nhận `status`/`scope` null khi update → state corrupt | Bug | Medium | S | **P0 — fix ngay** |
| FIX-C | `CourseOwnershipPolicy.hasFullAccess(null)` ném NPE | Bug | High | XS | **P0 — fix ngay** |
| FIX-D | PricingEngine precision-loss → `voucherApplied=true` nhưng `discount=0` | Bug | Medium | XS | **P1 — fix sau khi xác nhận business** |
| DEC-1 | Update voucher có cho sửa `code/type/value` khi chưa có usage không? | Spec | — | S–M | **P2 — cần team quyết** |
| DEC-2 | FIXED voucher max-value cap (mặc định 100M) — có cần enforce? | Spec | — | XS | **P2 — cần team quyết** |
| DEC-3 | Quote cho non-MEMBER không kèm voucher → có chặn 403? | Spec | — | XS | **P2 — cần team quyết** |
| OPT-1 | Voucher load 2 query trong checkout — gộp thành 1 | Perf | Low | XS | P3 — nice-to-have |
| OPT-2 | Bỏ `userRepository.save(user)` dư trong nhánh internal member | Cleanup | Low | XS | P3 — nice-to-have |

**Quy ước effort**: XS = <30 phút | S = 0.5 ngày | M = 1 ngày.

---

## P0 — FIX-A: Voucher null status/scope guard

### Vấn đề

`Voucher.updateSoftFields()` cho phép `newStatus = null` và `newScope = null` (chỉ check khi `scope == SPECIFIC_COURSES`). Voucher rơi vào "limbo state":

- `isInactive()` trả `false` (so sánh với `INACTIVE` không match null) → bypass validator
- `appliesTo(courseId)` trả `false` cho mọi course

### Thay đổi

**1. `domain/model/Voucher/Voucher.java`** — `updateSoftFields`:

```java
public void updateSoftFields(VoucherStatus newStatus, ...) {
    if (newStatus == null)
        throw new IllegalArgumentException("status không được null");
    if (newScope == null)
        throw new IllegalArgumentException("scope không được null");
    // ... existing checks
}
```

**2. `domain/service/VoucherValidator.java`** — defense-in-depth:

```java
// Thay isInactive() bằng kiểm tra strict ACTIVE
if (voucher.getStatus() != VoucherStatus.ACTIVE) {
    throw new VoucherInactiveException(voucher.getCode());
}
```

### File ảnh hưởng

- `domain/model/Voucher/Voucher.java` (thêm 2 null-check)
- `domain/service/VoucherValidator.java` (đổi check status)

### Test thay đổi

Các test hiện đang **phơi bày** bug sẽ FAIL sau fix → cần đảo lại assertion:

- `VoucherTest$UpdateSoftFields.allowsNullStatus` → đổi sang `assertThatThrownBy(...).isInstanceOf(IllegalArgumentException.class)`
- `VoucherTest$UpdateSoftFields.allowsNullScope` → tương tự
- `VoucherValidatorTest$StatusCheck.nullStatusBypassesInactiveCheck` → đổi sang assert ném `VoucherInactiveException`
- `UpdateVoucherUseCaseTest$StatusNullBug.statusNullAccepted` → đổi sang assert ném `IllegalArgumentException` ở use case

Thêm test mới:
- Voucher có `status = EXPIRED` (nếu enum mở rộng sau này) hoặc bất kỳ giá trị nào khác `ACTIVE` → ném `VoucherInactiveException`.

### Rủi ro & Rollback

- **Rủi ro**: nếu có data hiện tại trong DB với `status = NULL` (do bug Update_Voucher trước đây), mọi voucher đó sẽ ném `VoucherInactiveException` — đúng intent.
- **Migration**: chạy 1 query `UPDATE vouchers SET status = 'INACTIVE' WHERE status IS NULL` trước khi deploy fix.
- **Rollback**: revert 2 file. Không thay đổi schema → không cần rollback DB.

---

## P0 — FIX-C: Null-guard cho CourseOwnershipPolicy

### Vấn đề

`hasFullAccess(role)`, `hasFullCourseAccess(role)`, `isInstructorOwner(course, id, role)`, `isAdmin(role)` đều gọi `role.isStaff()` không null-check → NPE khi role null.

`canViewUnpublished` đã có null-guard → behavior không nhất quán.

### Thay đổi

**`domain/service/CourseOwnershipPolicy.java`** — thêm null-guard cho 4 method:

```java
public static boolean hasFullAccess(Role role) {
    if (role == null) return false;
    return role.isStaff() || role.isSuperAdmin();
}

public static boolean hasFullCourseAccess(Role role) {
    if (role == null) return false;
    return role.isStaff() || role.isAdminUser() || role.isSuperAdmin();
}

public static boolean isInstructorOwner(Course course, Long requesterId, Role role) {
    if (role == null) return false;
    return role.isInstructor() && isOwner(course, requesterId);
}

public static boolean isAdmin(Role role) {
    return hasFullCourseAccess(role);  // delegate, đã có guard
}
```

### File ảnh hưởng

- `domain/service/CourseOwnershipPolicy.java` (chỉ 1 file)

### Test thay đổi

- `CourseOwnershipPolicyTest$HasFullAccess.nullRoleNpe` → đổi sang `assertThat(hasFullAccess(null)).isFalse()`
- `UpdateVoucherUseCaseTest$Authorization.nullRoleNpe` → đổi sang assert ném `CourseAccessDeniedException` (use case sẽ chuyển null role thành 403)
- `CreateAndDeleteVoucherUseCaseTest$CreateAuthorization.nullRoleNpe` → tương tự

Thêm test mới:
- `hasFullCourseAccess(null) == false`
- `isInstructorOwner(course, id, null) == false`

### Rủi ro & Rollback

- **Rủi ro**: cực thấp — chỉ thêm 4 null-guard line. Không đổi behavior khi role không null.
- **Rollback**: revert 1 file.

---

## P1 — FIX-D: Precision-loss trong PricingEngine

### Vấn đề

Voucher PERCENT giá trị nhỏ × originalPrice nhỏ → sau `HALF_UP` scale 2, `discountAmount = 0` nhưng `voucherApplied = true`. UI hiển thị "đã áp voucher" mà giá không giảm; user mất 1 lượt dùng (nếu có usagePerUser limit) mà không được giảm.

Ví dụ: PERCENT 0.001% × 100đ → 0.001 → HALF_UP scale 2 = 0.00.

### Thay đổi

**`domain/service/PricingEngine.java`** — fold về `noDiscount` khi discount = 0:

```java
BigDecimal discount = computeDiscount(normalizedOriginal, voucher);
if (discount.compareTo(normalizedOriginal) > 0) {
    discount = normalizedOriginal;
}
// MỚI: nếu discount = 0 sau làm tròn, coi như không áp voucher
if (discount.signum() == 0) {
    return PriceQuote.noDiscount(normalizedOriginal);
}
return PriceQuote.withVoucher(normalizedOriginal, discount,
        voucher.getCode(), voucher.getType());
```

### Quyết định business cần xác nhận trước khi fix

Khi user gửi voucherCode hợp lệ nhưng do precision không giảm được, hành vi mong muốn là:

- **Option A** (đề xuất): Coi như không áp voucher → `voucherApplied = false`, **không tạo `VoucherUsage`** ở checkout (không tiêu thụ lượt dùng).
- **Option B**: Coi như đã áp → `voucherApplied = true`, tạo `VoucherUsage` với discount = 0 → tiêu thụ lượt dùng (như hiện tại). User confused.
- **Option C**: Reject với lỗi mới `VOUCHER_DISCOUNT_TOO_SMALL` → từ chối hẳn checkout.

→ **Option A** là an toàn và ít gây bất ngờ nhất.

### File ảnh hưởng

- `domain/service/PricingEngine.java` (thêm 3 line)

### Test thay đổi

- `PricingEngineTest$PercentVoucher.precisionLoss_voucherAppliedButZeroDiscount` → đổi assertion thành `voucherApplied = false` và `voucherCode = null`
- `ApplyVoucherCheckoutUseCaseTest`: thêm test "voucher PERCENT discount 0 do precision → không tạo VoucherUsage, paid = original".

### Rủi ro & Rollback

- **Rủi ro**: nếu business mong Option B (giữ behavior cũ), fix này phá business expectation → cần xác nhận trước.
- **Tương tác**: thay đổi này ảnh hưởng cả luồng `/quote` và `/purchase` (cùng dùng `PricingEngine`) — đảm bảo Property #12 (Quote-Checkout parity) vẫn hold.
- **Rollback**: revert 1 file.

---

## P2 — DEC-1: Update voucher có cho sửa code/type/value khi chưa có usage?

### Tình huống

Spec voucher-pricing/requirements.md mục 2.9: "WHILE voucher có ít nhất 1 Voucher_Usage, từ chối thay đổi code/type/value".

Impl: `UpdateVoucherInput` không có 3 field này → never editable. `VoucherImmutableFieldException` được tạo nhưng không có chỗ ném.

### 2 lựa chọn

**Lựa chọn A — Implement đúng spec** (cho admin sửa khi chưa có usage):

1. Thêm `code`, `type`, `value` vào `UpdateVoucherInput` (nullable — null = giữ nguyên).
2. Thêm field tương ứng vào `UpdateVoucherRequest` DTO.
3. `UpdateVoucherUseCase`:
   - Đếm `usedCount`.
   - Nếu input có gửi `code`/`type`/`value` mới mà `usedCount > 0` → ném `VoucherImmutableFieldException`.
   - Nếu `usedCount == 0` và có gửi → cho phép sửa.
4. Sửa `Voucher` thêm method `updateImmutableFields(code, type, value)` chỉ gọi khi cần.
5. Thêm exception handler cho `VoucherImmutableFieldException` (đã có ErrorCode `VOUCHER_IMMUTABLE_FIELD`).

**Lựa chọn B — Hợp thức hóa hành vi hiện tại** (never editable):

1. Update spec voucher-pricing 2.9 ghi rõ "code/type/value never editable; admin phải xóa và tạo lại".
2. Xóa `VoucherImmutableFieldException` và `VOUCHER_IMMUTABLE_FIELD` khỏi `ErrorCode`.

### Đề xuất

**Lựa chọn B** đơn giản hơn, an toàn hơn. Lựa chọn A thêm complexity và rủi ro nếu admin sửa nhầm code khi DB đã có cache.

### Cần team quyết

→ Câu hỏi cho team:
- Use case thực tế: admin có thường sửa typo voucher code không, hay sẽ tạo lại voucher mới?
- Có UI nào hiện đang thử gọi update với 3 field này không?

---

## P2 — DEC-2: Enforce FIXED voucher max-value?

### Tình huống

Spec mục 1.4: "FIXED voucher value ≤ `voucher.fixed.max-value` (mặc định 100,000,000)".
Impl: không enforce. Admin tạo được voucher FIXED 999,999,999.

### Đề xuất

**Implement nếu chấp nhận thêm 1 config**:

1. `application.yml`: thêm `voucher.fixed.max-value: 100000000`.
2. `CreateVoucherUseCase`: inject `@Value` và check.
3. Thêm test cap khi value > max → ném `IllegalArgumentException`.

### File ảnh hưởng

- `src/main/resources/application.yml` (1 dòng)
- `application/usecase/Voucher/CreateVoucherUseCase.java` (thêm 1 check)
- Test: thay `fixedHugeValueAllowed` thành `fixedHugeValueRejected`

### Cần team quyết

→ Có muốn enforce config này không? Nếu có, mặc định 100M có hợp lý cho VND không?

---

## P2 — DEC-3: Quote cho non-MEMBER không kèm voucher có chặn 403?

### Tình huống

Lệch giữa 2 file spec:
- `voucher-pricing/requirements.md`: không yêu cầu chặn role nếu không có voucher.
- `voucher-management/requirements.md` mục 10.6: yêu cầu chặn 403 cho mọi non-MEMBER.

Impl: theo voucher-pricing — chỉ chặn khi gửi voucherCode.

### Đề xuất

Endpoint `/quote` thực chất là "preview giá khóa học có/không voucher". Cho admin xem cũng hợp lý (tester scenario, support team).

→ **Giữ nguyên impl** (theo voucher-pricing). Update voucher-management mục 10.6 cho khớp.

### Cần team quyết

→ Quote endpoint phục vụ ai? Chỉ MEMBER (mua) hay cả admin (preview)?

---

## P3 — OPT-1: Gộp 2 query voucher trong checkout

### Tình huống

`ApplyVoucherCheckoutUseCase` gọi:
1. `voucherRepository.findByCode(normalizedCode)` — lấy id
2. `voucherRepository.findByIdForUpdate(id)` — lấy lại với pessimistic lock

→ 2 query thay vì 1.

### Thay đổi

**1. `JpaVoucherRepository.java`** — thêm method:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT v FROM VoucherJpaEntity v WHERE v.code = :code")
Optional<VoucherJpaEntity> findByCodeForUpdate(@Param("code") String code);
```

**2. `VoucherRepository.java`** (interface) — thêm `Optional<Voucher> findByCodeForUpdate(String code)`.

**3. `VoucherRepositoryImpl.java`** — implement.

**4. `ApplyVoucherCheckoutUseCase.java`** — đổi sang gọi `findByCodeForUpdate` trực tiếp.

### File ảnh hưởng

- 4 file (interface + impl + jpa + use case).

### Test

- Test hiện tại vẫn pass (chỉ refactor performance).
- Thêm 1 integration test (nếu có embedded DB) để verify lock thật sự được giữ.

### Rủi ro

Thấp — chỉ tối ưu. Verify `findByCode` vẫn được giữ cho `/quote` (read-only).

---

## P3 — OPT-2: Bỏ `userRepository.save(user)` dư trong internal member

### Tình huống

`ApplyVoucherCheckoutUseCase.processInternal` gọi `userRepository.save(user)` mặc dù internal member không thay đổi balance.

JPA dirty checking sẽ no-op nhưng code dư.

### Thay đổi

```java
private PurchaseCourseOutput processInternal(...) {
    course.enroll();
    // BỎ: userRepository.save(user);
    courseRepository.save(course);
    // ...
}
```

### File ảnh hưởng

- `application/usecase/Voucher/ApplyVoucherCheckoutUseCase.java` (xóa 1 line)

### Test

Không thay đổi — test pass đều.

---

## Lộ trình triển khai đề xuất

### Sprint hiện tại (urgent fixes)

1. **FIX-C** (5 phút) — null-guard CourseOwnershipPolicy.
2. **FIX-A** (30 phút) — null-validate Voucher.updateSoftFields + strict ACTIVE check trong VoucherValidator.
3. Migration script: `UPDATE vouchers SET status = 'INACTIVE' WHERE status IS NULL` (defensive, không có data thật vẫn nên chạy).
4. Update lại các test "bug-exposing" → đảo assertion (5 test cần sửa).
5. Run lại full test suite — kỳ vọng 214 pass.

### Sprint sau (cần business quyết)

6. **DEC-1, DEC-2, DEC-3** — đưa vào meeting team product, lock decision.
7. **FIX-D** — chỉ fix sau khi business chốt Option A/B/C.

### Backlog (nice-to-have)

8. **OPT-1** — gộp query voucher (đợi profiling thật).
9. **OPT-2** — cleanup `userRepository.save` dư.

---

## Metric thành công

Sau khi áp các fix P0:

| Metric | Hiện tại | Sau fix |
|--------|----------|---------|
| Số test "bug-exposing" | 8 | 0 (tất cả assertion đảo lại thành expected behavior) |
| Voucher state corrupted có thể tạo | Có (status null) | Không (validation chặn) |
| HTTP 500 từ NPE role null | Có | Không (trả 403 đúng) |
| Tổng test pass | 214 | 214 (không đổi số lượng, chỉ đảo assertion) |

---

## Câu hỏi cần xác nhận trước khi implement

1. **FIX-D Option**: chọn A (fold về no-discount), B (giữ nguyên), hay C (reject)?
2. **DEC-1**: Cho admin sửa code/type/value khi chưa có usage hay never editable?
3. **DEC-2**: Có enforce FIXED max-value config không? Mặc định 100M có hợp lý?
4. **DEC-3**: Quote endpoint chỉ MEMBER hay cho cả non-MEMBER xem giá?
5. **Migration timing**: chạy `UPDATE vouchers SET status = 'INACTIVE' WHERE status IS NULL` ngay khi deploy FIX-A, hay đợi maintenance window?

→ Khi nhận được câu trả lời, tôi sẽ thực thi P0 trước, sau đó P1, sau đó là P2 theo thứ tự.
