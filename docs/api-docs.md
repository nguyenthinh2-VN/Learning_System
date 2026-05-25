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

> **Lưu ý quan trọng:** Endpoint public này CHỈ trả về các khóa học đã được duyệt (`published = true`). Course mới do INSTRUCTOR / STAFF / ADMIN tạo mặc định ẩn (`published = false`) và không xuất hiện ở đây cho đến khi STAFF / SUPER_ADMIN duyệt qua `POST /api/v1/admin/courses/{id}/publish`. INSTRUCTOR muốn xem khóa học của mình (kể cả khi chưa duyệt) phải dùng `GET /api/v1/instructor/courses`.

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

> **Workflow duyệt:** Course tạo ra mặc định `published = false`, `priceLocked = false`, KHÔNG xuất hiện ở public listing. STAFF / SUPER_ADMIN cần duyệt qua `POST /api/v1/admin/courses/{id}/publish` để công khai. Khi publish, server tự động set `priceLocked = true`, INSTRUCTOR không sửa được giá nữa (xem section 10).

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

**Yêu cầu quyền:** Phải đăng nhập. Hệ thống tự động trừ tiền trong ví. Nếu là nội bộ (`isInternal = true`), khóa học được tính giá 0đ.

> **Voucher tích hợp:** Body có thể chứa `voucherCode` (tùy chọn) để áp dụng giảm giá. Server LUÔN tính lại giá ở thời điểm checkout, không tin giá quote trước đó. Xem chi tiết tại Section 11 (Voucher Pricing).

> **Anti-tampering:** DTO request CHỈ khai báo `voucherCode`. Mọi field giá khác (`price`, `originalPrice`, `discount`, `finalPrice`, `paidPrice`) nếu có trong body sẽ bị bỏ qua hoàn toàn.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID của khóa học muốn mua |

**Request Body (tùy chọn):**
```json
{
  "voucherCode": "WELCOME50"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| voucherCode | String | No | 0–32 ký tự, regex `^[A-Za-z0-9_-]*$` |

**Response 200 (Success — không voucher):**
```json
{
  "status": 200,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": 1,
    "originalPrice": 500000.00,
    "discountAmount": 0.00,
    "finalPrice": 500000.00,
    "paidPrice": 500000.00,
    "voucherApplied": false,
    "voucherCode": null
  },
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 200 (Success — có voucher):**
```json
{
  "status": 200,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": 1,
    "originalPrice": 500000.00,
    "discountAmount": 50000.00,
    "finalPrice": 450000.00,
    "paidPrice": 450000.00,
    "voucherApplied": true,
    "voucherCode": "WELCOME50"
  },
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Course chưa publish:**
```json
{
  "code": "COURSE_NOT_PUBLISHED",
  "message": "Khóa học với ID 5 chưa được duyệt và công khai.",
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Đã enrolled:**
```json
{
  "code": "ALREADY_ENROLLED",
  "message": "Bạn đã đăng ký khóa học này rồi.",
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Số dư không đủ:**
```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Số dư không đủ để thanh toán khóa học.",
  "timestamp": "2026-05-18T15:35:00"
}
```

*Ghi chú:*
- Khóa học đã đầy → HTTP 400 với mã `BAD_REQUEST`.
- Internal Member (`isInternal = true`) luôn `paidPrice = 0`, voucher bị bỏ qua, không tạo Voucher_Usage.
- INSTRUCTOR / STAFF / ADMIN_USER gửi `voucherCode` → HTTP 403 (`VOUCHER_USE_DENIED`).
- Mọi lỗi voucher (`VOUCHER_NOT_FOUND`, `VOUCHER_EXPIRED`, `VOUCHER_USAGE_LIMIT_REACHED`...) xem section 11.

---

## 8. Course Section Management

Base path: `/api/v1/courses/{courseId}/sections`

**Phân quyền:**
| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Xem sections | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tạo section | ❌ | ✅ (course của mình) | ✅ | ❌ | ✅ |
| Sửa section | ❌ | ✅ (course của mình) | ✅ | ❌ | ✅ |
| Xóa section | ❌ | ✅ (course của mình) | ✅ | ❌ | ✅ |

> **Lưu ý:** `ADMIN_USER` không có quyền thao tác Section (khác với Course-level).

---

### 8.1. Lấy danh sách Sections của một Course

**Endpoint:** `GET /api/v1/courses/{courseId}/sections`

**Yêu cầu quyền:** Đăng nhập (tất cả role).

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "title": "Phần 1: Giới thiệu",
      "orderIndex": 1,
      "lessons": [
        {
          "id": 1,
          "title": "Bài 1: Cài đặt môi trường",
          "contentUrl": "https://video-url.com/bai1",
          "orderIndex": 1
        }
      ]
    },
    {
      "id": 2,
      "title": "Phần 2: Clean Architecture",
      "orderIndex": 2,
      "lessons": []
    }
  ],
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-22T10:00:00"
}
```

---

### 8.2. Tạo Section mới

**Endpoint:** `POST /api/v1/courses/{courseId}/sections`

**Yêu cầu quyền:** `CREATE_SECTION` — Role `INSTRUCTOR` (chỉ course của mình), `STAFF`, `SUPER_ADMIN`.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |

**Request Body:**
```json
{
  "title": "Phần 3: Domain Layer",
  "orderIndex": 3
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | Yes | Không được để trống |
| orderIndex | Integer | Yes | >= 0 |

**Response 201 (Created):**
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 3,
    "title": "Phần 3: Domain Layer",
    "orderIndex": 3,
    "lessons": []
  },
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "SECTION_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền thêm chương học trong khóa học do chính mình tạo.",
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-22T10:00:00"
}
```

---

### 8.3. Cập nhật Section

**Endpoint:** `PUT /api/v1/courses/{courseId}/sections/{sectionId}`

**Yêu cầu quyền:** `EDIT_SECTION` — Role `INSTRUCTOR` (chỉ course của mình), `STAFF`, `SUPER_ADMIN`.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section cần cập nhật |

**Request Body:**
```json
{
  "title": "Phần 3: Domain Layer (Updated)",
  "orderIndex": 3
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | Yes | Không được để trống |
| orderIndex | Integer | Yes | >= 0 |

**Response 200 (Updated):**
```json
{
  "status": 200,
  "message": "Updated",
  "data": {
    "id": 3,
    "title": "Phần 3: Domain Layer (Updated)",
    "orderIndex": 3,
    "lessons": [...]
  },
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "SECTION_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền sửa/xóa chương học trong khóa học do chính mình tạo.",
  "timestamp": "2026-05-22T10:00:00"
}
```

---

### 8.4. Xóa Section

**Endpoint:** `DELETE /api/v1/courses/{courseId}/sections/{sectionId}`

**Yêu cầu quyền:** `EDIT_SECTION` — Role `INSTRUCTOR` (chỉ course của mình), `STAFF`, `SUPER_ADMIN`.

> **Lưu ý:** Xóa section sẽ tự động xóa toàn bộ lessons bên trong (cascade delete).

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section cần xóa |

**Response 200 (Deleted):**
```json
{
  "status": 200,
  "message": "Deleted",
  "data": null,
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-22T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "SECTION_ACCESS_DENIED",
  "message": "Bạn không có quyền sửa/xóa chương học.",
  "timestamp": "2026-05-22T10:00:00"
}
```

---

### Mã lỗi bổ sung (Section)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `SECTION_NOT_FOUND` | Không tìm thấy chương học |
| 403 | `SECTION_ACCESS_DENIED` | Không có quyền thao tác chương học |


## 9. Course Lesson Management

Base path: `/api/v1/courses/{courseId}/sections/{sectionId}/lessons`

**Phân quyền:**
| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Xem lessons | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tạo lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |
| Sửa lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |
| Xóa lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |

> **Lưu ý:** `ADMIN_USER` không có quyền thao tác Lesson (giống với Section-level).

---

### 9.1. Lấy danh sách Lessons của một Section

**Endpoint:** `GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons`

**Yêu cầu quyền:** Đăng nhập (tất cả role).

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section |

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "courseId": 1,
    "sectionId": 1,
    "lessons": [
      {
        "id": 1,
        "title": "Bài 1: Cài đặt môi trường",
        "contentUrl": "https://video-url.com/bai1",
        "orderIndex": 1
      },
      {
        "id": 2,
        "title": "Bài 2: Giới thiệu Clean Architecture",
        "contentUrl": "https://video-url.com/bai2",
        "orderIndex": 2
      }
    ]
  },
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

---

### 9.2. Tạo Lesson mới

**Endpoint:** `POST /api/v1/courses/{courseId}/sections/{sectionId}/lessons`

**Yêu cầu quyền:** `CREATE_LESSON` — Role `INSTRUCTOR` (chỉ section của course mình), `STAFF`, `SUPER_ADMIN`.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section |

**Request Body:**
```json
{
  "title": "Bài 3: Domain Model Design",
  "contentUrl": "https://video-url.com/bai3",
  "orderIndex": 3
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | Yes | Không được để trống |
| contentUrl | String | Yes | URL nội dung bài giảng (YouTube, Vimeo, S3...) |
| orderIndex | Integer | Yes | >= 0 |

**Response 201 (Created):**
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 3,
    "title": "Bài 3: Domain Model Design",
    "contentUrl": "https://video-url.com/bai3",
    "orderIndex": 3
  },
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền thêm bài giảng trong chương học thuộc khóa học do chính mình tạo.",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

---

### 9.3. Cập nhật Lesson

**Endpoint:** `PUT /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}`

**Yêu cầu quyền:** `EDIT_LESSON` — Role `INSTRUCTOR` (chỉ section của course mình), `STAFF`, `SUPER_ADMIN`.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section |
| lessonId | Long | ID của lesson cần cập nhật |

**Request Body:**
```json
{
  "title": "Bài 3: Domain Model Design (Updated)",
  "contentUrl": "https://video-url.com/bai3-updated",
  "orderIndex": 3
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | Yes | Không được để trống |
| contentUrl | String | Yes | URL nội dung bài giảng |
| orderIndex | Integer | Yes | >= 0 |

**Response 200 (Updated):**
```json
{
  "status": 200,
  "message": "Updated",
  "data": {
    "id": 3,
    "title": "Bài 3: Domain Model Design (Updated)",
    "contentUrl": "https://video-url.com/bai3-updated",
    "orderIndex": 3
  },
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Lesson không tồn tại):**
```json
{
  "code": "LESSON_NOT_FOUND",
  "message": "Không tìm thấy bài giảng với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền sửa/xóa bài giảng trong chương học thuộc khóa học do chính mình tạo.",
  "timestamp": "2026-05-23T10:00:00"
}
```

---

### 9.4. Xóa Lesson

**Endpoint:** `DELETE /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}`

**Yêu cầu quyền:** `EDIT_LESSON` — Role `INSTRUCTOR` (chỉ section của course mình), `STAFF`, `SUPER_ADMIN`.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID của khóa học |
| sectionId | Long | ID của section |
| lessonId | Long | ID của lesson cần xóa |

**Response 200 (Deleted):**
```json
{
  "status": 200,
  "message": "Deleted",
  "data": null,
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Lesson không tồn tại):**
```json
{
  "code": "LESSON_NOT_FOUND",
  "message": "Không tìm thấy bài giảng với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Không tìm thấy khóa học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 404 (Section không tồn tại):**
```json
{
  "code": "SECTION_NOT_FOUND",
  "message": "Không tìm thấy chương học với ID: 99",
  "timestamp": "2026-05-23T10:00:00"
}
```

**Response 403 (Không có quyền):**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Bạn không có quyền sửa/xóa bài giảng.",
  "timestamp": "2026-05-23T10:00:00"
}
```

---

### Mã lỗi bổ sung (Lesson)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `LESSON_NOT_FOUND` | Không tìm thấy bài giảng |
| 403 | `LESSON_ACCESS_DENIED` | Không có quyền thao tác bài giảng |

---

## 10. Course Approval & Visibility

Khi INSTRUCTOR / STAFF / ADMIN tạo khóa học mới (`POST /api/v1/courses`), course sẽ ở trạng thái **DRAFT** (`published = false`, `priceLocked = false`) — KHÔNG xuất hiện ở public listing. STAFF / SUPER_ADMIN cần duyệt qua endpoint publish để đưa course lên public.

**Hai cờ trên course:**

| Cờ | Ý nghĩa |
|----|---------|
| `published` | Course đã được duyệt và công khai cho MEMBER xem / mua |
| `priceLocked` | Giá bị khóa, INSTRUCTOR không sửa được (chỉ admin có `LOCK_COURSE_PRICE`) |

**Workflow:**

```
INSTRUCTOR tạo course (published = false, priceLocked = false)
  ↓
INSTRUCTOR thêm sections / lessons (vẫn ở draft)
  ↓
STAFF / SUPER_ADMIN xem GET /api/v1/admin/courses/pending
  ↓
STAFF / SUPER_ADMIN có thể PUT /api/v1/admin/courses/{id}/price để chỉnh giá
  ↓
STAFF / SUPER_ADMIN POST /api/v1/admin/courses/{id}/publish
  → published = true, priceLocked = true, publishedAt = now, publishedBy = adminId
  → Course xuất hiện ở public listing và mua được
  ↓
(Tùy chọn) STAFF / SUPER_ADMIN POST /api/v1/admin/courses/{id}/unpublish nếu cần ẩn
  → published = false (giữ nguyên priceLocked = true)
  → Course biến mất khỏi public listing nhưng giữ nguyên enrollment đã có
```

---

### 10.1. Danh sách course chưa duyệt (Pending)

**Endpoint:** `GET /api/v1/admin/courses/pending`

**Yêu cầu quyền:** `PUBLISH_COURSE` (Role STAFF, SUPER_ADMIN).

**Query Parameters:**
| Field | Type | Default | Mô tả |
|-------|------|---------|-------|
| keyword | String | (rỗng) | Tìm kiếm theo tiêu đề |
| page | Integer | 0 | 0-indexed |
| size | Integer | 10 | |

**Response 200 (Success):** Trả về danh sách course có `published = false`.

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
        "id": 5,
        "title": "Spring Boot Advanced",
        "price": 800000.00,
        "instructorId": 2,
        "published": false,
        "priceLocked": false
      }
    ]
  },
  "timestamp": "2026-05-24T10:00:00"
}
```

---

### 10.2. Danh sách toàn bộ course (Admin)

**Endpoint:** `GET /api/v1/admin/courses`

**Yêu cầu quyền:** `PUBLISH_COURSE`.

Query parameters tương tự pending. Response trả cả course đã publish và chưa publish.

---

### 10.3. Duyệt và Publish course

**Endpoint:** `POST /api/v1/admin/courses/{id}/publish`

**Yêu cầu quyền:** `PUBLISH_COURSE` (Role STAFF, SUPER_ADMIN).

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID của khóa học |

**Response 200 (Published):**
```json
{
  "status": 200,
  "message": "Khóa học đã được duyệt và public",
  "data": {
    "id": 5,
    "title": "Spring Boot Advanced",
    "price": 800000.00,
    "published": true,
    "priceLocked": true,
    "publishedAt": "2026-05-24T10:30:00",
    "publishedBy": 3
  },
  "timestamp": "2026-05-24T10:30:00"
}
```

**Response 400 — Course đã publish:**
```json
{
  "code": "COURSE_ALREADY_PUBLISHED",
  "message": "Khóa học với ID 5 đã được publish trước đó.",
  "timestamp": "2026-05-24T10:30:00"
}
```

**Response 400 — Giá không hợp lệ:**
```json
{
  "code": "BAD_REQUEST",
  "message": "Giá khóa học không hợp lệ. Vui lòng đặt giá trước khi publish.",
  "timestamp": "2026-05-24T10:30:00"
}
```

---

### 10.4. Unpublish course

**Endpoint:** `POST /api/v1/admin/courses/{id}/unpublish`

**Yêu cầu quyền:** `PUBLISH_COURSE`.

> Khi unpublish, `published = false` nhưng `priceLocked` GIỮ NGUYÊN. Các Enrollment đã có không bị xóa, học viên đã mua vẫn truy cập được khóa học.

**Response 200:**
```json
{
  "status": 200,
  "message": "Khóa học đã được ẩn khỏi public",
  "data": {
    "id": 5,
    "published": false,
    "priceLocked": true
  },
  "timestamp": "2026-05-24T10:35:00"
}
```

---

### 10.5. Cập nhật giá (Admin override khi priceLocked)

**Endpoint:** `PUT /api/v1/admin/courses/{id}/price`

**Yêu cầu quyền:** `LOCK_COURSE_PRICE` (Role STAFF, SUPER_ADMIN).

> Endpoint này cho phép admin sửa giá KỂ CẢ khi `priceLocked = true`. INSTRUCTOR sửa qua `PUT /api/v1/courses/{id}` thông thường sẽ bị chặn nếu `priceLocked = true`.

**Request Body:**
```json
{
  "price": 750000.00
}
```

**Response 200:**
```json
{
  "status": 200,
  "message": "Giá khóa học đã được cập nhật",
  "data": {
    "id": 5,
    "price": 750000.00,
    "priceLocked": true
  },
  "timestamp": "2026-05-24T10:40:00"
}
```

---

### 10.6. Course của Instructor (kể cả chưa publish)

**Endpoint:** `GET /api/v1/instructor/courses`

**Yêu cầu quyền:** Đăng nhập + Role `INSTRUCTOR`.

> Instructor chỉ thấy course do chính mình tạo. Bao gồm cả course chưa publish.

**Query Parameters:** `keyword`, `page`, `size`.

**Response 200:** Tương tự admin pending nhưng filter theo `instructorId` của chính requester.

---

### 10.7. Chi tiết course của Instructor

**Endpoint:** `GET /api/v1/instructor/courses/{id}`

**Yêu cầu quyền:** Đăng nhập + Role `INSTRUCTOR` + sở hữu course.

> Instructor xem được chi tiết course của mình kể cả khi chưa publish.

---

### 10.8. Lưu ý phân quyền sau publish

Khi course đã `published = true` và `priceLocked = true`:

| Role | Sửa metadata (title, description, maxStudents) | Sửa price |
|------|-----------------------------------------------|-----------|
| INSTRUCTOR (owner) | Có | **KHÔNG** (`COURSE_PRICE_LOCKED`) |
| STAFF | Có | Có (qua `/api/v1/admin/courses/{id}/price`) |
| ADMIN_USER | Có | Có |
| SUPER_ADMIN | Có | Có |

INSTRUCTOR cố tình truyền `price` trong `PUT /api/v1/courses/{id}` khi `priceLocked = true` sẽ nhận:

```json
{
  "code": "COURSE_PRICE_LOCKED",
  "message": "Giá khóa học đã bị khóa. Vui lòng liên hệ admin để sửa.",
  "timestamp": "2026-05-24T11:00:00"
}
```

---

### Mã lỗi bổ sung (Course Approval)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `COURSE_NOT_PUBLISHED` | Khóa học chưa được duyệt và công khai |
| 400 | `COURSE_PRICE_LOCKED` | Giá khóa học đã bị khóa, không thể sửa |
| 400 | `COURSE_ALREADY_PUBLISHED` | Course đã được publish trước đó |

---

## 11. Voucher Pricing & Management

Tính năng voucher cho phép STAFF / SUPER_ADMIN tạo mã giảm giá; MEMBER (External / Internal) áp dụng voucher khi mua khóa học.

**Trọng tâm thiết kế — chống giả mạo (anti-tampering):**

| Mối lo | Cách giải quyết |
|--------|-----------------|
| Tampering `courseId` | Server đọc giá trực tiếp từ DB theo `courseId` ở path |
| Tampering `price` từ client | DTO chỉ khai báo `voucherCode`, mọi field giá bị bỏ qua |
| Replay quote cũ | Mỗi `/purchase` tính lại giá từ đầu, không tin quote |
| Race condition voucher quota | Pessimistic write lock trên hàng voucher + UNIQUE `(voucherId, enrollmentId)` |

**Phân quyền:**

| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Quản lý voucher (CRUD) | ❌ | ❌ | ✅ (`MANAGE_VOUCHER`) | ❌ | ✅ |
| Quote / áp voucher khi mua | ✅ (`USE_VOUCHER`) | ❌ | ❌ | ❌ | ✅ |

> Internal Member (`isInternal = true`): luôn `paidPrice = 0`, voucher bị bỏ qua khi mua. Voucher chỉ có ý nghĩa với External Member.

---

### 11.1. Tạo Voucher (Admin)

**Endpoint:** `POST /api/v1/admin/vouchers`

**Yêu cầu quyền:** `MANAGE_VOUCHER` (Role STAFF, SUPER_ADMIN).

**Request Body:**
```json
{
  "code": "WELCOME50",
  "type": "PERCENT",
  "value": 50,
  "scope": "ALL_COURSES",
  "validFrom": "2026-05-01T00:00:00",
  "validTo": "2026-12-31T23:59:59",
  "minOrderAmount": 100000,
  "maxDiscount": 200000,
  "usageLimit": 1000,
  "usagePerUser": 1,
  "applicableCourseIds": null
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| code | String | Yes | 4–32 ký tự, regex `^[A-Za-z0-9_-]+$`, lưu uppercase, UNIQUE |
| type | Enum | Yes | `PERCENT` hoặc `FIXED` |
| value | BigDecimal | Yes | > 0; nếu `PERCENT` thì 0 < value ≤ 100 |
| scope | Enum | Yes | `ALL_COURSES` hoặc `SPECIFIC_COURSES` |
| validFrom | LocalDateTime | Yes | ISO-8601 |
| validTo | LocalDateTime | Yes | ISO-8601, ≥ validFrom |
| minOrderAmount | BigDecimal | No | ≥ 0, mặc định 0 |
| maxDiscount | BigDecimal | No | ≥ 0, chỉ áp dụng khi `PERCENT`; 0 = không giới hạn |
| usageLimit | Long | No | ≥ 0; 0 = không giới hạn |
| usagePerUser | Integer | No | ≥ 0; 0 = không giới hạn |
| applicableCourseIds | Set\<Long\> | Tùy `scope` | Bắt buộc và không rỗng khi `scope = SPECIFIC_COURSES` |

**Response 201:**
```json
{
  "status": 201,
  "message": "Tạo voucher thành công",
  "data": {
    "id": 1,
    "code": "WELCOME50",
    "type": "PERCENT",
    "value": 50,
    "status": "ACTIVE",
    "scope": "ALL_COURSES",
    "validFrom": "2026-05-01T00:00:00",
    "validTo": "2026-12-31T23:59:59",
    "minOrderAmount": 100000,
    "maxDiscount": 200000,
    "usageLimit": 1000,
    "usagePerUser": 1,
    "applicableCourseIds": null,
    "usedCount": 0,
    "createdAt": "2026-05-24T10:00:00",
    "updatedAt": "2026-05-24T10:00:00"
  },
  "timestamp": "2026-05-24T10:00:00"
}
```

**Response 409 (Code đã tồn tại):**
```json
{
  "code": "VOUCHER_CODE_ALREADY_EXISTS",
  "message": "Voucher code WELCOME50 đã tồn tại.",
  "timestamp": "2026-05-24T10:00:00"
}
```

---

### 11.2. Cập nhật Voucher

**Endpoint:** `PUT /api/v1/admin/vouchers/{id}`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

> **Immutable fields** (`code`, `type`, `value`): có thể gửi để sửa, nhưng chỉ được chấp nhận khi voucher **chưa có bất kỳ `Voucher_Usage` nào** (`usedCount = 0`). Nếu đã có usage → 400 `VOUCHER_IMMUTABLE_FIELD`. Không gửi (để `null`) = giữ nguyên giá trị cũ.
>
> **Soft fields** (`status`, `scope`, `validFrom`, `validTo`, `minOrderAmount`, `maxDiscount`, `usageLimit`, `usagePerUser`, `applicableCourseIds`): luôn được sửa, bắt buộc gửi đầy đủ.

**Request Body:**
```json
{
  "code": "SUMMER50",
  "type": "PERCENT",
  "value": 50,
  "status": "ACTIVE",
  "scope": "ALL_COURSES",
  "validFrom": "2026-06-01T00:00:00",
  "validTo": "2026-12-31T23:59:59",
  "minOrderAmount": 100000,
  "maxDiscount": 200000,
  "usageLimit": 500,
  "usagePerUser": 1,
  "applicableCourseIds": null
}
```

| Field | Type | Required | Ghi chú |
|-------|------|----------|---------|
| code | String | No | Nullable — chỉ sửa được khi `usedCount = 0`; regex `^[A-Za-z0-9_-]{1,32}$` |
| type | Enum | No | Nullable — chỉ sửa được khi `usedCount = 0`; `PERCENT` hoặc `FIXED` |
| value | BigDecimal | No | Nullable — chỉ sửa được khi `usedCount = 0`; > 0 |
| status | Enum | **Yes** | `ACTIVE` hoặc `INACTIVE` |
| scope | Enum | **Yes** | `ALL_COURSES` hoặc `SPECIFIC_COURSES` |
| validFrom | LocalDateTime | **Yes** | ISO-8601 |
| validTo | LocalDateTime | **Yes** | ISO-8601, ≥ validFrom |
| minOrderAmount | BigDecimal | No | ≥ 0 |
| maxDiscount | BigDecimal | No | ≥ 0 |
| usageLimit | Long | No | ≥ 0; 0 = không giới hạn |
| usagePerUser | Integer | No | ≥ 0; 0 = không giới hạn |
| applicableCourseIds | Set\<Long\> | Tùy `scope` | Bắt buộc khi `scope = SPECIFIC_COURSES` |

**Response 200:** Trả về voucher đã cập nhật với `usedCount` và metadata mới.

**Response 400 — usageLimit nhỏ hơn usedCount:**
```json
{
  "code": "VOUCHER_USAGE_LIMIT_TOO_LOW",
  "message": "usageLimit mới (10) không được nhỏ hơn usedCount hiện tại (15).",
  "timestamp": "2026-05-24T11:00:00"
}
```

**Response 400 — sửa field bất biến khi đã có usage:**
```json
{
  "code": "VOUCHER_IMMUTABLE_FIELD",
  "message": "Voucher đã có lượt dùng, không thể thay đổi field: code",
  "timestamp": "2026-05-24T11:00:00"
}
```

**Response 409 — code mới đã tồn tại:**
```json
{
  "code": "VOUCHER_CODE_ALREADY_EXISTS",
  "message": "Voucher code SUMMER50 đã tồn tại.",
  "timestamp": "2026-05-24T11:00:00"
}
```

---

### 11.3. Soft-delete Voucher

**Endpoint:** `DELETE /api/v1/admin/vouchers/{id}`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

> Voucher bị set `status = INACTIVE` (soft-delete) để bảo toàn lịch sử `Voucher_Usage`. Voucher đã có usage tuyệt đối không được xóa cứng.

**Response 200:**
```json
{
  "status": 200,
  "message": "Voucher đã được vô hiệu hóa (soft-delete)",
  "timestamp": "2026-05-24T11:00:00"
}
```

---

### 11.4. Danh sách Voucher (Admin)

**Endpoint:** `GET /api/v1/admin/vouchers`

**Yêu cầu quyền:** `MANAGE_VOUCHER`.

**Query Parameters:** `page` (default 0), `size` (default 10).

**Response 200:** Phân trang trả về danh sách voucher kèm `usedCount`.

---

### 11.5. Quote (Preview giá)

**Endpoint:** `POST /api/v1/courses/{courseId}/quote`

**Yêu cầu quyền:** Đăng nhập. Role MEMBER hoặc SUPER_ADMIN nếu gửi `voucherCode` (`USE_VOUCHER` permission). Role khác mà gửi `voucherCode` → 403.

> Read-only, không tiêu thụ voucher. Có thể gọi nhiều lần.

**Request Body (tùy chọn):**
```json
{
  "voucherCode": "WELCOME50"
}
```

> **Anti-tampering:** Body CHỈ chấp nhận `voucherCode`. Mọi field giá khác bị bỏ qua.

**Response 200 — không voucher:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 0.00,
    "finalPrice": 500000.00,
    "voucherApplied": false,
    "voucherCode": null,
    "voucherType": null,
    "internalDiscount": false,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

**Response 200 — voucher hợp lệ:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 200000.00,
    "finalPrice": 300000.00,
    "voucherApplied": true,
    "voucherCode": "WELCOME50",
    "voucherType": "PERCENT",
    "internalDiscount": false,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

**Response 200 — Internal Member (luôn 0đ, voucher bị bỏ qua):**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "originalPrice": 500000.00,
    "discountAmount": 500000.00,
    "finalPrice": 0.00,
    "voucherApplied": false,
    "voucherCode": null,
    "voucherType": null,
    "internalDiscount": true,
    "quotedAt": "2026-05-24T11:30:00"
  },
  "timestamp": "2026-05-24T11:30:00"
}
```

> `quotedAt` chỉ mang tính thông tin. KHÔNG được dùng làm chứng cứ ràng buộc giá ở luồng checkout — server luôn tính lại tại thời điểm `/purchase`.

---

### 11.6. Checkout với Voucher

Đã được mô tả trong section 7.2 (Mua Khóa Học). Tóm tắt:

- Endpoint: `POST /api/v1/courses/{id}/purchase` với body `{ "voucherCode": "..." }`.
- Server giữ pessimistic lock trên User → Course → Voucher (thứ tự cố định, chống deadlock).
- Validate voucher LẦN NỮA sau khi giữ lock — preview pass không có nghĩa checkout pass.
- UNIQUE `(voucher_id, enrollment_id)` ở DB chống race tạo 2 usage cho cùng enrollment.
- Audit log `event = "VOUCHER_APPLIED"` được ghi vào `logs/purchase_ledger.jsonl`.

---

### Voucher Validation Rules

Validator kiểm tra theo thứ tự cố định (cùng đầu vào → cùng exception):

1. `status = ACTIVE`
2. `validFrom ≤ now`
3. `now ≤ validTo`
4. Scope (course nằm trong `applicableCourseIds` nếu `SPECIFIC_COURSES`)
5. `originalPrice ≥ minOrderAmount`
6. `usedCount < usageLimit` (toàn cục, 0 = không giới hạn)
7. `perUserCount < usagePerUser` (theo user, 0 = không giới hạn)

---

### Pricing Engine Rules

| Voucher Type | Công thức tính `discountAmount` |
|--------------|---------------------------------|
| `null` | 0 (không giảm) |
| `PERCENT` | `min(originalPrice × value / 100, maxDiscount)` (HALF_UP, scale 2) — `maxDiscount = 0` nghĩa không giới hạn |
| `FIXED` | `min(value, originalPrice)` |

Invariants luôn được enforce:
- `0 ≤ discountAmount ≤ originalPrice`
- `finalPrice = originalPrice − discountAmount`
- `0 ≤ finalPrice ≤ originalPrice`
- Mọi BigDecimal `scale = 2`, rounding `HALF_UP`
- `originalPrice = 0` → `discountAmount = 0`, `finalPrice = 0` (voucher vô nghĩa với khóa miễn phí)

---

### Mã lỗi Voucher

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `VOUCHER_NOT_FOUND` | Không tìm thấy voucher với code đã chuẩn hóa |
| 400 | `VOUCHER_INACTIVE` | Voucher đang ở trạng thái INACTIVE |
| 400 | `VOUCHER_NOT_YET_ACTIVE` | Chưa tới thời điểm `validFrom` |
| 400 | `VOUCHER_EXPIRED` | Đã quá `validTo` |
| 400 | `VOUCHER_NOT_APPLICABLE` | Course không nằm trong `applicableCourseIds` |
| 400 | `VOUCHER_MIN_ORDER_NOT_MET` | `originalPrice < minOrderAmount` |
| 409 | `VOUCHER_USAGE_LIMIT_REACHED` | Đã đạt giới hạn lượt dùng tổng |
| 409 | `VOUCHER_USAGE_PER_USER_EXCEEDED` | User đã đạt giới hạn lượt dùng cá nhân |
| 403 | `VOUCHER_USE_DENIED` | Role không được phép sử dụng voucher |
| 409 | `VOUCHER_CODE_ALREADY_EXISTS` | Code đã tồn tại khi tạo voucher |
| 400 | `VOUCHER_USAGE_LIMIT_TOO_LOW` | `usageLimit` mới nhỏ hơn `usedCount` hiện tại |
| 400 | `VOUCHER_IMMUTABLE_FIELD` | Sửa field bất biến (code/type/value) khi voucher đã có usage |

---

## 12. Tổng kết API Endpoints

### Auth
- `POST /api/v1/auth/register` - Đăng ký tài khoản
- `POST /api/v1/auth/login` - Đăng nhập
- `POST /api/v1/admin/users` - Admin tạo tài khoản

### Course Management
- `GET /api/v1/courses` - Danh sách khóa học (chỉ `published = true`)
- `GET /api/v1/courses/{id}` - Chi tiết khóa học
- `POST /api/v1/courses` - Tạo khóa học (mặc định ẩn, chờ duyệt)
- `PUT /api/v1/courses/{id}` - Cập nhật khóa học
- `DELETE /api/v1/courses/{id}` - Xóa khóa học
- `POST /api/v1/courses/{id}/purchase` - Mua khóa học (tùy chọn `voucherCode`)
- `POST /api/v1/courses/{id}/quote` - Quote giá (preview với voucher, read-only)

### Course Approval (Admin / Instructor)
- `GET /api/v1/instructor/courses` - Course của instructor (cả pending)
- `GET /api/v1/instructor/courses/{id}` - Chi tiết course của instructor
- `GET /api/v1/admin/courses/pending` - Danh sách course chờ duyệt
- `GET /api/v1/admin/courses` - Toàn bộ course (cả publish và pending)
- `POST /api/v1/admin/courses/{id}/publish` - Duyệt và công khai course
- `POST /api/v1/admin/courses/{id}/unpublish` - Ẩn course đã publish
- `PUT /api/v1/admin/courses/{id}/price` - Sửa giá course (kể cả khi `priceLocked`)

### Wallet
- `POST /api/v1/users/me/top-up` - Nạp tiền vào ví

### Section Management
- `GET /api/v1/courses/{courseId}/sections` - Danh sách sections
- `POST /api/v1/courses/{courseId}/sections` - Tạo section
- `PUT /api/v1/courses/{courseId}/sections/{sectionId}` - Cập nhật section
- `DELETE /api/v1/courses/{courseId}/sections/{sectionId}` - Xóa section

### Lesson Management
- `GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons` - Danh sách lessons
- `POST /api/v1/courses/{courseId}/sections/{sectionId}/lessons` - Tạo lesson
- `PUT /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` - Cập nhật lesson
- `DELETE /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` - Xóa lesson

### Voucher Management (Admin)
- `POST /api/v1/admin/vouchers` - Tạo voucher
- `GET /api/v1/admin/vouchers` - Danh sách voucher (phân trang)
- `PUT /api/v1/admin/vouchers/{id}` - Cập nhật voucher
- `DELETE /api/v1/admin/vouchers/{id}` - Soft-delete voucher

---

## 13. Testing với Postman/Insomnia

### Authentication Flow:
1. **Register** → Lấy thông tin user (không có token)
2. **Login** → Lấy JWT token
3. **Gửi token** trong header `Authorization: Bearer <token>` cho các API protected

### Example Headers:
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNRU0yQjRBMUQiLCJ1c2VySWQiOjEsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInJvbGUiOiJNRU1CRVIiLCJpc0ludGVybmFsIjpmYWxzZSwiaWF0IjoxNzE1Nzg4MjAwLCJleHAiOjE3MTU4NzQ2MDB9.xxxx
```

### Testing Scenarios:
1. **MEMBER (External)**: Xem courses public, nạp tiền, quote giá với voucher, mua khóa học (có / không voucher).
2. **MEMBER (Internal)**: Mua khóa học luôn 0đ; voucher bị bỏ qua; quote luôn `internalDiscount = true`.
3. **INSTRUCTOR**: Tạo course (mặc định ẩn) → thêm sections / lessons → chờ admin duyệt. Sau publish, không sửa được giá. Xem course của mình qua `/api/v1/instructor/courses`.
4. **STAFF**: Toàn quyền course / section / lesson, duyệt course (`PUBLISH_COURSE`), khóa giá (`LOCK_COURSE_PRICE`), quản lý voucher (`MANAGE_VOUCHER`).
5. **ADMIN_USER**: Quản lý user, course (CRUD) nhưng KHÔNG có quyền section / lesson, KHÔNG duyệt course, KHÔNG quản lý voucher.
6. **SUPER_ADMIN**: Toàn quyền hệ thống bao gồm `USE_VOUCHER` (mua test) và `MANAGE_VOUCHER`.

### Voucher / Course Approval flow gợi ý test:
1. STAFF login → `POST /api/v1/admin/vouchers` tạo voucher `WELCOME50`.
2. INSTRUCTOR login → `POST /api/v1/courses` tạo course (course ở DRAFT, không xuất hiện ở public list).
3. STAFF → `GET /api/v1/admin/courses/pending` thấy course chờ duyệt.
4. STAFF → `POST /api/v1/admin/courses/{id}/publish` để công khai. Course xuất hiện ở `GET /api/v1/courses`.
5. MEMBER login → `POST /api/v1/courses/{id}/quote` với `{ "voucherCode": "WELCOME50" }` xem giá preview.
6. MEMBER → `POST /api/v1/courses/{id}/purchase` với `{ "voucherCode": "WELCOME50" }` để mua thực sự. Server tính lại giá độc lập, ghi `Voucher_Usage` và audit log.
7. Kiểm tra `logs/purchase_ledger.jsonl` thấy 2 dòng JSONL: `PURCHASE_COMPLETED` (hoặc `VOUCHER_APPLIED`).