# Kế hoạch: Admin sửa & khóa tài khoản người dùng

## Tổng quan

Hiện `AdminUserController` chỉ có:
- `GET /api/v1/admin/users` — danh sách (phân trang, tìm kiếm).
- `POST /api/v1/admin/users` — tạo user.
- `POST /api/v1/admin/users/{userId}/top-up` + `/top-up` — cộng tiền.

Thiếu khả năng **sửa** một user đã tồn tại: đổi role, bật/tắt `isInternal`, đổi tên, và **khóa/mở khóa tài khoản** (vô hiệu hóa đăng nhập). Khi làm tính năng update profile self-service, ta đã cố ý để các field nhạy cảm (`role`, `isInternal`) chỉ admin được sửa — nhưng endpoint admin tương ứng chưa tồn tại.

Tính năng này bổ sung:
1. `PUT /api/v1/admin/users/{id}` — cập nhật `name`, `roleName`, `isInternal`.
2. Cơ chế **khóa tài khoản** (`enabled = false`) + endpoint bật/tắt, và chặn đăng nhập user bị khóa.

---

## Phạm vi

### ✅ Trong phạm vi

| # | Endpoint | Mục đích |
|---|----------|----------|
| A | `PUT /api/v1/admin/users/{id}` | Sửa `name`, `roleName`, `isInternal` của user |
| B | `PATCH /api/v1/admin/users/{id}/status` | Khóa / mở khóa tài khoản (`enabled` true/false) |

### ❌ Ngoài phạm vi (cố ý)

- ❌ Admin đổi mật khẩu / reset mật khẩu user khác — luồng reset password riêng (làm sau, có thể kèm email).
- ❌ Admin sửa `email` / `username` — định danh đăng nhập, đổi cần cân nhắc kỹ (uniqueness, lịch sử). Để sau.
- ❌ Admin sửa `balance` trực tiếp — đã có luồng top-up riêng; không cho set thẳng số dư.
- ❌ Xóa cứng user — dùng khóa tài khoản (soft) thay cho xóa, bảo toàn enrollment / giao dịch / lịch sử.
- ❌ Audit log đầy đủ mọi thay đổi user — có thể thêm sau; đợt này chỉ log cơ bản.

---

## Quy tắc nghiệp vụ & bảo mật

1. **Phân quyền**: chỉ `ADMIN_USER` và `SUPER_ADMIN` được sửa/khóa user (nhất quán với `GET /admin/users` hiện dùng `hasAnyRole('ADMIN_USER','SUPER_ADMIN')`).
2. **Không tự hạ quyền / tự khóa chính mình**: admin KHÔNG được khóa tài khoản của chính mình, và KHÔNG được đổi role của chính mình (chống tự khóa toàn bộ quyền quản trị). So sánh `id` mục tiêu với `userId` trong JWT → nếu trùng và thao tác nguy hiểm (khóa / đổi role) → `400 BAD_REQUEST` (hoặc `403`).
3. **Bảo vệ SUPER_ADMIN**: `ADMIN_USER` KHÔNG được sửa role hay khóa một user đang là `SUPER_ADMIN` (tránh leo thang/đảo quyền). Chỉ `SUPER_ADMIN` mới thao tác được trên `SUPER_ADMIN` khác. (Nếu muốn đơn giản hơn: chỉ `SUPER_ADMIN` được đổi role — cân nhắc khi review.)
4. **Đổi role**: `roleName` phải tồn tại trong `RoleRepository` (đã có `findByName`); không tồn tại → `400 ROLE_NOT_FOUND` / `BAD_REQUEST`.
5. **Khóa tài khoản chặn đăng nhập**: user có `enabled = false` thì `LoginUseCase` từ chối với lỗi rõ ràng (`403 ACCOUNT_DISABLED` hoặc tái dùng `INVALID_CREDENTIALS` để không lộ trạng thái — đề xuất một mã riêng `ACCOUNT_DISABLED` cho UX rõ ràng).
6. **JWT đã phát hành vẫn còn hiệu lực tới khi hết hạn** (stateless). Khóa tài khoản chặn đăng nhập MỚI; token cũ vẫn dùng được tối đa 24h. Ghi chú; nếu cần chặn ngay phải thêm kiểm tra `enabled` trong `JwtAuthenticationFilter` (cân nhắc — thêm 1 query mỗi request). Đề xuất đợt này: chặn ở login, ghi chú giới hạn.

---

## Thiết kế theo Clean Architecture

### 1. Domain Layer (`domain/`)

**`domain/model/User.java`** (sửa):
- Thêm field `boolean enabled` (mặc định `true`).
- Thêm hành vi:
  ```java
  public void changeRole(Role newRole) { if (newRole == null) throw ...; this.role = newRole; }
  public void setInternal(boolean internal) { this.isInternal = internal; }  // hoặc đặt tên markInternal/markExternal
  public void rename(String newName) { ... }     // (đã có changeName từ update-profile — tái dùng)
  public void disable() { this.enabled = false; }
  public void enable() { this.enabled = true; }
  ```
- Cập nhật `create` / `reconstitute` để khởi tạo `enabled` (mặc định true cho user cũ).

> Lưu ý: `changeName` đã được thêm ở tính năng update-profile — tái dùng, không tạo trùng.

### 2. Application Layer (`application/`)

**Repository**: `UserRepository` đã có `findById`, `findByIdForUpdate`, `save`. Đủ dùng — chỉ cần đảm bảo `save` map field `enabled`.

**DTO** (`application/dto/User/`):
- `AdminUpdateUserInput(Long targetUserId, Long requesterId, String requesterRole, String name, String roleName, Boolean isInternal)`.
- `AdminUpdateUserStatusInput(Long targetUserId, Long requesterId, String requesterRole, boolean enabled)`.
- Tái dùng `UserListOutput` / hoặc thêm `AdminUserDetailOutput` cho response.

**Use case** (`application/usecase/User/`):
- `AdminUpdateUserUseCase.execute(AdminUpdateUserInput)`:
  - `@Transactional`, `findByIdForUpdate(targetUserId)` (404 nếu thiếu).
  - Kiểm tra quy tắc: không tự đổi role mình; ADMIN_USER không sửa SUPER_ADMIN (Rule 2, 3).
  - Resolve `Role` qua `RoleRepository.findByName` nếu `roleName` thay đổi.
  - Apply `rename`, `changeRole`, `setInternal` theo field gửi lên (null = giữ nguyên).
  - `save` → trả output.
- `AdminSetUserStatusUseCase.execute(AdminUpdateUserStatusInput)`:
  - `@Transactional`, `findByIdForUpdate` (404).
  - Không cho tự khóa mình (Rule 2); ADMIN_USER không khóa SUPER_ADMIN (Rule 3).
  - `enable()` / `disable()` → `save`.

**`LoginUseCase`** (sửa): sau khi verify password, kiểm tra `user.isEnabled()`; nếu false → ném `AccountDisabledException` (hoặc `InvalidCredentialsException`).

### 3. Adapter Layer (`adapter/`)

**Request DTO** (`adapter/dto/request/User/`):
- `AdminUpdateUserRequest { @Size(1,200) String name; String roleName; Boolean isInternal; }` (tất cả nullable = chỉ sửa field gửi lên) + `toInput(targetId, requesterId, requesterRole)`.
- `AdminUpdateUserStatusRequest { @NotNull Boolean enabled; }` + `toInput(...)`.

**Response DTO**: `AdminUserDetailResponse` (hoặc tái dùng `UserListResponse` + thêm `enabled`).

**`UserJpaEntity`** (sửa): thêm cột `enabled BOOLEAN NOT NULL DEFAULT TRUE` + map `toDomain`/`fromDomain`.

**`AdminUserController`** (sửa): thêm 2 endpoint với `@PreAuthorize("hasAnyRole('ADMIN_USER','SUPER_ADMIN')")`, lấy `requesterId`/role từ JWT (controller này chưa parse JWT — cần inject `JwtService` + `HttpServletRequest` như `UserController`, hoặc dùng `@AuthenticationPrincipal`/SecurityContext). Đề xuất: theo pattern `getClaims` đã dùng ở `UserController` để nhất quán.

### 4. Infrastructure Layer

- `ErrorCode`: thêm `ACCOUNT_DISABLED` (nếu chọn mã riêng). `ROLE_NOT_FOUND` đã có.
- `GlobalExceptionHandler`: thêm handler cho `AccountDisabledException` → 403; quy tắc tự-khóa/tự-đổi-role dùng `IllegalArgumentException`/`IllegalStateException` → 400 (đã có handler).
- `domain/exception/AccountDisabledException.java` (mới, nếu dùng mã riêng).

---

## Data Model

### Bảng `users` (thêm cột)
```sql
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
```
`ddl-auto: update` thêm cột tự động; user cũ mặc định `TRUE`.

---

## API (ghi vào `docs/API/user.md`)

- `PUT /api/v1/admin/users/{id}` — body `{ name?, roleName?, isInternal? }`, response user đã cập nhật. Lỗi: 404 USER_NOT_FOUND, 400 ROLE_NOT_FOUND/BAD_REQUEST, 403 ACCESS_DENIED.
- `PATCH /api/v1/admin/users/{id}/status` — body `{ enabled }`. Lỗi tương tự + quy tắc tự-khóa.
- Cập nhật `endpoints-summary.md` mục Admin User Management.
- Ghi rõ `ACCOUNT_DISABLED` vào bảng mã lỗi (nếu dùng).

---

## Frontend

- `AdminUsersPage.jsx`: thêm nút "Sửa" mở modal (đổi name / role / isInternal) và toggle "Khóa/Mở khóa".
- `adminApi.js`: `adminUpdateUserApi(id, data)`, `adminSetUserStatusApi(id, enabled)`.
- Hiển thị badge trạng thái (Đang hoạt động / Đã khóa) trong bảng user.
- Chặn UI: ẩn/disable nút khóa và đổi role với chính mình; ADMIN_USER không thấy thao tác trên SUPER_ADMIN.

---

## Kiểm thử

- `AdminUpdateUserUseCaseTest`: đổi name/role/isInternal thành công; roleName không tồn tại → lỗi; user không tồn tại → 404; ADMIN_USER sửa SUPER_ADMIN → bị chặn; tự đổi role mình → bị chặn; field null = giữ nguyên.
- `AdminSetUserStatusUseCaseTest`: khóa/mở khóa thành công; tự khóa mình → bị chặn; ADMIN_USER khóa SUPER_ADMIN → bị chặn.
- `LoginUseCaseTest` (mở rộng): user `enabled = false` → đăng nhập bị từ chối.
- Domain `UserTest`: `disable/enable/changeRole/setInternal`.
- Chạy `mvnw test` xanh trước khi hoàn thành.

---

## Các file liên quan

### Tạo mới
- `application/dto/User/AdminUpdateUserInput.java`
- `application/dto/User/AdminUpdateUserStatusInput.java`
- `application/dto/User/AdminUserDetailOutput.java` (hoặc tái dùng UserListOutput + enabled)
- `application/usecase/User/AdminUpdateUserUseCase.java`
- `application/usecase/User/AdminSetUserStatusUseCase.java`
- `adapter/dto/request/User/AdminUpdateUserRequest.java`
- `adapter/dto/request/User/AdminUpdateUserStatusRequest.java`
- `adapter/dto/response/AdminUserDetailResponse.java`
- `domain/exception/AccountDisabledException.java` (nếu dùng mã riêng)
- `src/test/.../AdminUpdateUserUseCaseTest.java`, `AdminSetUserStatusUseCaseTest.java`

### Chỉnh sửa
- `domain/model/User.java` (thêm `enabled` + hành vi `disable/enable/changeRole/setInternal`)
- `adapter/repository/jpa/UserEntity/UserJpaEntity.java` (cột `enabled` + map)
- `adapter/controller/AdminUserController.java` (2 endpoint + inject JwtService)
- `application/usecase/Auth/LoginUseCase.java` (chặn user bị khóa)
- `infrastructure/exception/ErrorCode.java` + `GlobalExceptionHandler.java`
- `fe/src/pages/admin/AdminUsersPage.jsx`, `fe/src/api/adminApi.js`
- `docs/API/user.md`, `docs/API/endpoints-summary.md`

### Tham khảo (chỉ đọc)
- `application/usecase/User/AdminCreateUserUseCase.java` (mẫu resolve Role + tạo user)
- `application/usecase/User/GetUsersUseCase.java`
- `application/repository/RoleRepository.java`
