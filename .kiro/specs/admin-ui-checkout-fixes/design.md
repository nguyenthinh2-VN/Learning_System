# Bugfix Design Document — admin-ui-checkout-fixes

## Overview

Tài liệu thiết kế kỹ thuật cho 4 lỗi đã phân tích ở `bugfix.md`, kèm 2 yêu cầu mở rộng cụ thể từ người dùng:

- **Yêu cầu A (BE):** Thêm khả năng cộng tiền theo **username HOẶC email** vào `AdminTopUpUseCase` (thay vì bắt buộc User ID dạng số).
- **Yêu cầu B (BE + FE):** Thêm **avatar (`avatarUrl`)** ở backend — lưu DB + trả về trong API profile/login. Thiết kế lại khối **tài khoản (account) ở sidebar website học viên (`AppSidebar`)** — avatar ảnh + icon đăng xuất ở bên phải (cạnh icon `⌄⌃`). **Admin sidebar giữ nguyên, không sửa.**

Quyết định đã chốt khi review:
1. **Endpoint Bug 1:** thêm endpoint mới `POST /admin/users/top-up` và **GIỮ** endpoint `/{userId}/top-up` cũ.
2. **Avatar:** thêm field `avatarUrl` thật ở BE (cột DB + trả trong profile). FE hiển thị `<img>` khi có, fallback initials khi trống.
3. **Sidebar redesign:** chỉ sửa `AppSidebar` (web học viên), **không** sửa `AdminSidebar`.

Nguyên tắc xuyên suốt: giữ Clean Architecture của BE (domain → application → adapter → infrastructure), không phá vỡ hành vi đang đúng (regression prevention ở §3 của `bugfix.md`). Mỗi lỗi thiết kế độc lập, có thể triển khai/review riêng.

## Glossary

| Thuật ngữ | Ý nghĩa |
|-----------|---------|
| identifier | Chuỗi định danh người dùng do admin nhập: username HOẶC email |
| adminToken / publicToken | 2 JWT tách biệt: token Admin Portal và token website học viên (lưu khác key trong localStorage) |
| most-specific match | Logic chọn url nav khớp dài nhất (cụ thể nhất) với pathname hiện tại |
| AuthContext | React context giữ `balance` (nguồn sự thật số dư), cập nhật qua `fetchProfile()` và WebSocket `WALLET_UPDATED` |
| preview (admin) | Chế độ xem trước khóa học chưa publish bằng quyền admin, không phải trang public |
| avatarUrl | URL ảnh đại diện người dùng; cột DB `avatar_url` (nullable), trả trong `GET /users/me/profile` |

## Bug Details

Tham chiếu đầy đủ điều kiện lỗi `C(X)` và bằng chứng tái hiện tại `bugfix.md`. Tóm tắt 4 lỗi + 2 yêu cầu:

- **BUG 1** — Form "Cộng tiền thủ công" chỉ nhận User ID số (`Input type="number"`) → admin khó nhớ id. (`AdminTopUpPage.jsx`, `AdminUserController`, `AdminTopUpUseCase`).
- **BUG 2** — Nút "Xem trước" ở trang chờ duyệt gọi `window.open('/courses/${id}')` chạy dưới `publicToken` → khóa chưa publish không hiển thị cho admin. (`AdminPendingCoursesPage.jsx`).
- **BUG 3** — `AdminSidebar.isActive()` dùng `startsWith` → `/admin/courses/pending` active đồng thời "Khóa học" + "Chờ duyệt".
- **BUG 4** — `PurchaseConfirmPage` đọc `balance` từ `useWalletStore` (store không có `balance`) → `undefined` → "NaN đ".
- **Yêu cầu A** — cộng tiền theo username/email.
- **Yêu cầu B** — thêm `avatarUrl` ở BE (DB + API profile); redesign account footer ở **`AppSidebar` (web học viên)**: avatar ảnh + icon logout bên phải cạnh `⌄⌃`. Admin sidebar không sửa.

## Expected Behavior

- **2.1 / Yêu cầu A** — Một ô nhập text cho username HOẶC email; BE phân giải đúng user, cộng tiền, báo lỗi rõ nếu không tìm thấy. Giữ ràng buộc SUPER_ADMIN, audit log, WebSocket push.
- **2.2** — "Xem trước" hiển thị nội dung khóa chưa publish bằng quyền admin, không phải trang đăng ký public.
- **2.3** — Sidebar chỉ tô sáng đúng MỘT mục (most-specific match).
- **2.4** — Trang xác nhận hiển thị số dư ví thực tế, định dạng VND đúng, không "NaN".
- **Yêu cầu B** — BE lưu + trả `avatarUrl` trong profile. Khối account ở `AppSidebar` có avatar (img khi có `avatarUrl`, fallback initials khi trống), tên, email, icon `⌄⌃`, và icon đăng xuất bên phải.

## Hypothesized Root Cause

| Bug | File gốc | Nguyên nhân gốc (đã xác minh qua code) |
|-----|----------|-----------------------------------------|
| BUG 1 | `AdminTopUpPage.jsx`, `AdminTopUpUseCase.java`, `AdminUserController.java` | Endpoint `POST /admin/users/{userId}/top-up` chỉ nhận `userId` số ở path; FE chỉ có ô number. Repo BE **đã có** `findByUsernameOrEmail` nhưng use case chưa dùng. |
| BUG 2 | `AdminPendingCoursesPage.jsx`, `CourseDetailPage.jsx` | `window.open('/courses/${id}')` mở route public chạy bằng `publicToken`; BE `GET /courses/{id}` chỉ trả khóa chưa publish khi mang JWT admin/owner. `adminApi.adminGetCourseDetailApi` (dùng adminToken) đã có nhưng không được dùng. |
| BUG 3 | `AdminSidebar.jsx` | `isActive(url) = location.pathname.startsWith(url)` → `/admin/courses/pending` khớp cả `/admin/courses`. |
| BUG 4 | `PurchaseConfirmPage.jsx`, `useWalletStore.js` | Trang đọc `const { balance, deduct } = useWalletStore()` nhưng store này KHÔNG có `balance`/`deduct`. Số dư thật ở `AuthContext.balance`. `undefined` → `formatMoney(undefined)` → "NaN đ". |

## Correctness Properties

### Property 1: Phân giải identifier (Bug 1)
∀ identifier là username hoặc email tồn tại → cộng tiền vào đúng ví của user được phân giải; identifier không tồn tại → 404 `USER_NOT_FOUND` và không cộng tiền.

**Validates: Requirements 2.1**

### Property 2: Regression quyền & audit (Bug 1)
Chỉ SUPER_ADMIN gọi được; mỗi lần cộng tiền thành công ghi đúng 1 audit record + đẩy đúng 1 sự kiện `WALLET_UPDATED` tới user đó; vẫn giữ pessimistic lock theo id.

**Validates: Requirements 3.1**

### Property 3: Preview admin (Bug 2)
Với khóa `published = false`, preview admin hiển thị nội dung; trang public `/courses/{id}` của khóa `published = true` không đổi hành vi.

**Validates: Requirements 2.2, 3.2**

### Property 4: Active duy nhất (Bug 3)
∀ pathname → đúng 1 item active = url khớp dài nhất (most-specific); `/admin` không "nuốt" các route con.

**Validates: Requirements 2.3, 3.3**

### Property 5: Số dư hợp lệ (Bug 4)
Ô "Số dư ví" = `formatBalance(AuthContext.balance)`, không bao giờ chứa "NaN"; luồng mua + đồng bộ balance qua `fetchProfile`/WebSocket giữ nguyên.

**Validates: Requirements 2.4, 3.4, 3.5**

### Property 6: Avatar & account footer (Yêu cầu B)
`GET /users/me/profile` trả thêm `avatarUrl` (có thể null). `AppSidebar` hiển thị `<img>` khi `avatarUrl` khác rỗng, ngược lại fallback initials; icon đăng xuất bên phải gọi đúng `publicLogout`. Các API hiện có vẫn hoạt động khi `avatarUrl = null`.

**Validates: Requirements 3.5**

## Fix Implementation

### BUG 1 — Cộng tiền theo username/email

**Backend**

1. `UserRepository` (port) — không đổi, đã có `findByUsernameOrEmail(username, email)`.

2. `AdminTopUpUseCase` — thêm overload phân giải identifier, giữ overload `Long` cho tương thích:
```java
@Transactional
public AdminTopUpOutput execute(String identifier, BigDecimal amount, String note) {
    if (identifier == null || identifier.isBlank()) {
        throw new IllegalArgumentException("Vui lòng nhập username hoặc email");
    }
    String key = identifier.trim();
    User resolved = userRepository.findByUsernameOrEmail(key, key)
            .orElseThrow(() -> new UserNotFoundException(key));
    return execute(resolved.getId(), amount, note); // tái dùng luồng có lock + audit
}
```

3. `UserNotFoundException` — thêm constructor `(String identifier)` nếu chưa có. `GlobalExceptionHandler` đã map → 404 `USER_NOT_FOUND` (không đổi).

4. `AdminUserController` — thêm endpoint mới (giữ `/{userId}/top-up` cũ):
```java
@PostMapping("/top-up")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<ApiResponse<AdminTopUpOutput>> adminTopUpByIdentifier(
        @Valid @RequestBody AdminTopUpByIdentifierRequest request) {
    AdminTopUpOutput output = adminTopUpUseCase.execute(
            request.identifier(), request.amount(), request.note());
    walletNotificationService.pushWalletUpdated(
            output.username(), output.userId(), output.newBalance(),
            output.addedAmount(), "ADMIN", output.referenceCode(), output.note());
    return ResponseEntity.ok(ApiResponse.success("Cộng tiền thành công", output));
}
```

5. DTO mới `adapter/dto/request/Wallet/AdminTopUpByIdentifierRequest.java`:
```java
public record AdminTopUpByIdentifierRequest(
    @NotBlank String identifier,
    @NotNull @Positive BigDecimal amount,
    @Size(max = 255) String note
) {}
```

**Frontend**

- `adminApi.js`: thêm `adminTopUpByIdentifierApi = (data) => adminApi.post('/admin/users/top-up', data)`.
- `AdminTopUpPage.jsx`: đổi ô `userId` (number) → ô `identifier` (text), label "Username hoặc Email *", placeholder `MEM2B4A1D hoặc user@example.com`; validate không rỗng; submit gọi API mới; 404 → "Không tìm thấy người dùng"; bỏ import `Wallet` thừa (đang lint error).

### BUG 2 — Preview khóa chưa duyệt bằng adminToken

- Route mới `/admin/courses/:id/preview` → `AdminCoursePreviewPage.jsx` (mới).
- `AdminCoursePreviewPage`: lấy `adminGetCourseDetailApi(id)` + `adminGetSectionsApi(id)` (đều dùng adminToken). Banner "Chế độ xem trước (chưa xuất bản)" + nút "Duyệt xuất bản" (`publishCourseApi`). Không render nút mua/đăng ký public.
- `AdminPendingCoursesPage.jsx`: nút "Xem trước" → `navigate('/admin/courses/${id}/preview')` (thêm `useNavigate`).
- Phương án thay thế (mở `/courses/${id}?preview=1` rồi cho `CourseDetailPage` đọc adminToken) bị loại vì trộn ngữ cảnh public/admin, dễ rò rỉ hành vi.

### BUG 3 — Sidebar most-specific active

`AdminSidebar.jsx`:
```js
const allUrls = NAV_GROUPS.flatMap(g => g.items).map(i => i.url);
const matchedUrl = (path) =>
  allUrls.filter(url => path === url || path.startsWith(url + '/'))
         .sort((a, b) => b.length - a.length)[0] ?? null;
const isActive = (url) => matchedUrl(location.pathname) === url;
```
- `/admin` → chỉ "Dashboard"; `/admin/courses` → "Khóa học"; `/admin/courses/pending` → "Chờ duyệt"; `/admin/courses/:id/preview` → "Khóa học".

### BUG 4 — Số dư từ AuthContext

`PurchaseConfirmPage.jsx`:
```js
// bỏ: const { balance, deduct } = useWalletStore();
const { balance, updateBalance, fetchProfile } = useAuth();

const insufficientBalance = balance != null && balance < finalPrice;

function formatBalance(amount) {
  if (amount == null) return '---';
  return new Intl.NumberFormat('vi-VN', { style:'currency', currency:'VND', maximumFractionDigits:0 }).format(amount);
}
```
- `useEffect`: nếu `balance == null` → gọi `fetchProfile()`.
- Sau mua thành công: thay `deduct(...)` bằng `fetchProfile()` (đồng bộ chắc chắn từ BE).
- Ô "Số dư ví" dùng `formatBalance(balance)` (giữ `formatMoney` cho giá khóa vì có nhánh `=== 0 → 'Miễn phí'`).
- Server vẫn kiểm tra số dư khi mua (giữ 3.4).

### Yêu cầu B — Avatar (BE) + Redesign account ở AppSidebar (web học viên)

**Backend — thêm field `avatarUrl`**

1. DB migration — thêm cột nullable vào bảng `users`:
```sql
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL;
```

2. `UserJpaEntity` — thêm cột + map 2 chiều:
```java
@Column(name = "avatar_url", length = 500)
private String avatarUrl;
```
- `toDomain()`: truyền `avatarUrl` vào `User.reconstitute(...)` (overload mới, xem dưới).
- `fromDomain(User)`: `e.avatarUrl = user.getAvatarUrl();`

3. `User` (domain) — thêm field `avatarUrl` + getter, và **overload `reconstitute`** để không phá ~10 callers hiện có:
```java
private String avatarUrl;
public String getAvatarUrl() { return avatarUrl; }

// Overload MỚI có avatarUrl (UserJpaEntity dùng cái này)
public static User reconstitute(Long id, String username, String email, String password, String name,
        Role role, boolean isInternal, BigDecimal balance, String avatarUrl,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    User u = reconstitute(id, username, email, password, name, role, isInternal, balance, createdAt, updatedAt);
    u.avatarUrl = avatarUrl;
    return u;
}
```
> Overload `reconstitute` cũ (không có `avatarUrl`) GIỮ NGUYÊN → toàn bộ test và code cũ không phải sửa. `avatarUrl` mặc định null.

4. `UserController.getMyProfile` — thêm `avatarUrl` vào map trả về:
```java
"avatarUrl", user.getAvatarUrl()   // có thể null
```
> Lưu ý `Map.of(...)` KHÔNG cho phép value null. Vì `avatarUrl` có thể null, đổi sang `java.util.HashMap` (hoặc `LinkedHashMap`) khi build `data`, hoặc dùng `user.getAvatarUrl() == null ? "" : user.getAvatarUrl()`. **Chọn:** dùng `HashMap` để giữ null đúng nghĩa (FE check rỗng).

5. (Tùy chọn, không bắt buộc cho bugfix) Endpoint cập nhật avatar `PUT /users/me/avatar` — **ngoài phạm vi**; lần này chỉ cần đọc + trả `avatarUrl`. Avatar có thể seed sẵn trong DB hoặc để null (FE fallback initials).

**Frontend**

- `AuthContext.fetchProfile`: thêm `avatarUrl` vào `userData` lưu localStorage + state:
```js
const userData = { id, username, email, name, role, isInternal, avatarUrl: data.avatarUrl };
```
- `AppSidebar.jsx` — redesign khối account ở footer (chỉ nhánh đã đăng nhập):
```jsx
<SidebarMenuItem>
  <div className="flex items-center gap-2 px-2 py-2">
    {/* Avatar: img nếu có, fallback initials */}
    <div className="size-8 rounded-full overflow-hidden bg-muted flex items-center justify-center shrink-0">
      {publicUser?.avatarUrl
        ? <img src={publicUser.avatarUrl} alt="" className="size-full object-cover" />
        : <span className="text-foreground font-semibold text-xs">
            {publicUser?.name?.charAt(0)?.toUpperCase() || 'U'}
          </span>}
    </div>
    {/* Tên + email + balance */}
    <div className="flex flex-col flex-1 min-w-0 text-left leading-tight">
      <span className="truncate font-semibold text-sm">{publicUser?.name}</span>
      <span className="truncate text-xs text-muted-foreground">{publicUser?.email}</span>
      <span className="truncate text-[11px] mt-0.5 text-emerald-500 font-medium">
        {balance != null ? formatBalance(balance) : '---'}
      </span>
    </div>
    {/* Chevrons + Logout bên phải */}
    <ChevronsUpDown className="size-4 text-muted-foreground shrink-0" />
    <button onClick={() => { publicLogout(); window.location.href = '/login'; }}
            title="Đăng xuất"
            className="p-1 rounded-md hover:bg-muted text-muted-foreground hover:text-foreground shrink-0">
      <LogOut className="size-4" />
    </button>
  </div>
</SidebarMenuItem>
```
- Import thêm `ChevronsUpDown` từ `lucide-react`.
- Nhánh chưa đăng nhập (nút "Đăng nhập") giữ nguyên.
- Mục "Đăng xuất" trong group "Tài khoản" (content) có thể giữ hoặc bỏ — **giữ** để không thay đổi điều hướng quen thuộc; icon logout ở footer là bổ sung theo ảnh mẫu.
- `AdminSidebar.jsx`: **KHÔNG sửa phần account** (theo quyết định 3). Vẫn áp dụng fix Bug 3 (most-specific active) cho admin sidebar.

### Files thay đổi (tổng hợp)

Backend:
- `application/usecase/Wallet/AdminTopUpUseCase.java` (sửa — overload identifier)
- `domain/exception/UserNotFoundException.java` (sửa — constructor String)
- `adapter/dto/request/Wallet/AdminTopUpByIdentifierRequest.java` (mới)
- `adapter/controller/AdminUserController.java` (sửa — endpoint `/top-up`)
- `domain/model/User.java` (sửa — field + overload `reconstitute` có avatarUrl)
- `adapter/repository/jpa/UserEntity/UserJpaEntity.java` (sửa — cột `avatar_url` + map)
- `adapter/controller/UserController.java` (sửa — trả `avatarUrl` trong profile, dùng HashMap)
- SQL migration thêm cột `avatar_url` (file trong `docs/sql/` hoặc `src/main/resources/sql/`)

Frontend:
- `api/adminApi.js` (sửa — `adminTopUpByIdentifierApi`)
- `pages/admin/AdminTopUpPage.jsx` (sửa — ô identifier; bỏ import `Wallet` thừa)
- `pages/admin/AdminPendingCoursesPage.jsx` (sửa — nút Xem trước → route preview)
- `pages/admin/AdminCoursePreviewPage.jsx` (mới — preview adminToken)
- router admin (sửa — route `/admin/courses/:id/preview`)
- `components/layout/AdminSidebar.jsx` (sửa — chỉ `isActive` most-specific; KHÔNG đụng account)
- `pages/PurchaseConfirmPage.jsx` (sửa — balance từ AuthContext, formatBalance null-safe)
- `context/AuthContext.jsx` (sửa — thêm `avatarUrl` vào userData)
- `components/layout/AppSidebar.jsx` (sửa — redesign account footer: avatar img + logout bên phải)

## Testing Strategy

- **Build BE:** `mvnw -q -DskipTests compile` sau khi sửa Java.
- **Unit test BE:** thêm/cập nhật test cho `AdminTopUpUseCase.execute(String, ...)` — resolve theo username, theo email, not-found → `UserNotFoundException`; giữ test cũ cho overload theo id (đảm bảo P1, P2). Kiểm tra `User.reconstitute` overload mới không phá test cũ (P6).
- **Build FE:** `npm run build` / `npm run lint` trong `fe/` để bắt lỗi import/JSX.
- **Manual:**
  1. Bug 1: SUPER_ADMIN nhập username và email → cộng OK; nhập sai → 404. (P1, P2)
  2. Bug 2: "Xem trước" khóa pending → thấy nội dung chưa publish, không thấy nút mua public. (P3)
  3. Bug 3: vào `/admin/courses/pending` → chỉ 1 mục active; kiểm tra `/admin`, `/admin/courses`. (P4)
  4. Bug 4: trang xác nhận → "Số dư ví" hiện số đúng, không "NaN"; mua xong số dư cập nhật. (P5)
  5. Yêu cầu B: `GET /users/me/profile` trả `avatarUrl`; `AppSidebar` hiện avatar img (khi có) / initials (khi null) + icon logout bên phải hoạt động. (P6)

## Quyết định đã chốt

1. **Endpoint Bug 1:** thêm endpoint mới `POST /admin/users/top-up`, GIỮ endpoint `/{userId}/top-up` cũ.
2. **Avatar:** thêm field `avatarUrl` thật ở BE (cột `avatar_url` + trả trong profile/userData). FE: img khi có, fallback initials khi null.
3. **Sidebar redesign:** chỉ sửa `AppSidebar` (web học viên); `AdminSidebar` chỉ nhận fix Bug 3 (most-specific active), không đổi account.
