# Ma trận phân quyền (RBAC Permission Matrix)

Dưới đây là bảng ma trận phân quyền. Đánh dấu `[x]` vào ô tương ứng để gán quyền cho Role.
Sau khi điền xong, hệ thống sẽ seed dữ liệu vào bảng `role_permissions` dựa trên ma trận này.

## Hướng dẫn

Danh sách các vai trò (Roles) trong hệ thống:
- **MEMBER**: Người dùng mặc định khi đăng ký (Học viên). Lưu ý: Member phân thành 2 loại (Nội bộ / Bên ngoài) được đánh dấu qua cờ `is_internal` ở bảng User.
- **INSTRUCTOR** (Giảng viên): Phụ trách tạo mới, chỉnh sửa, xóa khóa học và upload nội dung (video, bài giảng).
- **STAFF**: Nhân viên / Trợ lý quản lý nội dung.
- **ADMIN_USER**: Quản lý tài khoản, phụ trách cấp mới tài khoản cho MEMBER (nội bộ/bên ngoài).
- **SUPER_ADMIN**: Quản trị viên hệ thống (toàn quyền).

## Bảng phân quyền

| # | Permission | Mô tả | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|---|-----------|-------|--------|------------|-------|------------|-------------|
| 1 | `VIEW_COURSE` | Xem danh sách và chi tiết khóa học | [x] | [x] | [x] | [x] | [x] |
| 2 | `ENROLL_COURSE` | Đăng ký tham gia khóa học | [ ] | [ ] | [ ] | [ ] | [x] |
| 3 | `CREATE_COURSE` | Tạo khóa học mới (mặc định ẩn, chờ duyệt) | [ ] | [x] | [x] | [x] | [x] |
| 4 | `EDIT_COURSE` | Chỉnh sửa thông tin khóa học | [ ] | [x] | [x] | [x] | [x] |
| 5 | `DELETE_COURSE` | Xóa khóa học | [ ] | [x] | [x] | [x] | [x] |
| 6 | `CREATE_SECTION` | Tạo chương học trong khóa học | [ ] | [x] | [x] | [ ] | [x] |
| 7 | `EDIT_SECTION` | Sửa / Xóa chương học | [ ] | [x] | [x] | [ ] | [x] |
| 8 | `CREATE_LESSON` | Tạo bài giảng trong chương học | [ ] | [x] | [x] | [ ] | [x] |
| 9 | `EDIT_LESSON` | Sửa / Xóa bài giảng | [ ] | [x] | [x] | [ ] | [x] |
| 10 | `VIEW_USER` | Xem thông tin người dùng | [ ] | [ ] | [ ] | [x] | [x] |
| 11 | `CREATE_USER` | Cấp tài khoản mới (Nội bộ/Ngoài) | [ ] | [ ] | [ ] | [x] | [x] |
| 12 | `EDIT_USER` | Chỉnh sửa thông tin người dùng | [ ] | [ ] | [ ] | [x] | [x] |
| 13 | `DELETE_USER` | Xóa người dùng | [ ] | [ ] | [ ] | [ ] | [x] |
| 14 | `MANAGE_ROLE` | Quản lý vai trò và phân quyền | [ ] | [ ] | [ ] | [ ] | [x] |
| 15 | `VIEW_REPORT` | Xem báo cáo, thống kê | [ ] | [x] | [ ] | [ ] | [x] |
| 16 | `PUBLISH_COURSE` | Duyệt và publish khóa học (set published = true, lock giá) | [ ] | [ ] | [x] | [ ] | [x] |
| 17 | `LOCK_COURSE_PRICE` | Sửa giá khóa học (kể cả khi đã priceLocked) | [ ] | [ ] | [x] | [ ] | [x] |
| 18 | `MANAGE_VOUCHER` | Tạo / sửa / xóa / xem voucher trong panel admin | [ ] | [ ] | [x] | [ ] | [x] |
| 19 | `USE_VOUCHER` | Áp dụng voucher khi quote / mua khóa học | [x] | [ ] | [ ] | [ ] | [x] |
| 20 | `VIEW_LESSON` 🆕 | Xem nội dung bài giảng đã trả phí (`GET .../lessons`) | [x] | [x] | [x] | [ ] | [x] |
| 21 | `TRACK_PROGRESS` 🆕 | Đánh dấu hoàn thành & xem tiến độ học (`.../complete`, `.../progress`) | [x] | [x] | [x] | [ ] | [x] |
| 22 | `VIEW_TRANSACTION` 🆕 | Xem giao dịch toàn hệ thống trong admin panel | [ ] | [ ] | [ ] | [ ] | [x] |
| 23 | `VIEW_REVENUE` 🆕 | Xem báo cáo doanh thu (admin) | [ ] | [ ] | [ ] | [ ] | [x] |
| 24 | `MANAGE_WALLET` 🆕 | Admin cộng tiền thủ công vào ví user | [ ] | [ ] | [ ] | [ ] | [x] |
| 25 | `MANAGE_DEPARTMENT` 🆕 | Quản lý phòng ban | [ ] | [ ] | [ ] | [x] | [x] |

> Lưu ý phân quyền:
> - `CREATE_USER` (#11): nay **đã được seed** trong `DataInitializer` và gán cho ADMIN_USER + SUPER_ADMIN. Tuy nhiên endpoint tạo tài khoản `POST /api/v1/admin/users` vẫn enforce bằng **role gate** `@PreAuthorize("hasAnyRole('ADMIN_USER','SUPER_ADMIN','STAFF')")` chứ chưa kiểm tra permission này. Vì là role-gate nên thực tế **STAFF vẫn tạo được tài khoản** (rộng hơn cột `CREATE_USER`). Khi chuyển sang enforce theo permission động, cần đồng bộ lại gate cho khớp.
> - Hệ thống hiện **chưa enforce authorization qua từng permission** (`hasAuthority(...)`); phân quyền thực tế chạy bằng `@PreAuthorize("hasAnyRole(...)")` + domain policy (`CourseOwnershipPolicy`, `LessonAuthorizationService`, ...). Bảng `permissions` + `role_permissions` đã seed sẵn để dành cho phân quyền động (ADMIN tự gán permission cho role) sẽ enforce sau này.
> - 🆕 **Phân quyền động (Dynamic RBAC) đã được triển khai** (xem `docs/plan/plan-dynamic-permission.md`): `MANAGE_ROLE` cho phép SUPER_ADMIN gán/gỡ permission cho role qua `PUT /api/v1/admin/roles/{roleName}/permissions`. `JwtAuthenticationFilter` nạp permission động từ `PermissionCacheService` (cache theo role, reload tức thì khi đổi ma trận) và set authorities gồm **cả** `ROLE_<role>` lẫn từng permission. Nhóm endpoint `/api/v1/admin/permissions` + `/api/v1/admin/roles/**` đã enforce bằng `hasAuthority('MANAGE_ROLE')`. Các endpoint khác đang migrate dần sang `hasAuthority(...)` (vẫn giữ role-gate song song để không vỡ). `SUPER_ADMIN` luôn full quyền và không sửa được ma trận (chống tự khóa).
> - `INSTRUCTOR` không có `PUBLISH_COURSE` — phải nhờ STAFF/SUPER_ADMIN duyệt course mới publish được.
> - `INSTRUCTOR` cũng không có `LOCK_COURSE_PRICE` — sau khi course được publish, giá bị khóa, INSTRUCTOR phải nhờ admin sửa giá.
> - Chỉ `MEMBER` (External / Internal) và `SUPER_ADMIN` có `USE_VOUCHER`. INSTRUCTOR / STAFF / ADMIN_USER không có vì họ không phải đối tượng mua khóa học.
> - `MEMBER` Internal (`is_internal = TRUE`) được mua khóa học với giá 0đ, voucher bị bỏ qua khi mua. Chỉ External Member dùng voucher mới có ý nghĩa.
> - 🆕 `VIEW_LESSON` (#20): gate role là `hasAnyRole('MEMBER','INSTRUCTOR','STAFF','ADMIN_USER','SUPER_ADMIN')` nhưng domain policy (`LessonAuthorizationService`) siết lại: SUPER_ADMIN/STAFF luôn xem được, INSTRUCTOR chỉ xem course của mình, MEMBER chỉ xem khi đã enrolled, **ADMIN_USER bị từ chối 403**. Vì vậy ma trận đánh dấu ADMIN_USER `[ ]`.
> - 🆕 `TRACK_PROGRESS` (#21): áp dụng cho `POST/DELETE .../lessons/{id}/complete` và `GET .../progress`. Cùng quy tắc với `VIEW_LESSON` (ADMIN_USER không thuộc luồng học tập → 403); ngoài ra MEMBER phải enrolled, INSTRUCTOR phải sở hữu course.
> - 🆕 `VIEW_TRANSACTION` (#22), `VIEW_REVENUE` (#23), `MANAGE_WALLET` (#24): các chức năng tài chính nhạy cảm trong admin panel, hiện chỉ `SUPER_ADMIN` (`@PreAuthorize("hasRole('SUPER_ADMIN')")`). Tách riêng khỏi `VIEW_REPORT` (vốn được gán cho cả INSTRUCTOR) vì dữ liệu doanh thu/giao dịch toàn hệ thống rộng hơn report của instructor.
> - 🆕 `MANAGE_DEPARTMENT` (#25): Quản lý sơ đồ phòng ban (tạo/sửa/xóa). Được gán cho `ADMIN_USER` và `SUPER_ADMIN`.
> - 🆕 Các permission #20–#25 (cùng với `CREATE_USER` #11) **đã được seed trong DB** và gán cho role theo đúng ma trận này (`DataInitializer` nay seed 25 permissions, idempotent — DB cũ tự bổ sung khi khởi động lại). Hiện endpoint vẫn enforce bằng role-gate; các permission này sẵn sàng cho phân quyền động (ADMIN tự gán permission cho role).

## Ánh xạ Chức năng → Quyền sử dụng (theo endpoint thực tế)

Bảng dưới liệt kê **quyền thực tế đang enforce** trên từng chức năng. Vì hệ thống hiện dùng **role-gate** (`@PreAuthorize("hasAnyRole(...)")`) + domain policy thay vì `hasAuthority(permission)`, cột "Cơ chế enforce" ghi rõ role nào được phép. Cột "Permission (matrix)" là permission khái niệm tương ứng ở bảng trên (mang tính tài liệu).

### Auth & Tài khoản cá nhân (self-service)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Đăng ký | `POST /api/v1/auth/register` | (public) | — | Tài khoản mới luôn là MEMBER |
| Đăng nhập | `POST /api/v1/auth/login` | (public) | — | User bị khóa (`enabled = false`) → 403 `ACCOUNT_DISABLED` |
| Xem thông tin cá nhân + số dư | `GET /api/v1/users/me/profile` | Mọi role đã đăng nhập | — | Chỉ trả về của chính mình (JWT `userId`) |
| Sửa thông tin cá nhân (name, avatarUrl) | `PUT /api/v1/users/me/profile` | Mọi role đã đăng nhập | — | KHÔNG sửa được email/username/role/isInternal/balance |
| Đổi mật khẩu | `PUT /api/v1/users/me/password` | Mọi role đã đăng nhập | — | Yêu cầu đúng `currentPassword` |
| Upload ảnh đại diện | `POST /api/v1/users/me/avatar` | Mọi role đã đăng nhập | — | JPEG/PNG/WebP, ≤ 2MB |

### Ví & Giao dịch

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Nạp tiền (legacy) | `POST /api/v1/users/me/top-up` | Mọi role đã đăng nhập | — | |
| Khởi tạo nạp tiền (QR/mock) | `POST /api/v1/wallet/top-up/init` | Mọi role đã đăng nhập | — | |
| Mock webhook (dev) | `POST /api/v1/webhook/mock` | (public, chỉ active khi `payment.provider=mock`) | — | |
| Admin cộng tiền thủ công | `POST /api/v1/admin/users/{userId}/top-up`, `POST /api/v1/admin/users/top-up` | `SUPER_ADMIN` | `MANAGE_WALLET` 🆕 | |
| Lịch sử giao dịch của tôi | `GET /api/v1/users/me/transactions` | Mọi role đã đăng nhập | — | Chỉ giao dịch của chính mình (CREDIT nạp + DEBIT mua) |
| Mua khóa học | `POST /api/v1/courses/{id}/purchase` | Mọi role đã đăng nhập | `USE_VOUCHER` (khi gửi voucherCode) | Internal Member 0đ; INSTRUCTOR/STAFF/ADMIN_USER gửi voucher → 403 |
| Quote giá (preview voucher) | `POST /api/v1/courses/{id}/quote` | MEMBER, SUPER_ADMIN (nếu gửi voucherCode) | `USE_VOUCHER` | |

### Khóa học / Section / Lesson

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Xem danh sách / chi tiết course (public) | `GET /api/v1/courses`, `GET /api/v1/courses/{id}` | (public) | `VIEW_COURSE` | Chỉ trả course `published = true` |
| Tạo khóa học | `POST /api/v1/courses` | INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN | `CREATE_COURSE` | Mặc định ẩn, chờ duyệt |
| Sửa khóa học | `PUT /api/v1/courses/{id}` | INSTRUCTOR (của mình), STAFF, ADMIN_USER, SUPER_ADMIN | `EDIT_COURSE` | INSTRUCTOR không sửa giá khi `priceLocked` (`COURSE_PRICE_LOCKED`) |
| Xóa khóa học | `DELETE /api/v1/courses/{id}` | INSTRUCTOR (của mình), STAFF, ADMIN_USER, SUPER_ADMIN | `DELETE_COURSE` | |
| Tạo / Sửa / Xóa Section | `.../sections` (POST/PUT/DELETE) | INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN | `CREATE_SECTION`, `EDIT_SECTION` | ADMIN_USER KHÔNG có quyền |
| Tạo / Sửa / Xóa Lesson | `.../lessons` (POST/PUT/DELETE) | INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN | `CREATE_LESSON`, `EDIT_LESSON` | ADMIN_USER KHÔNG có quyền |
| Xem lesson (nội dung trả phí) | `GET .../lessons` | SUPER_ADMIN, STAFF; INSTRUCTOR (chủ sở hữu); MEMBER (đã enrolled) | `VIEW_LESSON` 🆕 | ADMIN_USER → 403; MEMBER chưa mua → 403 (`LESSON_ACCESS_DENIED`) |

### Tiến độ học & Kiểm tra (Lesson Progress & Section Test)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Đánh dấu / bỏ đánh dấu hoàn thành | `POST` / `DELETE .../lessons/{lessonId}/complete` | SUPER_ADMIN, STAFF; INSTRUCTOR (chủ sở hữu); MEMBER (đã enrolled) | `TRACK_PROGRESS` 🆕 | ADMIN_USER → 403 |
| Xem tiến độ tổng quan | `GET /api/v1/courses/{courseId}/progress` | SUPER_ADMIN, STAFF; INSTRUCTOR (chủ sở hữu); MEMBER (đã enrolled) | `TRACK_PROGRESS` 🆕 | ADMIN_USER → 403 |
| Xem bài test chương | `GET /api/v1/sections/{sectionId}/test` | Mọi role đã đăng nhập | `TRACK_PROGRESS` | Cần bổ sung kiểm tra enrollment (chưa enforce) |
| Nộp bài test chương | `POST /api/v1/sections/{sectionId}/submit-test` | Mọi role đã đăng nhập | `TRACK_PROGRESS` | Cần bổ sung kiểm tra enrollment (chưa enforce) |
| Lấy bài test gốc (kèm đáp án) | `GET /api/v1/admin/sections/{sectionId}/test` | INSTRUCTOR (chủ sở hữu), STAFF, SUPER_ADMIN | `EDIT_SECTION` | Dùng để sửa bài test |
| Cập nhật bài test (Admin) | `PUT /api/v1/admin/sections/{sectionId}/test` | INSTRUCTOR (chủ sở hữu), STAFF, SUPER_ADMIN | `EDIT_SECTION` | Dùng để sửa bài test |

### Duyệt khóa học (Course Approval)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Danh sách course chờ duyệt | `GET /api/v1/admin/courses/pending` | STAFF, SUPER_ADMIN | `PUBLISH_COURSE` | |
| Toàn bộ course (admin) | `GET /api/v1/admin/courses` | STAFF, SUPER_ADMIN | `PUBLISH_COURSE` | |
| Duyệt / Ẩn course | `POST .../publish`, `POST .../unpublish` | STAFF, SUPER_ADMIN | `PUBLISH_COURSE` | |
| Sửa giá (kể cả priceLocked) | `PUT /api/v1/admin/courses/{id}/price` | STAFF, SUPER_ADMIN | `LOCK_COURSE_PRICE` | |
| Course của Instructor (cả draft) | `GET /api/v1/instructor/courses`, `.../{id}` | INSTRUCTOR (của mình) | — | |

### Quản trị người dùng (Admin)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Danh sách users | `GET /api/v1/admin/users` | ADMIN_USER, SUPER_ADMIN | `VIEW_USER` | |
| Tạo tài khoản | `POST /api/v1/admin/users` | ADMIN_USER, SUPER_ADMIN, **STAFF** | `CREATE_USER` | Role-gate rộng hơn matrix (STAFF tạo được dù không có `CREATE_USER`) |
| Sửa user (name, role, isInternal) | `PUT /api/v1/admin/users/{id}` | ADMIN_USER, SUPER_ADMIN | `EDIT_USER` | Không tự đổi role mình; ADMIN_USER không sửa SUPER_ADMIN |
| Khóa / Mở khóa tài khoản | `PATCH /api/v1/admin/users/{id}/status` | ADMIN_USER, SUPER_ADMIN | `EDIT_USER` | Không tự khóa mình; ADMIN_USER không khóa SUPER_ADMIN |

### Voucher (Admin)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Tạo / Sửa / Xóa / Xem voucher | `/api/v1/admin/vouchers` (CRUD) | STAFF, SUPER_ADMIN | `MANAGE_VOUCHER` | |

### Báo cáo & Giao dịch (Admin)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Giao dịch toàn hệ thống | `GET /api/v1/admin/transactions` | SUPER_ADMIN | `VIEW_TRANSACTION` 🆕 | `@PreAuthorize("hasRole('SUPER_ADMIN')")`; lọc + phân trang |
| Báo cáo doanh thu | `GET /api/v1/admin/reports/revenue` | SUPER_ADMIN | `VIEW_REVENUE` 🆕 | `@PreAuthorize("hasRole('SUPER_ADMIN')")`; DAY / MONTH |
| Admin cộng tiền vào ví | `POST /api/v1/admin/users/{userId}/top-up`, `POST /api/v1/admin/users/top-up` | SUPER_ADMIN | `MANAGE_WALLET` 🆕 | `@PreAuthorize("hasRole('SUPER_ADMIN')")`; push WebSocket `WALLET_UPDATED` |

### Quản lý Phòng ban (Admin)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Tạo / Sửa / Xóa / Xem phòng ban | `/api/v1/admin/departments` (CRUD) | ADMIN_USER, SUPER_ADMIN | `MANAGE_DEPARTMENT` 🆕 | Cấu trúc cây (hierarchy) |

> Quy ước cột "Role được phép":
> - "Mọi role đã đăng nhập" = endpoint thuộc nhánh `anyRequest().authenticated()`, không gắn role gate; chỉ cần JWT hợp lệ. Logic "chỉ của chính mình" được enforce trong use case bằng `userId` lấy từ JWT.
> - "(của mình)" / "(chủ sở hữu)" / "(đã enrolled)" = role gate cho phép vào, nhưng domain policy (`CourseOwnershipPolicy`, `LessonAuthorizationService`) raise 403 nếu không thỏa điều kiện sở hữu / enrollment.



Các quyền dưới đây có thể được thêm vào hệ thống sau này khi cần (chưa có endpoint tương ứng):

| # | Permission | Mô tả | Gợi ý Role |
|---|-----------|-------|------------|
| 25 | `GRADE_STUDENT` | Chấm điểm học viên | INSTRUCTOR, SUPER_ADMIN |
| 26 | `VIEW_GRADE` | Xem điểm của chính mình | MEMBER |
| 27 | `MANAGE_PAYMENT` | Quản lý thanh toán, học phí | SUPER_ADMIN |
| 28 | `SEND_NOTIFICATION` | Gửi thông báo đến người dùng | STAFF, SUPER_ADMIN |
| 29 | `MANAGE_SCHEDULE` | Quản lý lịch học, thời khóa biểu | INSTRUCTOR, SUPER_ADMIN |
| 30 | `EXPORT_DATA` | Xuất dữ liệu (CSV, Excel) | SUPER_ADMIN |
| 31 | `REVIEW_CONTENT` | Kiểm duyệt nội dung trước khi publish | STAFF, SUPER_ADMIN |

## Cấu trúc DB dự kiến

```sql
-- Bảng roles 
SELECT * FROM roles;
-- 1 | MEMBER       | Học viên (nội bộ/ngoài)
-- 2 | INSTRUCTOR   | Giảng viên
-- 3 | STAFF        | Nhân viên / Trợ lý quản lý nội dung
-- 4 | ADMIN_USER   | Quản lý tài khoản
-- 5 | SUPER_ADMIN  | Quản trị viên tối cao

-- Bảng users (đã có trường is_internal)
-- is_internal BOOLEAN DEFAULT FALSE;

-- Bảng permissions (24 permissions đã seed — gồm CREATE_USER + 5 permission mới #20–#24)
SELECT * FROM permissions ORDER BY id;
-- 1  | VIEW_COURSE         | Xem khóa học
-- 2  | ENROLL_COURSE       | Đăng ký khóa học
-- 3  | CREATE_COURSE       | Tạo khóa học mới
-- 4  | EDIT_COURSE         | Chỉnh sửa khóa học
-- 5  | DELETE_COURSE       | Xóa khóa học
-- 6  | CREATE_SECTION      | Tạo chương học trong khóa học
-- 7  | EDIT_SECTION        | Sửa / Xóa chương học
-- 8  | CREATE_LESSON       | Tạo bài giảng trong chương học
-- 9  | EDIT_LESSON         | Sửa / Xóa bài giảng
-- 10 | VIEW_USER           | Xem thông tin người dùng
-- 11 | EDIT_USER           | Chỉnh sửa thông tin người dùng
-- 12 | DELETE_USER         | Xóa người dùng
-- 13 | MANAGE_ROLE         | Quản lý vai trò và phân quyền
-- 14 | VIEW_REPORT         | Xem báo cáo thống kê
-- 15 | PUBLISH_COURSE      | Duyệt và publish khóa học
-- 16 | LOCK_COURSE_PRICE   | Khóa giá / sửa giá đã khóa
-- 17 | MANAGE_VOUCHER      | Tạo / sửa / xóa / xem voucher
-- 18 | USE_VOUCHER         | Áp dụng voucher khi mua khóa học
-- 19 | CREATE_USER         | Cấp tài khoản mới (nội bộ/ngoài)
-- 20 | VIEW_LESSON         | Xem nội dung bài giảng đã trả phí
-- 21 | TRACK_PROGRESS      | Đánh dấu hoàn thành & xem tiến độ học
-- 22 | VIEW_TRANSACTION    | Xem giao dịch toàn hệ thống
-- 23 | VIEW_REVENUE        | Xem báo cáo doanh thu
-- 24 | MANAGE_WALLET       | Admin cộng tiền thủ công vào ví user
-- 25 | MANAGE_DEPARTMENT   | Quản lý phòng ban
-- (Ghi chú: id 19–25 là thứ tự seed; số thứ tự trong bảng matrix ở trên là #11 + #20–#25)

-- Bảng role_permissions (junction table)
SELECT r.name AS role, p.name AS permission
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.name, p.name;
```
