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
| MEMBER | Đăng ký tự do | Học viên (Có thể là nội bộ hoặc ngoài) |
| INSTRUCTOR | Admin/Staff gán | Giảng viên |
| STAFF | Admin gán | Nhân viên / Trợ lý nội dung |
| ADMIN_USER | Super Admin gán | Quản lý tài khoản |
| SUPER_ADMIN| System | Quản trị viên toàn quyền |

> Xem chi tiết ma trận phân quyền tại `docs/permission-matrix.md`

---

## 5. Database Schema

### Bảng `users`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt hashed) |
| name | VARCHAR(200) | NOT NULL |
| role_id | BIGINT | FK → roles.id, NOT NULL |
| is_internal | BOOLEAN | NOT NULL |
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
        "enrolledCount": 5,
        "price": 500000.00,
        "instructorId": 2
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
    "enrolledCount": 5,
    "price": 500000.00,
    "instructorId": 2,
    "sections": [
      {
        "title": "Phần 1: Giới thiệu",
        "orderIndex": 1,
        "lessons": [
          {
            "title": "Bài 1: Cài đặt môi trường",
            "contentUrl": "https://video-url.com/bai1",
            "orderIndex": 1
          }
        ]
      }
    ]
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

---

### 6.3. Tạo khóa học mới

**Endpoint:** `POST /api/v1/courses`

**Yêu cầu quyền:** `CREATE_COURSE` (ROLE `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`)

**Request Body:**
```json
{
  "title": "Spring Boot Clean Architecture",
  "description": "Khóa học chi tiết",
  "maxStudents": 100,
  "price": 500000.00,
  "requestedInstructorId": 2, 
  "sections": [
    {
      "title": "Phần 1: Khởi đầu",
      "orderIndex": 1,
      "lessons": [
        {
          "title": "Bài 1",
          "contentUrl": "url_bai_1",
          "orderIndex": 1
        }
      ]
    }
  ]
}
```
*Lưu ý: `requestedInstructorId` chỉ bắt buộc nếu người tạo là STAFF/ADMIN. Nếu là INSTRUCTOR, hệ thống sẽ tự động dùng ID của chính INSTRUCTOR đó.*

**Response 201 (Created):**
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 2,
    "title": "Spring Boot Clean Architecture",
    "description": "Khóa học chi tiết",
    "maxStudents": 100,
    "enrolledCount": 0,
    "price": 500000.00,
    "instructorId": 2,
    "sections": [ ... ]
  },
  "timestamp": "2026-05-18T10:00:00"
}
```

---

### 6.4. Chỉnh sửa khóa học

**Endpoint:** `PUT /api/v1/courses/{id}`

**Yêu cầu quyền:** `EDIT_COURSE` (Giảng viên chỉ sửa khóa học của mình; Staff/Admin sửa tự do).

**Request Body:**
*(Tương tự phần Tạo khóa học nhưng cập nhật toàn phần theo Nested JSON)*

**Response 200 (Updated):**
```json
{
  "status": 200,
  "message": "Updated",
  "data": { ... },
  "timestamp": "2026-05-18T10:00:00"
}
```

---

### 6.5. Xóa khóa học

**Endpoint:** `DELETE /api/v1/courses/{id}`

**Yêu cầu quyền:** `DELETE_COURSE` (Giảng viên chỉ xóa khóa học của mình; Staff/Admin xóa tự do).

**Response 200 (Deleted):**
```json
{
  "status": 200,
  "message": "Deleted",
  "timestamp": "2026-05-18T10:00:00"
}
```

---

## 7. Wallet & Monetization

### 7.1. Nạp tiền vào ví (Top-up)

**Endpoint:** `POST /api/v1/users/me/top-up`

**Yêu cầu quyền:** Phải đăng nhập (MEMBER, STAFF, INSTRUCTOR...)

**Request Body:**
```json
{
  "amount": 500000.00
}
```

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Nạp tiền thành công",
  "data": {
    "newBalance": 1500000.00
  },
  "timestamp": "2026-05-18T15:30:00"
}
```

**Response 400 (Bad Request - Số tiền <= 0):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Nạp tiền phải lớn hơn 0",
  "timestamp": "2026-05-18T15:30:00"
}
```

---

### 7.2. Mua Khóa Học

**Endpoint:** `POST /api/v1/courses/{id}/purchase`

**Yêu cầu quyền:** Phải đăng nhập. Hệ thống tự động trừ tiền trong ví. Nếu là nội bộ (isInternal = true), khóa học được tính giá 0đ.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID của khóa học muốn mua |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": 1,
    "paidPrice": 500000.00
  },
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400/500 (Lỗi nghiệp vụ):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Số dư không đủ để thanh toán khóa học. Vui lòng nạp thêm tiền.",
  "timestamp": "2026-05-18T15:35:00"
}
```
*Ghi chú: Sẽ bắn lỗi nếu khóa học đã đầy, hoặc user đã mua khóa học này rồi.*
