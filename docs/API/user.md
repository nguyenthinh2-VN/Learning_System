# User Profile & Admin User Management

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 16. User Profile & Wallet Balance

### 16.1. Xem thông tin cá nhân + số dư ví

**Endpoint:** `GET /api/v1/users/me/profile`

**Yêu cầu quyền:** Đăng nhập (mọi role)

FE dùng endpoint này để hiển thị thông tin user và số dư ví trên header/navbar sau khi login.

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "MEM2B4A1D",
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "role": "MEMBER",
    "isInternal": false,
    "balance": 1500000.00,
    "avatarUrl": "http://localhost:8080/uploads/avatars/3f8a2c1b.png"
  },
  "timestamp": "2026-05-26T12:00:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID user |
| username | String | Username (VD: `MEM2B4A1D`) |
| email | String | Email đăng nhập |
| name | String | Tên hiển thị |
| role | String | `MEMBER` / `INSTRUCTOR` / `STAFF` / `ADMIN_USER` / `SUPER_ADMIN` |
| isInternal | Boolean | `true` nếu là nhân sự nội bộ |
| balance | BigDecimal | Số dư ví hiện tại (đơn vị: VNĐ) |
| avatarUrl | String | URL ảnh đại diện. `null` nếu chưa có |

**Response 404:**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "Không tìm thấy người dùng với ID: 1",
  "timestamp": "2026-05-26T12:00:00"
}
```

> **Gợi ý FE:** Gọi endpoint này ngay sau khi login thành công để lấy `balance` ban đầu. Sau đó lắng nghe WebSocket `/user/queue/wallet` để cập nhật `balance` realtime khi có giao dịch nạp tiền (xem [wallet.md](./wallet.md) mục 14.4).

---

### 16.2. Cập nhật thông tin cá nhân

**Endpoint:** `PUT /api/v1/users/me/profile`

**Yêu cầu quyền:** Đăng nhập (mọi role). Chỉ sửa thông tin của chính mình (xác định qua `userId` trong JWT).

> **Lưu ý phạm vi:** Endpoint self-service này CHỈ cho sửa `name` và `avatarUrl`. Các field định danh / nhạy cảm (`email`, `username`, `role`, `isInternal`, `balance`) KHÔNG sửa được ở đây — chỉ admin mới có quyền (qua Admin User Management).

**Request Body:**
```json
{
  "name": "Nguyễn Văn A (Updated)",
  "avatarUrl": "http://localhost:8080/uploads/avatars/3f8a2c1b.png"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | Yes | `@NotBlank`, độ dài 1–200 |
| avatarUrl | String | No | Tối đa 500 ký tự. `null` = **giữ nguyên** giá trị cũ; `""` = **xóa** avatar (về null) |

**Response 200:**
```json
{
  "status": 200,
  "message": "Cập nhật thông tin thành công",
  "data": {
    "id": 1,
    "username": "MEM2B4A1D",
    "email": "user@example.com",
    "name": "Nguyễn Văn A (Updated)",
    "role": "MEMBER",
    "isInternal": false,
    "balance": 1500000.00,
    "avatarUrl": "http://localhost:8080/uploads/avatars/3f8a2c1b.png"
  },
  "timestamp": "2026-05-29T12:00:00"
}
```

**Response 400 (Validation lỗi):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "name: must not be blank",
  "timestamp": "2026-05-29T12:00:00"
}
```

---

### 16.3. Đổi mật khẩu

**Endpoint:** `PUT /api/v1/users/me/password`

**Yêu cầu quyền:** Đăng nhập (mọi role). Đổi mật khẩu của chính mình.

**Request Body:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| currentPassword | String | Yes | `@NotBlank` — phải khớp mật khẩu hiện tại |
| newPassword | String | Yes | `@NotBlank`, độ dài 6–100, phải **khác** `currentPassword` |

**Response 200:**
```json
{
  "status": 200,
  "message": "Đổi mật khẩu thành công",
  "timestamp": "2026-05-29T12:00:00"
}
```

> JWT là stateless: token cũ vẫn còn hiệu lực tới khi hết hạn (24h). Đổi mật khẩu không thu hồi token đang dùng.

**Response 400 — Mật khẩu hiện tại sai:**
```json
{
  "code": "INVALID_PASSWORD",
  "message": "Mật khẩu hiện tại không đúng.",
  "timestamp": "2026-05-29T12:00:00"
}
```

**Response 400 — Mật khẩu mới trùng mật khẩu cũ:**
```json
{
  "code": "BAD_REQUEST",
  "message": "Mật khẩu mới phải khác mật khẩu cũ.",
  "timestamp": "2026-05-29T12:00:00"
}
```

---

### 16.4. Upload ảnh đại diện (Avatar)

**Endpoint:** `POST /api/v1/users/me/avatar`

**Yêu cầu quyền:** Đăng nhập (mọi role). Upload avatar cho chính mình.

**Content-Type:** `multipart/form-data` — part tên **`file`**.

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| file | File | Yes | Định dạng `image/jpeg`, `image/png`, `image/webp`; tối đa **2MB** |

> Ảnh được lưu trên ổ đĩa của BE (`./uploads/avatars/`), tên file random `{uuid}.{ext}` (không dùng tên gốc của client). Server tự cập nhật `avatarUrl` của user và phục vụ ảnh tĩnh tại `GET /uploads/avatars/{file}` (public, không cần JWT).

**Ví dụ (curl):**
```bash
curl -X POST http://localhost:8080/api/v1/users/me/avatar \
  -H "Authorization: Bearer <token>" \
  -F "file=@avatar.png"
```

**Response 200:**
```json
{
  "status": 200,
  "message": "Cập nhật ảnh đại diện thành công",
  "data": {
    "id": 1,
    "username": "MEM2B4A1D",
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "role": "MEMBER",
    "isInternal": false,
    "balance": 1500000.00,
    "avatarUrl": "http://localhost:8080/uploads/avatars/3f8a2c1b.png"
  },
  "timestamp": "2026-05-29T12:00:00"
}
```

**Response 400 — Định dạng không hợp lệ:**
```json
{
  "code": "INVALID_FILE_TYPE",
  "message": "Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPEG, PNG, WebP.",
  "timestamp": "2026-05-29T12:00:00"
}
```

**Response 400 — File quá lớn:**
```json
{
  "code": "FILE_TOO_LARGE",
  "message": "File vượt quá kích thước tối đa cho phép (2MB).",
  "timestamp": "2026-05-29T12:00:00"
}
```

---

### Mã lỗi (User Profile)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `INVALID_PASSWORD` | Mật khẩu hiện tại không đúng |
| 400 | `INVALID_FILE_TYPE` | Định dạng file ảnh không được hỗ trợ |
| 400 | `FILE_TOO_LARGE` | File vượt quá 2MB |
| 404 | `USER_NOT_FOUND` | Không tìm thấy user |

---

### Tóm tắt các endpoint `/api/v1/users/me/*`

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/me/profile` | GET | Thông tin cá nhân + số dư ví |
| `/me/profile` | PUT | Cập nhật `name` / `avatarUrl` |
| `/me/password` | PUT | Đổi mật khẩu |
| `/me/avatar` | POST | Upload ảnh đại diện (multipart) |
| `/me/top-up` | POST | Nạp tiền trực tiếp (legacy) |
| `/me/enrollments` | GET | Danh sách khóa học đã mua (phân trang) |
| `/me/transactions` | GET | Lịch sử giao dịch ví (nạp + mua, phân trang) |

---

## 17. Admin User Management

### 17.1. Danh sách người dùng (Admin)

**Endpoint:** `GET /api/v1/admin/users`

**Yêu cầu quyền:** `VIEW_USER` (Role `ADMIN_USER`, `SUPER_ADMIN`)

**Query Parameters:**
| Field | Type | Default | Mô tả |
|-------|------|---------|-------|
| keyword | String | (rỗng) | Tìm theo tên hoặc email (LIKE search, case-insensitive) |
| page | Integer | 0 | 0-indexed |
| size | Integer | 20 | Số user/trang |

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 50,
    "totalPages": 3,
    "page": 0,
    "size": 20,
    "items": [
      {
        "id": 1,
        "username": "MEM2B4A1D",
        "email": "user@example.com",
        "name": "Nguyễn Văn A",
        "role": "MEMBER",
        "isInternal": false,
        "enabled": true,
        "createdAt": "2026-05-15T15:30:00"
      }
    ]
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `enabled = false` nghĩa là tài khoản đã bị khóa (không đăng nhập được).

---

### 17.2. Tạo tài khoản (Admin/Staff)

Đã mô tả tại [auth.md](./auth.md) mục 1.3 (`POST /api/v1/admin/users`).

---

### 17.3. Cập nhật thông tin người dùng (Admin)

**Endpoint:** `PUT /api/v1/admin/users/{id}`

**Yêu cầu quyền:** Role `ADMIN_USER` hoặc `SUPER_ADMIN`.

Cho phép admin sửa `name`, `roleName`, `isInternal` của một user. Các field `null` = giữ nguyên giá trị cũ.

> **Không** sửa được `email` / `username` (định danh đăng nhập) và `balance` (dùng luồng top-up) qua endpoint này.

**Request Body:**
```json
{
  "name": "Tên mới",
  "roleName": "INSTRUCTOR",
  "isInternal": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | No | 1–200 ký tự; null = giữ nguyên |
| roleName | String | No | Tên Role hợp lệ (MEMBER, INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN); null = giữ nguyên |
| isInternal | Boolean | No | null = giữ nguyên |

**Response 200:**
```json
{
  "status": 200,
  "message": "Cập nhật người dùng thành công",
  "data": {
    "id": 10,
    "username": "GV7F81A2",
    "email": "gv@example.com",
    "name": "Tên mới",
    "role": "INSTRUCTOR",
    "isInternal": true,
    "enabled": true,
    "createdAt": "2026-05-18T10:30:00"
  },
  "timestamp": "2026-05-30T10:00:00"
}
```

**Quy tắc bảo mật:**
- Admin **không** được tự đổi vai trò của chính mình → `400 BAD_REQUEST`.
- `ADMIN_USER` **không** được chỉnh sửa tài khoản `SUPER_ADMIN` → `400 BAD_REQUEST`.

**Response 404 (User không tồn tại):**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "Không tìm thấy người dùng với ID: 99",
  "timestamp": "2026-05-30T10:00:00"
}
```

**Response 400 (Role không tồn tại / vi phạm quy tắc):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Không tìm thấy vai trò: GHOST",
  "timestamp": "2026-05-30T10:00:00"
}
```

---

### 17.4. Khóa / Mở khóa tài khoản (Admin)

**Endpoint:** `PATCH /api/v1/admin/users/{id}/status`

**Yêu cầu quyền:** Role `ADMIN_USER` hoặc `SUPER_ADMIN`.

Khóa tài khoản (`enabled = false`) sẽ chặn user đó **đăng nhập mới**. Token JWT đã phát hành trước đó vẫn còn hiệu lực tới khi hết hạn (24h) — đây là giới hạn của JWT stateless.

**Request Body:**
```json
{
  "enabled": false
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| enabled | Boolean | Yes | `false` = khóa, `true` = mở khóa |

**Response 200:**
```json
{
  "status": 200,
  "message": "Đã khóa tài khoản",
  "data": {
    "id": 10,
    "username": "GV7F81A2",
    "email": "gv@example.com",
    "name": "Tên mới",
    "role": "INSTRUCTOR",
    "isInternal": true,
    "enabled": false,
    "createdAt": "2026-05-18T10:30:00"
  },
  "timestamp": "2026-05-30T10:00:00"
}
```

**Quy tắc bảo mật:**
- Admin **không** được tự khóa tài khoản của chính mình → `400 BAD_REQUEST`.
- `ADMIN_USER` **không** được khóa tài khoản `SUPER_ADMIN` → `400 BAD_REQUEST`.

**Khi user bị khóa cố đăng nhập** (`POST /api/v1/auth/login`):
```json
{
  "code": "ACCOUNT_DISABLED",
  "message": "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.",
  "timestamp": "2026-05-30T10:00:00"
}
```

---

### Mã lỗi (Admin User Management)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `USER_NOT_FOUND` | Không tìm thấy user |
| 400 | `BAD_REQUEST` | Role không tồn tại / vi phạm quy tắc tự-sửa / ADMIN_USER thao tác SUPER_ADMIN |
| 403 | `ACCOUNT_DISABLED` | Tài khoản bị khóa (khi đăng nhập) |
