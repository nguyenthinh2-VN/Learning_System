# API Documentation — Learning System

Base URL: `http://localhost:8080/api/v1`

Tài liệu API được tách theo từng mục để dễ tra cứu và tránh quá nhiều context trong một file. Xem từng file con bên dưới.

---

## Mục lục

| # | File | Nội dung |
|---|------|----------|
| 1 | [auth.md](./auth.md) | Đăng ký, đăng nhập, JWT token |
| 2 | [roles-permissions.md](./roles-permissions.md) | Roles & Permissions, Database Schema |
| 3 | [course.md](./course.md) | Course Management (CRUD), thumbnailUrl |
| 4 | [course-approval.md](./course-approval.md) | Duyệt course, publish/unpublish, Instructor Portal |
| 5 | [section.md](./section.md) | Course Section Management |
| 6 | [lesson.md](./lesson.md) | Course Lesson Management |
| 7 | [wallet.md](./wallet.md) | Wallet, mua khóa học, top-up, WebSocket |
| 8 | [voucher.md](./voucher.md) | Voucher Pricing & Management |
| 9 | [enrollment.md](./enrollment.md) | My Enrollments & Lesson Access Control |
| 10 | [user.md](./user.md) | User Profile, Admin User Management |
| 11 | [testing.md](./testing.md) | Testing với Postman/Insomnia |
| 12 | [endpoints-summary.md](./endpoints-summary.md) | Tổng kết toàn bộ endpoints |

---

## Error Response Format (chung)

Tất cả lỗi trả về format thống nhất:

```json
{
  "code": "ERROR_CODE",
  "message": "Mô tả lỗi bằng tiếng Việt",
  "timestamp": "2026-05-15T15:30:00"
}
```

### Mã lỗi chung

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ |
| 400 | `INVALID_EMAIL` | Email sai định dạng |
| 400 | `INVALID_PASSWORD` | Mật khẩu hiện tại không đúng (đổi mật khẩu) |
| 400 | `INVALID_FILE_TYPE` | Định dạng file upload không được hỗ trợ |
| 400 | `FILE_TOO_LARGE` | File upload vượt quá kích thước cho phép |
| 401 | `INVALID_CREDENTIALS` | Email hoặc mật khẩu sai |
| 404 | `USER_NOT_FOUND` | Không tìm thấy user |
| 409 | `EMAIL_ALREADY_EXISTS` | Email đã được đăng ký |
| 500 | `INTERNAL_ERROR` | Lỗi hệ thống |

> Mã lỗi theo từng domain (Section, Lesson, Course Approval, Voucher) được liệt kê ở cuối mỗi file con tương ứng.
