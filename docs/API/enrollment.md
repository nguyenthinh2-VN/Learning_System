# My Enrollments & Lesson Access Control

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 15. My Enrollments & Lesson Access Control

### 15.1. Lấy danh sách khóa học đã mua

**Endpoint:** `GET /api/v1/users/me/enrollments`

**Yêu cầu quyền:** Đăng nhập (mọi role). Trả về enrollment của chính người gọi.

**Query Parameters:**
| Field | Type | Default | Validation |
|-------|------|---------|------------|
| page | Integer | 0 | >= 0 |
| size | Integer | 20 | [1, 100] |

Sắp xếp theo `enrolledAt DESC`.

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 2,
    "totalPages": 1,
    "page": 0,
    "size": 20,
    "items": [
      {
        "enrollmentId": 5,
        "courseId": 10,
        "paidPrice": 500000.00,
        "enrolledAt": "2026-05-26T10:00:00"
      },
      {
        "enrollmentId": 3,
        "courseId": 7,
        "paidPrice": 0.00,
        "enrolledAt": "2026-05-20T09:00:00"
      }
    ]
  },
  "timestamp": "2026-05-26T12:00:00"
}
```

Trả về trang rỗng (không phải 403) nếu chưa có enrollment nào.

**Response 400 (page/size không hợp lệ):**
```json
{ "code": "BAD_REQUEST", "message": "size phải trong khoảng [1, 100]" }
```

> **Lưu ý:** Response chỉ trả `courseId`. FE muốn hiển thị title/thumbnail thì join qua `GET /api/v1/courses/{courseId}`.

---

### 15.2. Kiểm soát truy cập Lesson (cập nhật)

**Endpoint:** `GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons`

Quy tắc phân quyền **đã được siết chặt** — không còn cho phép mọi user đăng nhập xem tự do:

| Role | Quy tắc |
|------|---------|
| `SUPER_ADMIN` | Luôn được phép |
| `STAFF` | Luôn được phép |
| `INSTRUCTOR` | Chỉ được phép nếu **sở hữu** khóa học (`instructorId == requesterId`) |
| `MEMBER` | Chỉ được phép nếu **đã enrolled** (`enrollments` có row cho courseId này) |
| `ADMIN_USER` | **Từ chối 403** — không thuộc luồng học tập |

**Response 403 — MEMBER chưa enrolled:**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học để xem bài giảng.",
  "timestamp": "2026-05-26T12:00:00"
}
```

**Response 403 — INSTRUCTOR không phải chủ sở hữu:**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Giảng viên chỉ có quyền xem bài giảng trong khóa học do chính mình tạo.",
  "timestamp": "2026-05-26T12:00:00"
}
```

> **Lưu ý thiết kế:** Khóa học giá 0đ vẫn yêu cầu enrollment row. `ApplyVoucherCheckoutUseCase` đã tạo enrollment cho giao dịch 0đ (internal member). Không có ngoại lệ "khóa học miễn phí".
