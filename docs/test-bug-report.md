# Test Bug Report — Vai Tester Adversarial

Tài liệu này tổng hợp các bug và observation phát hiện được khi viết test với góc nhìn tester adversarial (không phải dev viết test để xác nhận code chạy đúng). Mục tiêu: phơi bày kẽ hở, không che dấu.

**Kết quả:** 214 tests / 0 failures / 0 errors. Tất cả test PASS — nhưng nhiều test "pass" lại chính là test phơi bày bug: chúng xác nhận behavior hiện tại đang lệch spec hoặc chấp nhận state corrupted mà code không từ chối.

---

## 1. Bug-A — Voucher state corrupted (status null / scope null)

### Mô tả

`Voucher.updateSoftFields()` KHÔNG validate `status` và `scope` được phép null hay không. Khi gọi (qua `UpdateVoucherUseCase`), nếu input có `status = null` hoặc `scope = null`, voucher rơi vào "limbo state":

- `voucher.isInactive()` trả `false` (vì so sánh với `INACTIVE` không match null)
- `voucher.isActive()` trả `false`
- `voucher.appliesTo(courseId)` trả `false` cho mọi courseId (rớt qua hết case)

### Hậu quả

- Voucher null status sẽ **PASS qua VoucherValidator** ở bước check status (hàm chỉ ném `VoucherInactiveException` khi `status == INACTIVE`).
- Voucher null scope sẽ ném `VoucherNotApplicableException` khi quote/checkout — đúng exception nhưng message không phản ánh state corrupted.
- Adapter level có `@NotNull VoucherStatus status` ở DTO nên Bean Validation chặn được. Nhưng nếu use case bị gọi trực tiếp (test nội bộ, hoặc bypass Bean Validation), bug vẫn tồn tại.

### Test phơi bày

- `VoucherTest$UpdateSoftFields.allowsNullStatus`
- `VoucherTest$UpdateSoftFields.allowsNullScope`
- `VoucherTest$AppliesTo.nullScopeFallsThrough`
- `VoucherValidatorTest$StatusCheck.nullStatusBypassesInactiveCheck`
- `UpdateVoucherUseCaseTest$StatusNullBug.statusNullAccepted`

### Khuyến nghị fix

Thêm null-check ở `Voucher.updateSoftFields`:

```java
if (newStatus == null) throw new IllegalArgumentException("status không được null");
if (newScope == null) throw new IllegalArgumentException("scope không được null");
```

Thêm null-check trong `VoucherValidator` (defense-in-depth):

```java
if (voucher.getStatus() != VoucherStatus.ACTIVE) {
    throw new VoucherInactiveException(voucher.getCode());
}
```

---

## 2. Bug-C — `CourseOwnershipPolicy.hasFullAccess(null)` ném NullPointerException

### Mô tả

`CourseOwnershipPolicy.hasFullAccess(Role role)` không null-check. Mọi voucher use case (`CreateVoucherUseCase`, `UpdateVoucherUseCase`, `DeleteVoucherUseCase`, `GetVouchersUseCase`) gọi `hasFullAccess(input.requesterRole())`.

Nếu `requesterRole = null` (token bị tampered, JWT lỗi parse role, hoặc test gọi với null), code ném `NullPointerException` thay vì trả về `false` để tiếp tục thành lỗi 403.

### Hậu quả

- `GlobalExceptionHandler` catch `Exception` → trả HTTP 500 `INTERNAL_ERROR` thay vì 403 `ACCESS_DENIED`.
- Lộ thông tin lỗi nội bộ ra client.
- Hành vi không nhất quán với `canViewUnpublished` (method này CÓ null-guard).

### Test phơi bày

- `CourseOwnershipPolicyTest$HasFullAccess.nullRoleNpe`
- `UpdateVoucherUseCaseTest$Authorization.nullRoleNpe`
- `CreateAndDeleteVoucherUseCaseTest$CreateAuthorization.nullRoleNpe`

### Khuyến nghị fix

Thêm null-guard cho cả 3 method `hasFullAccess`, `hasFullCourseAccess`, `isInstructorOwner`, `isAdmin`:

```java
public static boolean hasFullAccess(Role role) {
    if (role == null) return false;
    return role.isStaff() || role.isSuperAdmin();
}
```

---

## 3. Bug-D — Precision loss: voucherApplied = true nhưng discount = 0

### Mô tả

Với voucher PERCENT giá trị nhỏ + originalPrice nhỏ, sau khi `HALF_UP` về scale 2, `discountAmount` có thể bằng 0 nhưng `voucherApplied` vẫn = `true`.

Ví dụ: voucher PERCENT 0.001% trên 100đ → rawDiscount = 0.001đ → HALF_UP scale 2 = 0.00đ. Final = 100đ, voucherApplied = true.

### Hậu quả

- UI hiển thị "Đã áp voucher" nhưng giá không giảm — gây confusion cho user.
- `VoucherUsage` được tạo với `discountAmount = 0`, vẫn tiêu thụ 1 lượt voucher của user. User bị mất "lượt dùng" nhưng không nhận giảm giá thật.

### Test phơi bày

- `PricingEngineTest$PercentVoucher.precisionLoss_voucherAppliedButZeroDiscount`

### Khuyến nghị fix

Nếu `discountAmount.signum() == 0`, treat như "no voucher applied":

```java
if (discount.signum() == 0) {
    return PriceQuote.noDiscount(normalizedOriginal);
}
return PriceQuote.withVoucher(normalizedOriginal, discount, voucher.getCode(), voucher.getType());
```

---

## 4. Spec mismatch — UpdateVoucher không cho sửa code/type/value cả khi chưa có usage

### Mô tả

Spec `voucher-pricing/requirements.md` mục 2.9: "WHILE voucher có ít nhất một bản ghi Voucher_Usage, THE Update_Voucher_UseCase SHALL từ chối thay đổi các trường code, type, và value".

Impl thực tế: `UpdateVoucherInput` không khai báo 3 field này → admin **KHÔNG bao giờ sửa được** kể cả khi voucher chưa có usage.

### Đánh giá

- An toàn hơn spec (stricter).
- Nhưng nếu admin muốn sửa typo trong code (vd `WELOMECE50` → `WELCOME50`) trước khi voucher được dùng lần nào, không thể.
- Exception class `VoucherImmutableFieldException` đã được tạo ra nhưng **không có chỗ ném** trong code.

### Test phơi bày

- `UpdateVoucherUseCaseTest$ImmutableFieldsObservation.code_type_value_neverChangeable`

### Khuyến nghị

Hoặc:
- (a) Implement đúng spec: thêm `code/type/value` vào `UpdateVoucherInput`, chỉ chặn khi `usedCount > 0`. Dùng `VoucherImmutableFieldException` đã có.
- (b) Update spec ghi rõ "never editable" và xóa `VoucherImmutableFieldException` không dùng.

---

## 5. Spec mismatch — FIXED voucher max-value không được enforce

### Mô tả

Spec mục 1.4: "WHEN tạo voucher loại FIXED, value phải ≤ giá trị tối đa cho phép của hệ thống (cấu hình `voucher.fixed.max-value`, mặc định 100,000,000)".

Impl: `CreateVoucherUseCase` chỉ check PERCENT > 100. KHÔNG có check FIXED max value.

### Hậu quả

- Admin có thể tạo voucher FIXED với value 999,999,999đ. Khi áp dụng, `min(value, originalPrice)` cap về originalPrice → final = 0. Không nguy hiểm vì invariant Pricing engine chặn final < 0.
- Nhưng có thể nhầm lẫn data trên admin dashboard (voucher `value = 999,999,999` hiển thị méo UI).

### Test phơi bày

- `CreateAndDeleteVoucherUseCaseTest$ValueValidation.fixedHugeValueAllowed`

### Khuyến nghị fix

Thêm check trong `CreateVoucherUseCase` với cấu hình:

```java
@Value("${voucher.fixed.max-value:100000000}")
private BigDecimal fixedMaxValue;

if (input.type() == VoucherType.FIXED && input.value().compareTo(fixedMaxValue) > 0) {
    throw new IllegalArgumentException("Voucher FIXED value vượt quá max-value: " + fixedMaxValue);
}
```

---

## 6. Spec mismatch — Quote cho non-MEMBER không có voucher: pass thay vì 403

### Mô tả

Spec voucher-management mục 10.6: "Price_Preview_Controller SHALL được mount riêng cho voucher preview. Nếu user không phải MEMBER, controller SHALL trả lỗi 403".

Impl: `QuotePricingUseCase` CHỈ chặn khi có `voucherCode`. Nếu STAFF/INSTRUCTOR/ADMIN_USER gọi `/quote` không kèm voucher, request VẪN PASS và trả giá gốc.

### Đánh giá

- Spec voucher-pricing/requirements.md không yêu cầu chặn — chỉ voucher-management mới có. Hai spec lệch nhau.
- Behavior hiện tại chấp nhận được nếu coi `/quote` là endpoint preview giá tổng quát.

### Test phơi bày

- `QuotePricingUseCaseTest$RolePermission.staffNoVoucherAllowed`

### Khuyến nghị

Quyết định team: chọn 1 trong 2 spec làm chuẩn rồi đồng bộ impl + tài liệu.

---

## 7. Performance observation — voucher load 2 lần

### Mô tả

`ApplyVoucherCheckoutUseCase` gọi:
1. `voucherRepository.findByCode(normalizedCode)` để lấy voucher
2. `voucherRepository.findByIdForUpdate(voucher.getId())` để giữ pessimistic lock

→ 2 query thay vì 1.

### Đánh giá

- Không phải bug correctness — chỉ là dư query.
- Ý đồ: tránh impl JPA `findByCodeForUpdate` (cần custom query với `@Lock`).

### Khuyến nghị

Thêm method `findByCodeForUpdate` vào `JpaVoucherRepository` để giảm còn 1 query:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT v FROM VoucherJpaEntity v WHERE v.code = :code")
Optional<VoucherJpaEntity> findByCodeForUpdate(@Param("code") String code);
```

---

## 8. Defensive code observation — Internal Member vẫn save user dù không thay đổi

### Mô tả

`ApplyVoucherCheckoutUseCase.processInternal` gọi `userRepository.save(user)` mặc dù internal member không có biến đổi balance.

### Đánh giá

- Không phải bug. JPA dirty checking sẽ no-op nếu không có thay đổi.
- Code hơi dư.

### Khuyến nghị

Bỏ dòng `userRepository.save(user)` trong nhánh internal member.

---

## Tổng kết

| Loại | Số lượng |
|------|----------|
| Test phơi bày bug thật (cần fix) | 8 |
| Test phơi bày spec mismatch | 4 |
| Test confirm correctness | 202 |
| Tổng | 214 |

**3 bug ưu tiên fix:**
1. Bug-A (status/scope null) — defensive validation trong `Voucher.updateSoftFields` và `VoucherValidator`.
2. Bug-C (NPE khi role null) — null-guard trong `CourseOwnershipPolicy`.
3. Bug-D (precision loss) — fold về `noDiscount` khi `discountAmount = 0`.

**3 spec mismatch để team quyết:**
4. UpdateVoucher có cho sửa code/type/value khi chưa có usage không?
5. FIXED max-value config có nên enforce không?
6. Quote cho non-MEMBER không kèm voucher có chặn 403 không?

**Cách chạy lại test:**

```bash
.\mvnw.cmd test
```

Chỉ chạy các test domain + voucher use case (nhanh, không cần MySQL):

```bash
.\mvnw.cmd test "-Dtest=PricingEngineTest+VoucherValidatorTest+VoucherTest+CourseTest+CourseOwnershipPolicyTest+UserBalanceTest+QuotePricingUseCaseTest+ApplyVoucherCheckoutUseCaseTest+UpdateVoucherUseCaseTest+CreateAndDeleteVoucherUseCaseTest"
```
