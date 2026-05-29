# Auth & JWT

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 1. Auth

### 1.1. Đăng ký tài khoản

**Endpoint:** `POST /api/v1/auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "123456",
  "name": "Nguyễn Văn A"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | Yes | Định dạng email hợp lệ |
| password | String | Yes | Tối thiểu 6 ký tự, tối đa 100 |
| name | String | Yes | Tối thiểu 1 ký tự, tối đa 200 |

**Response 201 (Created):**
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 1,
    "username": "MEM2B4A1D",
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "role": "MEMBER",
    "isInternal": false,
    "createdAt": "2026-05-15T15:30:00"
  },
  "timestamp": "2026-05-15T15:30:00"
}
```

**Response 409 (Email đã tồn tại):**
```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "Email already exists: user@example.com",
  "timestamp": "2026-05-15T15:30:00"
}
```

**Response 400 (Validation lỗi):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "email: must be a well-formed email address; password: size must be between 6 and 100",
  "timestamp": "2026-05-15T15:30:00"
}
```

> Sau khi đăng ký thành công, user luôn có role **MEMBER**. Không trả về accessToken — cần gọi login để lấy token.

---

### 1.2. Đăng nhập

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "identifier": "user@example.com",
  "password": "123456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| identifier | String | Yes | Username hoặc Email |
| password | String | Yes | Không được trống |

**Response 200 (Success):**
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
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2Vy...",
    "lastLogin": "2026-05-15T15:30:00"
  },
  "timestamp": "2026-05-15T15:30:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| accessToken | String | JWT token, thời hạn 24h. Dùng cho header `Authorization: Bearer <token>` |
| role | String | MEMBER / STAFF / ADMIN |

**Response 401 (Sai email/username hoặc password):**
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid credentials",
  "timestamp": "2026-05-15T15:30:00"
}
```

---

### 1.3. Tạo tài khoản (Dành cho Admin/Staff)

**Endpoint:** `POST /api/v1/admin/users`

**Yêu cầu quyền:** Phải có Role `ADMIN_USER`, `SUPER_ADMIN`, hoặc `STAFF`.

**Request Body:**
```json
{
  "email": "gv_nam@example.com",
  "password": "password123",
  "name": "Trần Văn Nam",
  "roleName": "INSTRUCTOR",
  "isInternal": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | Yes | Định dạng email hợp lệ |
| password | String | Yes | Tối thiểu 6 ký tự |
| name | String | Yes | Tên người dùng |
| roleName | String | Yes | Tên Role (MEMBER, INSTRUCTOR, STAFF, v.v) |
| isInternal | Boolean| Yes | true nếu là nội bộ, false nếu ngoài |

**Response 201 (Created):**
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 2,
    "username": "GV7F81A2",
    "email": "gv_nam@example.com",
    "name": "Trần Văn Nam",
    "role": "INSTRUCTOR",
    "isInternal": true,
    "createdAt": "2026-05-18T10:30:00"
  },
  "timestamp": "2026-05-18T10:30:00"
}
```

---

## 2. Cách dùng JWT Token

Sau khi login, gửi token trong tất cả request cần xác thực:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2Vy...
```

Token chứa claims:
```json
{
  "sub": "MEM2B4A1D",
  "userId": 1,
  "email": "user@example.com",
  "role": "MEMBER",
  "isInternal": false,
  "iat": 1715788200,
  "exp": 1715874600
}
```
