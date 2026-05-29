# Course Section Management

[← Quay lại mục lục](./README.md)

Base path: `/api/v1/courses/{courseId}/sections`

---

## 8. Course Section Management

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

### Mã lỗi (Section)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `SECTION_NOT_FOUND` | Không tìm thấy chương học |
| 403 | `SECTION_ACCESS_DENIED` | Không có quyền thao tác chương học |
