# Course Management

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

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

> **Workflow duyệt:** Course tạo ra mặc định `published = false`, `priceLocked = false`, KHÔNG xuất hiện ở public listing. STAFF / SUPER_ADMIN cần duyệt qua `POST /api/v1/admin/courses/{id}/publish` để công khai. Khi publish, server tự động set `priceLocked = true`, INSTRUCTOR không sửa được giá nữa (xem [course-approval.md](./course-approval.md)).

**Request Body:**
```json
{
  "title": "Spring Boot Clean Architecture",
  "description": "Khóa học chi tiết",
  "maxStudents": 100,
  "price": 500000.00,
  "thumbnailUrl": "https://example.com/images/spring-boot.jpg",
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
    "thumbnailUrl": "https://example.com/images/spring-boot.jpg",
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

## 18. Course thumbnailUrl

Field `thumbnailUrl` được thêm vào Course để hiển thị ảnh bìa khóa học.

### Thay đổi schema

**Bảng `courses`** — thêm cột:
```sql
ALTER TABLE courses ADD COLUMN thumbnail_url VARCHAR(500) NULL;
```

### Thay đổi API

Field `thumbnailUrl` xuất hiện trong tất cả response liên quan đến Course:

**`GET /api/v1/courses`** — mỗi item trong `items[]`:
```json
{
  "id": 1,
  "title": "Spring Boot Clean Architecture",
  "thumbnailUrl": "https://example.com/images/spring-boot.jpg",
  ...
}
```

**`GET /api/v1/courses/{id}`** — trong `data`:
```json
{
  "id": 1,
  "thumbnailUrl": "https://example.com/images/spring-boot.jpg",
  ...
}
```

**`POST /api/v1/courses`** — request body (tùy chọn):
```json
{
  "title": "...",
  "thumbnailUrl": "https://example.com/images/course.jpg",
  ...
}
```

**`PUT /api/v1/courses/{id}`** — request body (tùy chọn, null = giữ nguyên):
```json
{
  "title": "...",
  "thumbnailUrl": "https://example.com/images/course-updated.jpg",
  ...
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| thumbnailUrl | String | No | URL ảnh (JPEG, PNG, WebP...). Tối đa 500 ký tự. `null` = không có ảnh |

> **Lưu ý FE:** Khi `thumbnailUrl = null`, hiển thị placeholder icon. Khi có URL, hiển thị `<img>` với fallback về placeholder nếu ảnh lỗi.

---

### Mã lỗi (Course)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `COURSE_NOT_FOUND` | Không tìm thấy khóa học |
