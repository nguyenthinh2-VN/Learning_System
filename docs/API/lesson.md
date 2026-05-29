# Course Lesson Management

[← Quay lại mục lục](./README.md)

Base path: `/api/v1/courses/{courseId}/sections/{sectionId}/lessons`

> **Lưu ý:** Quy tắc truy cập khi **xem** lessons đã được siết chặt — xem [enrollment.md](./enrollment.md) (mục 15.2 Kiểm soát truy cập Lesson).

---

## 9. Course Lesson Management

**Phân quyền (thao tác CRUD):**
| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Xem lessons | ✅* | ✅* | ✅ | ✅** | ✅ |
| Tạo lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |
| Sửa lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |
| Xóa lesson | ❌ | ✅ (section của course mình) | ✅ | ❌ | ✅ |

> \* Quyền **xem** lessons đã được siết: MEMBER phải đã enrolled, INSTRUCTOR phải sở hữu course. \*\* ADMIN_USER bị từ chối 403 ở luồng xem lesson. Chi tiết tại [enrollment.md](./enrollment.md) mục 15.2.
>
> **Lưu ý:** `ADMIN_USER` không có quyền thao tác Lesson (giống với Section-level).

---

### 9.1. Lấy danh sách Lessons của một Section

**Endpoint:** `GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons`

**Yêu cầu quyền:** Xem mục 15.2 ([enrollment.md](./enrollment.md)) — kiểm soát truy cập theo role và enrollment.

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

**Response 404 (Course / Section không tồn tại):** `COURSE_NOT_FOUND` / `SECTION_NOT_FOUND` (tương tự mục 9.1).

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

**Response 403 (Không có quyền):**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền sửa/xóa bài giảng trong chương học thuộc khóa học do chính mình tạo.",
  "timestamp": "2026-05-23T10:00:00"
}
```

> Cũng có thể trả `COURSE_NOT_FOUND` / `SECTION_NOT_FOUND` (404) nếu course/section không tồn tại.

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

**Response 403 (Không có quyền):**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Bạn không có quyền sửa/xóa bài giảng.",
  "timestamp": "2026-05-23T10:00:00"
}
```

> Cũng có thể trả `COURSE_NOT_FOUND` / `SECTION_NOT_FOUND` (404) nếu course/section không tồn tại.

---

### Mã lỗi (Lesson)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `LESSON_NOT_FOUND` | Không tìm thấy bài giảng |
| 403 | `LESSON_ACCESS_DENIED` | Không có quyền thao tác bài giảng |
