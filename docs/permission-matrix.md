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
| 3 | `CREATE_COURSE` | Tạo khóa học mới | [ ] | [x] | [ ] | [ ] | [x] |
| 4 | `EDIT_COURSE` | Chỉnh sửa thông tin khóa học | [ ] | [x] | [ ] | [ ] | [x] |
| 5 | `DELETE_COURSE` | Xóa khóa học | [ ] | [x] | [ ] | [ ] | [x] |
| 6 | `UPLOAD_CONTENT` | Tải lên tài liệu, bài giảng, video | [ ] | [x] | [ ] | [ ] | [x] |
| 7 | `VIEW_USER` | Xem thông tin người dùng | [ ] | [ ] | [ ] | [x] | [x] |
| 8 | `CREATE_USER` | Cấp tài khoản mới (Nội bộ/Ngoài) | [ ] | [ ] | [ ] | [x] | [x] |
| 9 | `EDIT_USER` | Chỉnh sửa thông tin người dùng | [ ] | [ ] | [ ] | [x] | [x] |
| 10 | `DELETE_USER` | Xóa người dùng | [ ] | [ ] | [ ] | [ ] | [x] |
| 11 | `MANAGE_ROLE` | Quản lý vai trò và phân quyền | [ ] | [ ] | [ ] | [ ] | [x] |
| 12 | `VIEW_REPORT` | Xem báo cáo, thống kê | [ ] | [x] | [ ] | [ ] | [x] |

## Quyền mở rộng (có thể thêm sau)

Các quyền dưới đây có thể được thêm vào hệ thống sau này khi cần:

| # | Permission | Mô tả | Gợi ý Role |
|---|-----------|-------|------------|
| 13 | `GRADE_STUDENT` | Chấm điểm học viên | INSTRUCTOR, SUPER_ADMIN |
| 14 | `VIEW_GRADE` | Xem điểm của chính mình | MEMBER |
| 15 | `MANAGE_PAYMENT` | Quản lý thanh toán, học phí | SUPER_ADMIN |
| 16 | `SEND_NOTIFICATION` | Gửi thông báo đến người dùng | STAFF, SUPER_ADMIN |
| 17 | `MANAGE_SCHEDULE` | Quản lý lịch học, thời khóa biểu | INSTRUCTOR, SUPER_ADMIN |
| 18 | `EXPORT_DATA` | Xuất dữ liệu (CSV, Excel) | SUPER_ADMIN |
| 19 | `REVIEW_CONTENT` | Kiểm duyệt nội dung trước khi publish | STAFF, SUPER_ADMIN |

## Cấu trúc DB dự kiến

```sql
-- Bảng roles 
SELECT * FROM roles;
-- 1 | MEMBER       | Học viên (nội bộ/ngoài)
-- 2 | INSTRUCTOR   | Giảng viên
-- 3 | STAFF        | Nhân viên
-- 4 | ADMIN_USER   | Quản lý user
-- 5 | SUPER_ADMIN  | Quản trị viên tối cao

-- Bảng users (sẽ thêm trường is_internal)
-- Thêm cột is_internal BOOLEAN DEFAULT FALSE;

-- Bảng permissions (sẽ seed dựa trên ma trận đã duyệt)
SELECT * FROM permissions;

-- Bảng role_permissions (junction table)
SELECT r.name AS role, p.name AS permission
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
ORDER BY r.name, p.name;
```
