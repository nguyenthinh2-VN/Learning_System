# Implementation Plan

## Overview

Bugfix theo phương pháp Bug Condition (C(X) → P(result)). Tham chiếu đầy đủ điều kiện lỗi tại `bugfix.md` và phương án kỹ thuật tại `design.md`.

**Phạm vi test:** Property-Based Test (PBT) neo vào **backend Bug 1** (`AdminTopUpUseCase`) vì đây là nơi có test framework (JUnit) và đặc tả mang tính phổ quát ("∀ username/email tồn tại → cộng đúng ví"). Theo Testing Strategy đã duyệt trong `design.md`, các lỗi **frontend** (Bug 2, 3, 4, Yêu cầu B-FE) được kiểm chứng bằng `npm run build` + `npm run lint` + kiểm thử thủ công — FE hiện **không** có test framework và việc dựng mới nằm ngoài phạm vi bugfix này.

**Lệnh build:** Backend dùng `mvnw` (Windows) ở thư mục gốc `e:\Learning_System`. Frontend chạy trong `e:\Learning_System\fe`. **KHÔNG** chạy dev server dài hạn — chỉ chạy build/lint một lần.

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "description": "Viết PBT trên code CHƯA sửa (exploration + preservation)",
      "tasks": ["1", "2"]
    },
    {
      "wave": 2,
      "description": "BUG 1 backend + frontend; verify Property 1 & 2",
      "tasks": ["3"]
    },
    {
      "wave": 3,
      "description": "Avatar backend + các fix frontend thuần (độc lập, song song)",
      "tasks": ["4", "5", "6", "7"]
    },
    {
      "wave": 4,
      "description": "Yêu cầu B frontend (phụ thuộc task 4 trả avatarUrl)",
      "tasks": ["8"]
    },
    {
      "wave": 5,
      "description": "Verification (build/lint/manual) rồi checkpoint",
      "tasks": ["9", "10"]
    }
  ],
  "dependencies": {
    "3": ["1", "2"],
    "8": ["4"],
    "9": ["3", "4", "5", "6", "7", "8"],
    "10": ["9"]
  }
}
```

Sơ đồ phụ thuộc (trực quan):

```
[1] Property 1: Bug Condition (BE, exploration) ─┐
                                                  ├─► [3] BUG 1 fix (BE+FE) ─► [3.4] verify P1 / [3.5] verify P2
[2] Property 2: Preservation (BE, baseline) ─────┘

[4] Avatar (BE) ─────► [8] Yêu cầu B (FE: AuthContext avatarUrl + AppSidebar)

[5] BUG 2 (FE preview)  ┐
[6] BUG 3 (FE sidebar)  ├─ (FE thuần, song song) ─► [9] Verification ─► [10] Checkpoint
[7] BUG 4 (FE balance)  ┘
```

Tóm tắt phụ thuộc:
- **[1], [2]** (PBT) phải viết **TRƯỚC** [3] (chạy trên code chưa sửa).
- **[3.4], [3.5]** chạy **SAU** khi [3.1]–[3.3] hoàn tất.
- **[8]** (FE avatar/account) phụ thuộc **[4]** (BE trả `avatarUrl`).
- **[5], [6], [7]** là FE thuần độc lập, có thể tiến hành song song.
- **[9]** chạy sau khi tất cả task triển khai xong; **[10]** là checkpoint cuối.

## Tasks

- [ ] 1. Viết exploration test cho Bug Condition (Bug 1 — phân giải username/email)
  - **Property 1: Bug Condition** - Phân giải định danh khi cộng tiền (username/email)
  - **QUAN TRỌNG**: Viết test property-based này **TRƯỚC** khi triển khai fix.
  - **MỤC TIÊU**: Phơi bày counterexample chứng minh lỗi tồn tại — hiện không có cách cộng tiền theo username/email ở tầng use case (`AdminTopUpUseCase` chỉ có `execute(Long, ...)`).
  - **Scoped PBT Approach**: Vì lỗi mang tính tất định, thu hẹp property về các case cụ thể để tái hiện ổn định:
    - identifier = username tồn tại (vd `MEM2B4A1D`) → kỳ vọng phân giải đúng user và cộng đúng ví
    - identifier = email tồn tại (vd `user@example.com`) → kỳ vọng phân giải đúng user
    - identifier = chuỗi không tồn tại → kỳ vọng ném `UserNotFoundException` (sẽ map 404 `USER_NOT_FOUND`)
  - Test ở `src/test/java/.../application/usecase/Wallet/AdminTopUpUseCaseTest.java` gọi `adminTopUpUseCase.execute(String identifier, amount, note)` (overload mới theo `design.md`).
  - Mock `UserRepository.findByUsernameOrEmail(key, key)` theo từng case.
  - Encode đặc tả từ Bug Condition: `isUsernameOrEmail(identifier) AND NOT isNumericId(identifier)` (xem `bugfix.md` §"BUG 1").
  - Assertion phải khớp Expected Behavior: `result.resolvedUser = lookupByUsernameOrEmail(identifier)` và cộng đúng ví.
  - Chạy test trên **code CHƯA sửa** bằng: `mvnw -q test -Dtest=AdminTopUpUseCaseTest`
  - **KẾT QUẢ KỲ VỌNG**: Test **FAIL** (đúng — chứng minh lỗi: overload `execute(String, ...)` chưa tồn tại / không phân giải được theo định danh).
  - **KHÔNG** sửa test hay code khi nó fail ở bước này.
  - Ghi lại counterexample (vd: "không có API cộng tiền theo `MEM2B4A1D`; chỉ chấp nhận User ID số").
  - Đánh dấu hoàn thành khi test đã viết, đã chạy và đã ghi nhận thất bại.
  - _Requirements: 2.1_

- [ ] 2. Viết preservation property test (Bug 1 — bảo toàn hành vi cộng tiền theo ID) — TRƯỚC khi fix
  - **Property 2: Preservation** - Cộng tiền theo User ID + ràng buộc SUPER_ADMIN + audit + WebSocket
  - **QUAN TRỌNG**: Theo phương pháp observation-first — quan sát hành vi trên **code CHƯA sửa** rồi viết test chốt hành vi đó.
  - Observe trên code chưa sửa qua overload hiện có `execute(Long userId, amount, note)`:
    - Cộng tiền cho `userId` hợp lệ → cộng đúng ví, trả `newBalance` = balance cũ + amount
    - Ghi đúng **1** audit record cho mỗi lần cộng thành công
    - Đẩy đúng **1** sự kiện `WALLET_UPDATED` realtime tới user đó
    - Giữ pessimistic lock theo id
  - Viết property test (scope tới tập `amount` dương đa dạng, vd nhiều giá trị `BigDecimal > 0`): ∀ amount hợp lệ → `newBalance = oldBalance + amount` và đúng 1 audit + 1 event.
  - (Khuyến nghị: dùng JUnit parameterized cho nhiều `amount`; nếu dự án bổ sung jqwik thì có thể generate ngẫu nhiên — không bắt buộc.)
  - Chạy trên **code CHƯA sửa**: `mvnw -q test -Dtest=AdminTopUpUseCaseTest`
  - **KẾT QUẢ KỲ VỌNG**: Test **PASS** (xác nhận baseline cần bảo toàn).
  - Đánh dấu hoàn thành khi test đã viết, đã chạy và PASS trên code chưa sửa.
  - _Requirements: 3.1_

- [ ] 3. BUG 1 — Cộng tiền theo username/email (Backend + Frontend)

  - [ ] 3.1 Backend: thêm khả năng phân giải identifier ở tầng application/adapter
    - `domain/exception/UserNotFoundException.java`: thêm constructor `(String identifier)` nếu chưa có (giữ constructor cũ); `GlobalExceptionHandler` đã map → 404 `USER_NOT_FOUND` (không đổi).
    - `application/usecase/Wallet/AdminTopUpUseCase.java`: thêm overload `execute(String identifier, BigDecimal amount, String note)` — validate identifier không rỗng, `userRepository.findByUsernameOrEmail(key, key)` → nếu trống ném `UserNotFoundException(key)`, sau đó **tái dùng** `execute(resolved.getId(), amount, note)` để giữ nguyên luồng lock + audit. **GIỮ NGUYÊN** overload `execute(Long, ...)`.
    - `adapter/dto/request/Wallet/AdminTopUpByIdentifierRequest.java` (mới): record `{ @NotBlank identifier, @NotNull @Positive amount, @Size(max=255) note }`.
    - `adapter/controller/AdminUserController.java`: thêm endpoint mới `POST /admin/users/top-up` (`@PreAuthorize("hasRole('SUPER_ADMIN')")`), gọi overload identifier rồi `walletNotificationService.pushWalletUpdated(...)`. **GIỮ** endpoint `/{userId}/top-up` cũ.
    - _Bug_Condition: isBugCondition_1(X) = isUsernameOrEmail(X.identifier) AND NOT isNumericId(X.identifier) (bugfix.md)_
    - _Expected_Behavior: result.resolvedUser = lookupByUsernameOrEmail(identifier) AND creditedCorrectWallet = TRUE (design Property 1)_
    - _Preservation: chỉ SUPER_ADMIN; 1 audit + 1 WALLET_UPDATED; pessimistic lock theo id (design Property 2)_
    - _Requirements: 2.1, 3.1_

  - [ ] 3.2 Backend: bổ sung/cập nhật unit test cho `AdminTopUpUseCase`
    - Hoàn thiện các case từ task 1: resolve theo username, resolve theo email, not-found → `UserNotFoundException`.
    - Giữ nguyên/kế thừa test cho overload theo id (đảm bảo không hồi quy).
    - _Requirements: 2.1, 3.1_

  - [ ] 3.3 Frontend: ô nhập identifier ở form cộng tiền
    - `api/adminApi.js`: thêm `adminTopUpByIdentifierApi = (data) => adminApi.post('/admin/users/top-up', data)`.
    - `pages/admin/AdminTopUpPage.jsx`: đổi ô `userId` (`type="number"`) → ô `identifier` (`type="text"`), label "Username hoặc Email *", placeholder `MEM2B4A1D hoặc user@example.com`; validate không rỗng; submit gọi `adminTopUpByIdentifierApi`; xử lý 404 → hiển thị "Không tìm thấy người dùng"; **bỏ import `Wallet` thừa** (đang gây lint error).
    - _Bug_Condition: form chỉ nhận User ID số (Input type="number") (bugfix.md 1.1)_
    - _Expected_Behavior: ô text nhận username HOẶC email, báo lỗi rõ nếu không tìm thấy (2.1)_
    - _Requirements: 2.1_

  - [ ] 3.4 Verify exploration test (Bug Condition) giờ đã PASS
    - **Property 1: Expected Behavior** - Phân giải định danh khi cộng tiền (username/email)
    - **QUAN TRỌNG**: Chạy lại **CHÍNH** test từ task 1 — KHÔNG viết test mới.
    - Chạy: `mvnw -q test -Dtest=AdminTopUpUseCaseTest`
    - **KẾT QUẢ KỲ VỌNG**: Test **PASS** (xác nhận lỗi đã được sửa, hành vi mong đợi thỏa mãn).
    - _Requirements: 2.1_

  - [ ] 3.5 Verify preservation test vẫn PASS (không hồi quy)
    - **Property 2: Preservation** - Cộng tiền theo User ID + SUPER_ADMIN + audit + WebSocket
    - **QUAN TRỌNG**: Chạy lại **CHÍNH** test từ task 2 — KHÔNG viết test mới.
    - Chạy: `mvnw -q test -Dtest=AdminTopUpUseCaseTest`
    - **KẾT QUẢ KỲ VỌNG**: Test **PASS** (không hồi quy hành vi cộng tiền theo id, quyền, audit, WebSocket).
    - _Requirements: 3.1_

- [ ] 4. Yêu cầu B (Backend) — thêm field `avatarUrl`
  - SQL migration: `ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL;` (đặt file trong `src/main/resources/sql/` hoặc `docs/sql/` theo quy ước dự án).
  - `adapter/repository/jpa/UserEntity/UserJpaEntity.java`: thêm `@Column(name="avatar_url", length=500) private String avatarUrl;`; `toDomain()` truyền `avatarUrl` vào overload `User.reconstitute(...)` mới; `fromDomain(User)` set `e.avatarUrl = user.getAvatarUrl()`.
  - `domain/model/User.java`: thêm field `avatarUrl` + getter; thêm **overload mới** `reconstitute(..., String avatarUrl, createdAt, updatedAt)` gọi lại overload cũ rồi gán `avatarUrl`. **GIỮ NGUYÊN** overload `reconstitute` cũ (không có `avatarUrl`) để không phá ~10 callers/test hiện có.
  - `adapter/controller/UserController.java` (`getMyProfile`): thêm `avatarUrl` (có thể null) vào map `data`; **đổi sang `java.util.HashMap`** vì `Map.of(...)` không cho phép value null.
  - _Bug_Condition: profile chưa có avatarUrl; FE không có nguồn ảnh đại diện (design Yêu cầu B)_
  - _Expected_Behavior: GET /users/me/profile trả thêm avatarUrl (nullable); overload reconstitute mới không phá test cũ (design Property 6)_
  - _Preservation: các API hiện có vẫn hoạt động khi avatarUrl = null; overload reconstitute cũ giữ nguyên_
  - _Requirements: 3.5_

- [ ] 5. BUG 2 (Frontend) — Preview khóa chưa duyệt bằng adminToken
  - `pages/admin/AdminCoursePreviewPage.jsx` (mới): lấy dữ liệu qua `adminGetCourseDetailApi(id)` + `adminGetSectionsApi(id)` (đều dùng `adminToken`); banner "Chế độ xem trước (chưa xuất bản)" + nút "Duyệt xuất bản" (`publishCourseApi`); **KHÔNG** render nút mua/đăng ký public.
  - Router admin: thêm route `/admin/courses/:id/preview` → `AdminCoursePreviewPage`.
  - `pages/admin/AdminPendingCoursesPage.jsx`: đổi nút "Xem trước" từ `window.open('/courses/${id}')` → `navigate('/admin/courses/${id}/preview')` (thêm `useNavigate`).
  - _Bug_Condition: isBugCondition_2(X) = course.published=FALSE AND viewerCanPreview AND viewerContext=PUBLIC (bugfix.md)_
  - _Expected_Behavior: hiển thị nội dung chưa publish bằng quyền admin, không phải trang đăng ký public (design Property 3)_
  - _Preservation: /courses/{id} của khóa published=true giữ nguyên hành vi public (3.2)_
  - _Requirements: 2.2, 3.2_

- [ ] 6. BUG 3 (Frontend) — Sidebar admin tô sáng most-specific
  - `components/layout/AdminSidebar.jsx`: thay logic `isActive` dùng `startsWith` bằng most-specific match: gom `allUrls` từ `NAV_GROUPS`, `matchedUrl(path)` = url khớp (`path === url || path.startsWith(url + '/')`) có độ dài lớn nhất; `isActive(url) = matchedUrl(location.pathname) === url`.
  - Đảm bảo: `/admin`→Dashboard; `/admin/courses`→Khóa học; `/admin/courses/pending`→Chờ duyệt; `/admin/courses/:id/preview`→Khóa học. **KHÔNG** sửa phần account của AdminSidebar.
  - _Bug_Condition: isBugCondition_3(X) = COUNT(item WHERE path STARTS_WITH item.url) > 1 (bugfix.md)_
  - _Expected_Behavior: COUNT(activeItems)=1 AND activeItems[0]=mostSpecificMatch(path) (design Property 4)_
  - _Preservation: /admin và /admin/courses vẫn tô sáng đúng mục khi không có route con cụ thể hơn (3.3)_
  - _Requirements: 2.3, 3.3_

- [ ] 7. BUG 4 (Frontend) — Số dư ví đọc từ AuthContext
  - `pages/PurchaseConfirmPage.jsx`: bỏ `const { balance, deduct } = useWalletStore();`; dùng `const { balance, updateBalance, fetchProfile } = useAuth();`.
  - `insufficientBalance = balance != null && balance < finalPrice`.
  - Thêm `formatBalance(amount)` null-safe: `amount == null → '---'`, ngược lại format VND (`Intl.NumberFormat('vi-VN', {style:'currency', currency:'VND', maximumFractionDigits:0})`).
  - `useEffect`: nếu `balance == null` → gọi `fetchProfile()` khi mount.
  - Sau mua thành công: thay `deduct(...)` bằng `fetchProfile()` để đồng bộ số dư từ BE.
  - Ô "Số dư ví" dùng `formatBalance(balance)`; giữ `formatMoney` cho giá khóa (có nhánh `=== 0 → 'Miễn phí'`).
  - _Bug_Condition: isBugCondition_4(X) = page="PurchaseConfirm" AND readBalance(balanceSource)=UNDEFINED (bugfix.md)_
  - _Expected_Behavior: rendered = formatVND(actualBalance) AND không chứa "NaN" (design Property 5)_
  - _Preservation: luồng quote/voucher, kiểm tra số dư, mua thành công, đồng bộ qua fetchProfile/WebSocket giữ nguyên (3.4, 3.5)_
  - _Requirements: 2.4, 3.4, 3.5_

- [ ] 8. Yêu cầu B (Frontend) — AuthContext avatarUrl + redesign account footer ở AppSidebar
  - **Phụ thuộc task 4** (BE phải trả `avatarUrl` trong profile).
  - `context/AuthContext.jsx` (`fetchProfile`): thêm `avatarUrl: data.avatarUrl` vào `userData` (lưu localStorage + state).
  - `components/layout/AppSidebar.jsx`: redesign khối account ở footer (chỉ nhánh đã đăng nhập) — avatar `<img src={publicUser.avatarUrl}>` khi có, fallback initials khi trống; tên + email + balance; `ChevronsUpDown` (import thêm từ `lucide-react`) và **icon đăng xuất bên phải** gọi `publicLogout()` rồi điều hướng `/login`. Nhánh chưa đăng nhập ("Đăng nhập") giữ nguyên. **KHÔNG** sửa AdminSidebar account.
  - _Bug_Condition: AppSidebar không có nguồn avatarUrl, account footer chưa có avatar img + logout bên phải (design Yêu cầu B)_
  - _Expected_Behavior: hiển thị img khi avatarUrl khác rỗng, fallback initials khi null; icon logout gọi đúng publicLogout (design Property 6)_
  - _Preservation: nhánh chưa đăng nhập và điều hướng quen thuộc giữ nguyên; AppSidebar hoạt động khi avatarUrl=null_
  - _Requirements: 3.5_

- [ ] 9. Verification — build, lint và kiểm thử thủ công

  - [ ] 9.1 Backend build + test
    - Chạy ở `e:\Learning_System`: `mvnw -q -DskipTests compile` (biên dịch), sau đó `mvnw -q test -Dtest=AdminTopUpUseCaseTest` (Property 1 & 2 PASS).
    - Xác nhận overload `User.reconstitute` mới không phá test cũ (chạy thêm test liên quan User nếu có).
    - _Requirements: 2.1, 3.1, 3.5_

  - [ ] 9.2 Frontend build + lint
    - Chạy ở `e:\Learning_System\fe`: `npm run build` và `npm run lint` (một lần, không watch). Xác nhận hết lỗi import/JSX (gồm lỗi import `Wallet` thừa đã bỏ).
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.5_

  - [ ] 9.3 Kiểm thử thủ công (theo Testing Strategy của design)
    - Bug 1: SUPER_ADMIN nhập username và email → cộng OK; nhập sai → 404 "Không tìm thấy người dùng".
    - Bug 2: "Xem trước" khóa pending → thấy nội dung chưa publish, không thấy nút mua public.
    - Bug 3: vào `/admin/courses/pending` → chỉ 1 mục active; kiểm tra `/admin`, `/admin/courses`.
    - Bug 4: trang xác nhận → "Số dư ví" hiện số đúng, không "NaN"; mua xong số dư cập nhật.
    - Yêu cầu B: `GET /users/me/profile` trả `avatarUrl`; `AppSidebar` hiện avatar img (khi có) / initials (khi null) + icon logout bên phải hoạt động.
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 10. Checkpoint — đảm bảo tất cả test PASS
  - Đảm bảo Property 1 (Expected Behavior) và Property 2 (Preservation) đều PASS; backend compile sạch; frontend build/lint sạch.
  - Nếu phát sinh nghi vấn hoặc test fail không như kỳ vọng, dừng lại và hỏi người dùng.
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_

## Notes

- **PBT chỉ ở backend Bug 1**: theo Testing Strategy đã duyệt, FE không có test framework nên Bug 2/3/4 và Yêu cầu B-FE verify bằng build + lint + manual.
- **Quyết định đã chốt** (design §"Quyết định đã chốt"): (1) thêm endpoint mới `POST /admin/users/top-up` và GIỮ `/{userId}/top-up`; (2) thêm `avatarUrl` thật ở BE (cột DB + profile), FE img/initials fallback; (3) chỉ redesign `AppSidebar`, `AdminSidebar` chỉ nhận fix Bug 3.
- **Không phá hành vi đang đúng**: giữ overload `User.reconstitute` cũ và endpoint top-up theo id để tránh hồi quy.
- **An toàn lệnh**: không chạy `npm run dev`/watch; backend dùng `mvnw` trên Windows.
