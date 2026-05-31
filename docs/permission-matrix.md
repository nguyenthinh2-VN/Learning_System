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
| 11 | `CREATE_USER` ⚠️ | Cấp tài khoản mới (Nội bộ/Ngoài) | [ ] | [ ] | [ ] | [x] | [x] |
| 12 | `EDIT_USER` | Chỉnh sửa thông tin người dùng | [ ] | [ ] | [ ] | [x] | [x] |
| 13 | `DELETE_USER` | Xóa người dùng | [ ] | [ ] | [ ] | [ ] | [x] |
| 14 | `MANAGE_ROLE` | Quản lý vai trò và phân quyền | [ ] | [ ] | [ ] | [ ] | [x] |
| 15 | `VIEW_REPORT` | Xem báo cáo, thống kê | [ ] | [x] | [ ] | [ ] | [x] |
| 16 | `PUBLISH_COURSE` | Duyệt và publish khóa học (set published = true, lock giá) | [ ] | [ ] | [x] | [ ] | [x] |
| 17 | `LOCK_COURSE_PRICE` | Sửa giá khóa học (kể cả khi đã priceLocked) | [ ] | [ ] | [x] | [ ] | [x] |
| 18 | `MANAGE_VOUCHER` | Tạo / sửa / xóa / xem voucher trong panel admin | [ ] | [ ] | [x] | [ ] | [x] |
| 19 | `USE_VOUCHER` | Áp dụng voucher khi quote / mua khóa học | [x] | [ ] | [ ] | [ ] | [x] |

> Lưu ý phân quyền:
> - ⚠️ `CREATE_USER` (#11) là **permission dự kiến** — hiện CHƯA được seed trong DB (`DataInitializer` chỉ seed 18 permissions, không bao gồm `CREATE_USER`). Endpoint tạo tài khoản `POST /api/v1/admin/users` hiện được bảo vệ bằng **role gate** `@PreAuthorize("hasAnyRole('ADMIN_USER','SUPER_ADMIN','STAFF')")` chứ không kiểm tra permission này. Vì là role-gate nên trên thực tế **STAFF cũng tạo được tài khoản** (rộng hơn ma trận `CREATE_USER` ở trên). Khi nào enforce `CREATE_USER` qua permission thật thì cần seed nó vào DB và đồng bộ lại danh sách role được phép.
> - Hệ thống hiện **chưa enforce authorization qua từng permission** (`hasAuthority(...)`); phân quyền thực tế chạy bằng `@PreAuthorize("hasAnyRole(...)")` + domain policy (`CourseOwnershipPolicy`, `LessonAuthorizationService`, ...). Bảng `permissions` mang tính tài liệu / để dành cho việc enforce sau này.
> - `INSTRUCTOR` không có `PUBLISH_COURSE` — phải nhờ STAFF/SUPER_ADMIN duyệt course mới publish được.
> - `INSTRUCTOR` cũng không có `LOCK_COURSE_PRICE` — sau khi course được publish, giá bị khóa, INSTRUCTOR phải nhờ admin sửa giá.
> - Chỉ `MEMBER` (External / Internal) và `SUPER_ADMIN` có `USE_VOUCHER`. INSTRUCTOR / STAFF / ADMIN_USER không có vì họ không phải đối tượng mua khóa học.
> - `MEMBER` Internal (`is_internal = TRUE`) được mua khóa học với giá 0đ, voucher bị bỏ qua khi mua. Chỉ External Member dùng voucher mới có ý nghĩa.

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
| Admin cộng tiền thủ công | `POST /api/v1/admin/users/{userId}/top-up`, `POST /api/v1/admin/users/top-up` | `SUPER_ADMIN` | — | |
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
| Xem lesson (nội dung trả phí) | `GET .../lessons` | SUPER_ADMIN, STAFF; INSTRUCTOR (chủ sở hữu); MEMBER (đã enrolled) | — | ADMIN_USER → 403; MEMBER chưa mua → 403 (`LESSON_ACCESS_DENIED`) |

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
| Tạo tài khoản | `POST /api/v1/admin/users` | ADMIN_USER, SUPER_ADMIN, **STAFF** | `CREATE_USER` ⚠️ | Role-gate rộng hơn matrix (STAFF tạo được) |
| Sửa user (name, role, isInternal) | `PUT /api/v1/admin/users/{id}` | ADMIN_USER, SUPER_ADMIN | `EDIT_USER` | Không tự đổi role mình; ADMIN_USER không sửa SUPER_ADMIN |
| Khóa / Mở khóa tài khoản | `PATCH /api/v1/admin/users/{id}/status` | ADMIN_USER, SUPER_ADMIN | `EDIT_USER` | Không tự khóa mình; ADMIN_USER không khóa SUPER_ADMIN |

### Voucher (Admin)

| Chức năng | Endpoint | Role được phép | Permission (matrix) | Ghi chú |
|-----------|----------|----------------|---------------------|---------|
| Tạo / Sửa / Xóa / Xem voucher | `/api/v1/admin/vouchers` (CRUD) | STAFF, SUPER_ADMIN | `MANAGE_VOUCHER` | |

> Quy ước cột "Role được phép":
> - "Mọi role đã đăng nhập" = endpoint thuộc nhánh `anyRequest().authenticated()`, không gắn role gate; chỉ cần JWT hợp lệ. Logic "chỉ của chính mình" được enforce trong use case bằng `userId` lấy từ JWT.
> - "(của mình)" / "(chủ sở hữu)" / "(đã enrolled)" = role gate cho phép vào, nhưng domain policy (`CourseOwnershipPolicy`, `LessonAuthorizationService`) raise 403 nếu không thỏa điều kiện sở hữu / enrollment.



Các quyền dưới đây có thể được thêm vào hệ thống sau này khi cần:

| # | Permission | Mô tả | Gợi ý Role |
|---|-----------|-------|------------|
| 20 | `GRADE_STUDENT` | Chấm điểm học viên | INSTRUCTOR, SUPER_ADMIN |
| 21 | `VIEW_GRADE` | Xem điểm của chính mình | MEMBER |
| 22 | `MANAGE_PAYMENT` | Quản lý thanh toán, học phí | SUPER_ADMIN |
| 23 | `SEND_NOTIFICATION` | Gửi thông báo đến người dùng | STAFF, SUPER_ADMIN |
| 24 | `MANAGE_SCHEDULE` | Quản lý lịch học, thời khóa biểu | INSTRUCTOR, SUPER_ADMIN |
| 25 | `EXPORT_DATA` | Xuất dữ liệu (CSV, Excel) | SUPER_ADMIN |
| 26 | `REVIEW_CONTENT` | Kiểm duyệt nội dung trước khi publish | STAFF, SUPER_ADMIN |

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

-- Bảng permissions (18 permissions đã seed — CREATE_USER CHƯA seed, xem ghi chú ⚠️)
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
-- (CREATE_USER          | Cấp tài khoản mới — ĐỊNH NGHĨA TRONG MATRIX NHƯNG CHƯA SEED)
-- 11 | EDIT_USER           | Chỉnh sửa thông tin người dùng
-- 12 | DELETE_USER         | Xóa người dùng
-- 13 | MANAGE_ROLE         | Quản lý vai trò và phân quyền
-- 14 | VIEW_REPORT         | Xem báo cáo thống kê
-- 15 | PUBLISH_COURSE      | Duyệt và publish khóa học
-- 16 | LOCK_COURSE_PRICE   | Khóa giá / sửa giá đã khóa
-- 17 | MANAGE_VOUCHER      | Tạo / sửa / xóa / xem voucher
-- 18 | USE_VOUCHER         | Áp dụng voucher khi mua khóa học

-- Bảng role_permissions (junction table)
SELECT r.name AS role, p.name AS permission
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.name, p.name;
```
