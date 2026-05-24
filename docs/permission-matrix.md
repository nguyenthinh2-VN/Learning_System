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

> Lưu ý phân quyền:
> - `INSTRUCTOR` không có `PUBLISH_COURSE` — phải nhờ STAFF/SUPER_ADMIN duyệt course mới publish được.
> - `INSTRUCTOR` cũng không có `LOCK_COURSE_PRICE` — sau khi course được publish, giá bị khóa, INSTRUCTOR phải nhờ admin sửa giá.
> - Chỉ `MEMBER` (External / Internal) và `SUPER_ADMIN` có `USE_VOUCHER`. INSTRUCTOR / STAFF / ADMIN_USER không có vì họ không phải đối tượng mua khóa học.
> - `MEMBER` Internal (`is_internal = TRUE`) được mua khóa học với giá 0đ, voucher bị bỏ qua khi mua. Chỉ External Member dùng voucher mới có ý nghĩa.

## Quyền mở rộng (có thể thêm sau)

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

-- Bảng permissions (19 permissions đã seed)
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
-- 11 | CREATE_USER         | Cấp tài khoản mới (Nội bộ/Ngoài)
-- 12 | EDIT_USER           | Chỉnh sửa thông tin người dùng
-- 13 | DELETE_USER         | Xóa người dùng
-- 14 | MANAGE_ROLE         | Quản lý vai trò và phân quyền
-- 15 | VIEW_REPORT         | Xem báo cáo thống kê
-- 16 | PUBLISH_COURSE      | Duyệt và publish khóa học
-- 17 | LOCK_COURSE_PRICE   | Khóa giá / sửa giá đã khóa
-- 18 | MANAGE_VOUCHER      | Tạo / sửa / xóa / xem voucher
-- 19 | USE_VOUCHER         | Áp dụng voucher khi mua khóa học

-- Bảng role_permissions (junction table)
SELECT r.name AS role, p.name AS permission
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.name, p.name;
```
