# API Documentation — Learning System

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
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "role": "MEMBER",
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
  "email": "user@example.com",
  "password": "123456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | Yes | Định dạng email hợp lệ |
| password | String | Yes | Không được trống |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "Nguyễn Văn A",
    "role": "MEMBER",
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

**Response 401 (Sai email hoặc password):**
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "timestamp": "2026-05-15T15:30:00"
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
  "sub": "user@example.com",
  "userId": 1,
  "role": "MEMBER",
  "iat": 1715788200,
  "exp": 1715874600
}
```

---

## 3. Error Response Format (chung)

Tất cả lỗi trả về format thống nhất:

```json
{
  "code": "ERROR_CODE",
  "message": "Mô tả lỗi bằng tiếng Việt",
  "timestamp": "2026-05-15T15:30:00"
}
```

### Mã lỗi

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ |
| 400 | `INVALID_EMAIL` | Email sai định dạng |
| 401 | `INVALID_CREDENTIALS` | Email hoặc mật khẩu sai |
| 404 | `USER_NOT_FOUND` | Không tìm thấy user |
| 409 | `EMAIL_ALREADY_EXISTS` | Email đã được đăng ký |
| 500 | `INTERNAL_ERROR` | Lỗi hệ thống |

---

## 4. Roles & Permissions

| Role | Mặc định khi | Mô tả |
|------|-------------|-------|
| MEMBER | Đăng ký | Học viên |
| STAFF | Admin gán | Nhân viên / giảng viên |
| ADMIN | Admin gán | Quản trị viên toàn quyền |

> Xem chi tiết ma trận phân quyền tại `docs/permission-matrix.md`

---

## 5. Database Schema

### Bảng `users`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt hashed) |
| name | VARCHAR(200) | NOT NULL |
| role_id | BIGINT | FK → roles.id, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### Bảng `roles`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | |

### Bảng `permissions`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | |

### Bảng `role_permissions`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| role_id | BIGINT | FK → roles.id |
| permission_id | BIGINT | FK → permissions.id |

---

## 6. Course Management

### 6.1. Lấy danh sách khóa học (Có phân trang, tìm kiếm)

**Endpoint:** `GET /api/v1/courses`

**Query Parameters:**
| Field | Type | Default | Mô tả |
|-------|------|---------|-------|
| keyword | String | (rỗng) | Tìm kiếm theo tiêu đề khóa học |
| page | Integer| 0 | Số trang (0-indexed) |
| size | Integer| 10 | Số lượng khóa học trên 1 trang |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10,
    "items": [
      {
        "id": 1,
        "title": "Spring Boot Clean Architecture",
        "description": "Học xây dựng ứng dụng với Clean Architecture",
        "maxStudents": 100,
        "enrolledCount": 5
      }
    ]
  },
  "timestamp": "2026-05-18T10:00:00"
}
```

---

### 6.2. Xem chi tiết khóa học

**Endpoint:** `GET /api/v1/courses/{id}`

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID của khóa học |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "title": "Spring Boot Clean Architecture",
    "description": "Học xây dựng ứng dụng với Clean Architecture",
    "maxStudents": 100,
    "enrolledCount": 5
  },
  "timestamp": "2026-05-18T10:00:00"
}
```

**Response 404 (Không tìm thấy):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Course not found with id: 999",
  "timestamp": "2026-05-18T10:00:00"
}
```
