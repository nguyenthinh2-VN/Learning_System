# Course Approval, Visibility & Instructor Portal

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

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

## 19. Instructor Portal

INSTRUCTOR có thể đăng nhập vào Admin Portal với quyền hạn chế theo permission-matrix.

### Quyền của INSTRUCTOR trong Admin Portal

| Tính năng | Được phép |
|-----------|-----------|
| Xem dashboard | ✅ |
| Xem khóa học của mình | ✅ (`GET /api/v1/instructor/courses`) |
| Tạo khóa học mới | ✅ (`POST /api/v1/courses`) |
| Sửa khóa học của mình | ✅ (`PUT /api/v1/courses/{id}`) |
| Xóa khóa học của mình | ✅ (`DELETE /api/v1/courses/{id}`) |
| Quản lý Section/Lesson | ✅ (chỉ course của mình) |
| Duyệt/ẩn course | ❌ (cần STAFF/SUPER_ADMIN) |
| Sửa giá sau publish | ❌ (cần STAFF/SUPER_ADMIN) |
| Xem danh sách users | ❌ |
| Quản lý voucher | ❌ |
| Cộng tiền | ❌ |

### 19.1. Danh sách course của Instructor

**Endpoint:** `GET /api/v1/instructor/courses`

**Yêu cầu quyền:** Role `INSTRUCTOR`

**Query Parameters:** `keyword`, `page`, `size`

**Response 200:** Tương tự `GET /api/v1/admin/courses` nhưng chỉ trả về course của instructor đang đăng nhập (cả published và draft).

---

### 19.2. Chi tiết course của Instructor

**Endpoint:** `GET /api/v1/instructor/courses/{id}`

**Yêu cầu quyền:** Role `INSTRUCTOR` + sở hữu course.

---

### Mã lỗi (Course Approval)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `COURSE_NOT_PUBLISHED` | Khóa học chưa được duyệt và công khai |
| 400 | `COURSE_PRICE_LOCKED` | Giá khóa học đã bị khóa, không thể sửa |
| 400 | `COURSE_ALREADY_PUBLISHED` | Course đã được publish trước đó |
