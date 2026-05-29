# Tổng kết API Endpoints

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 12. Tổng kết API Endpoints

### Auth — [auth.md](./auth.md)
- `POST /api/v1/auth/register` - Đăng ký tài khoản
- `POST /api/v1/auth/login` - Đăng nhập
- `POST /api/v1/admin/users` - Admin tạo tài khoản

### Course Management — [course.md](./course.md)
- `GET /api/v1/courses` - Danh sách khóa học (chỉ `published = true`)
- `GET /api/v1/courses/{id}` - Chi tiết khóa học
- `POST /api/v1/courses` - Tạo khóa học (mặc định ẩn, chờ duyệt)
- `PUT /api/v1/courses/{id}` - Cập nhật khóa học
- `DELETE /api/v1/courses/{id}` - Xóa khóa học
- `POST /api/v1/courses/{id}/purchase` - Mua khóa học (tùy chọn `voucherCode`)
- `POST /api/v1/courses/{id}/quote` - Quote giá (preview với voucher, read-only)

### Course Approval (Admin / Instructor) — [course-approval.md](./course-approval.md)
- `GET /api/v1/instructor/courses` - Course của instructor (cả pending)
- `GET /api/v1/instructor/courses/{id}` - Chi tiết course của instructor
- `GET /api/v1/admin/courses/pending` - Danh sách course chờ duyệt
- `GET /api/v1/admin/courses` - Toàn bộ course (cả publish và pending)
- `POST /api/v1/admin/courses/{id}/publish` - Duyệt và công khai course
- `POST /api/v1/admin/courses/{id}/unpublish` - Ẩn course đã publish
- `PUT /api/v1/admin/courses/{id}/price` - Sửa giá course (kể cả khi `priceLocked`)

### Wallet — [wallet.md](./wallet.md)
- `POST /api/v1/users/me/top-up` - Nạp tiền vào ví (legacy)
- `POST /api/v1/wallet/top-up/init` - Khởi tạo nạp tiền (QR / mock)
- `POST /api/v1/webhook/mock?ref={referenceCode}` - Mock webhook (chỉ dev)
- `POST /api/v1/admin/users/{userId}/top-up` - Admin cộng tiền thủ công
- `GET /api/v1/users/me/transactions` - Lịch sử giao dịch ví (nạp + mua, phân trang)
- `ws://localhost:8080/ws` - WebSocket nhận event `WALLET_UPDATED`

### Section Management — [section.md](./section.md)
- `GET /api/v1/courses/{courseId}/sections` - Danh sách sections
- `POST /api/v1/courses/{courseId}/sections` - Tạo section
- `PUT /api/v1/courses/{courseId}/sections/{sectionId}` - Cập nhật section
- `DELETE /api/v1/courses/{courseId}/sections/{sectionId}` - Xóa section

### Lesson Management — [lesson.md](./lesson.md)
- `GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons` - Danh sách lessons
- `POST /api/v1/courses/{courseId}/sections/{sectionId}/lessons` - Tạo lesson
- `PUT /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` - Cập nhật lesson
- `DELETE /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` - Xóa lesson

### Voucher Management (Admin) — [voucher.md](./voucher.md)
- `POST /api/v1/admin/vouchers` - Tạo voucher
- `GET /api/v1/admin/vouchers` - Danh sách voucher (phân trang)
- `PUT /api/v1/admin/vouchers/{id}` - Cập nhật voucher
- `DELETE /api/v1/admin/vouchers/{id}` - Soft-delete voucher

### Enrollment — [enrollment.md](./enrollment.md)
- `GET /api/v1/users/me/enrollments` - Danh sách khóa học đã mua (phân trang)

### User Profile & Admin User Management — [user.md](./user.md)
- `GET /api/v1/users/me/profile` - Thông tin cá nhân + số dư ví
- `PUT /api/v1/users/me/profile` - Cập nhật `name` / `avatarUrl`
- `PUT /api/v1/users/me/password` - Đổi mật khẩu
- `POST /api/v1/users/me/avatar` - Upload ảnh đại diện (multipart, JPEG/PNG/WebP, ≤ 2MB)
- `GET /api/v1/admin/users` - Danh sách users (phân trang, tìm kiếm)

---

## Tổng kết endpoints mới (cập nhật 2026-05-28)

### Admin User Management
- `GET /api/v1/admin/users` — Danh sách users (phân trang, tìm kiếm) — `ADMIN_USER`, `SUPER_ADMIN`

### Course thumbnailUrl
- Tất cả Course endpoints đã hỗ trợ field `thumbnailUrl` (nullable)

### Instructor Portal
- `GET /api/v1/instructor/courses` — Course của instructor (cả draft)
- `GET /api/v1/instructor/courses/{id}` — Chi tiết course của instructor

### Admin Course Management
- `GET /api/v1/admin/courses` — Tất cả courses — `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`
- `GET /api/v1/admin/courses/pending` — Courses chờ duyệt — `STAFF`, `SUPER_ADMIN`
- `POST /api/v1/admin/courses/{id}/publish` — Duyệt course — `STAFF`, `SUPER_ADMIN`
- `POST /api/v1/admin/courses/{id}/unpublish` — Ẩn course — `STAFF`, `SUPER_ADMIN`
- `PUT /api/v1/admin/courses/{id}/price` — Sửa giá (kể cả priceLocked) — `STAFF`, `SUPER_ADMIN`

### Admin Section/Lesson Management
- Tất cả Section/Lesson endpoints đã mô tả tại [section.md](./section.md) và [lesson.md](./lesson.md)
- INSTRUCTOR chỉ thao tác được trên course của mình
- ADMIN_USER **không có quyền** thao tác Section/Lesson
