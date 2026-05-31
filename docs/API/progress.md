# Lesson Progress (Theo dõi tiến độ học)

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

Cho phép học viên đánh dấu lesson đã học xong và xem % hoàn thành khóa học.

**Phân quyền (ai được thao tác tiến độ của một khóa học):**
| Role | Quy tắc |
|------|---------|
| `MEMBER` | Chỉ khi **đã enrolled** khóa học |
| `INSTRUCTOR` | Chỉ khi **sở hữu** khóa học |
| `STAFF`, `SUPER_ADMIN` | Luôn được phép |
| `ADMIN_USER` | **Từ chối 403** (không thuộc luồng học tập) |

> Tiến độ luôn thuộc về chính người gọi (xác định qua `userId` trong JWT). Không xem/sửa được tiến độ của người khác.

---

## 1. Đánh dấu lesson đã hoàn thành

**Endpoint:** `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete`

**Yêu cầu quyền:** Đăng nhập + thỏa bảng phân quyền ở trên.

> **Idempotent:** Đánh dấu một lesson đã hoàn thành rồi → không tạo bản ghi trùng, vẫn trả 200.

**Response 200:**
```json
{
  "status": 200,
  "message": "Đã đánh dấu hoàn thành",
  "data": { "lessonId": 3, "completed": true },
  "timestamp": "2026-05-30T10:00:00"
}
```

**Response 404 — Lesson không thuộc course / không tồn tại:**
```json
{
  "code": "LESSON_NOT_FOUND",
  "message": "Không tìm thấy bài giảng với id: 999",
  "timestamp": "2026-05-30T10:00:00"
}
```

**Response 403 — MEMBER chưa enrolled:**
```json
{
  "code": "LESSON_ACCESS_DENIED",
  "message": "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học để xem bài giảng.",
  "timestamp": "2026-05-30T10:00:00"
}
```

---

## 2. Bỏ đánh dấu hoàn thành (học lại)

**Endpoint:** `DELETE /api/v1/courses/{courseId}/lessons/{lessonId}/complete`

**Yêu cầu quyền:** Như trên.

> **No-op an toàn:** Bỏ đánh dấu một lesson chưa từng đánh dấu → vẫn trả 200, không lỗi.

**Response 200:**
```json
{
  "status": 200,
  "message": "Đã bỏ đánh dấu",
  "data": { "lessonId": 3, "completed": false },
  "timestamp": "2026-05-30T10:00:00"
}
```

---

## 3. Tiến độ tổng quan của khóa học

**Endpoint:** `GET /api/v1/courses/{courseId}/progress`

**Yêu cầu quyền:** Như trên.

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "courseId": 7,
    "totalLessons": 4,
    "completedLessons": 3,
    "progressPercent": 75,
    "completedLessonIds": [1, 2, 3]
  },
  "timestamp": "2026-05-30T10:00:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| courseId | Long | ID khóa học |
| totalLessons | Integer | Tổng số lesson hiện có của khóa |
| completedLessons | Integer | Số lesson người gọi đã hoàn thành |
| progressPercent | Integer | `round(completedLessons / totalLessons × 100)`; `0` nếu khóa chưa có lesson |
| completedLessonIds | Long[] | Danh sách `lessonId` đã hoàn thành (FE dùng để tick ✓) |

**Response 404 (Course không tồn tại):**
```json
{
  "code": "COURSE_NOT_FOUND",
  "message": "Course not found with id: 999",
  "timestamp": "2026-05-30T10:00:00"
}
```

---

### Mã lỗi (Lesson Progress)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `COURSE_NOT_FOUND` | Không tìm thấy khóa học |
| 404 | `LESSON_NOT_FOUND` | Lesson không thuộc khóa học / không tồn tại |
| 403 | `LESSON_ACCESS_DENIED` | Không có quyền (chưa enrolled / không sở hữu / ADMIN_USER) |
