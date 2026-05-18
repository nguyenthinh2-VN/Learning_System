-- Bảng roles (MEMBER, INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN)
INSERT IGNORE INTO roles (name, description) VALUES
('MEMBER', 'Học viên (nội bộ/ngoài)'),
('INSTRUCTOR', 'Giảng viên'),
('STAFF', 'Nhân viên / Trợ lý quản lý nội dung'),
('ADMIN_USER', 'Quản trị viên quản lý tài khoản'),
('SUPER_ADMIN', 'Quản trị viên hệ thống (toàn quyền)');

-- Bảng permissions
INSERT IGNORE INTO permissions (name, description) VALUES
('VIEW_COURSE', 'Xem danh sách và chi tiết khóa học'),
('ENROLL_COURSE', 'Đăng ký tham gia khóa học'),
('CREATE_COURSE', 'Tạo khóa học mới'),
('EDIT_COURSE', 'Chỉnh sửa thông tin khóa học'),
('DELETE_COURSE', 'Xóa khóa học'),
('UPLOAD_CONTENT', 'Tải lên tài liệu, bài giảng, video'),
('VIEW_USER', 'Xem thông tin người dùng'),
('CREATE_USER', 'Cấp tài khoản mới (Nội bộ/Ngoài)'),
('EDIT_USER', 'Chỉnh sửa thông tin người dùng'),
('DELETE_USER', 'Xóa người dùng'),
('MANAGE_ROLE', 'Quản lý vai trò và phân quyền'),
('VIEW_REPORT', 'Xem báo cáo, thống kê');

-- Gán quyền cho MEMBER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'MEMBER' AND p.name IN ('VIEW_COURSE');

-- Gán quyền cho INSTRUCTOR
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'INSTRUCTOR' AND p.name IN (
    'VIEW_COURSE', 'CREATE_COURSE', 'EDIT_COURSE', 'DELETE_COURSE', 'UPLOAD_CONTENT', 'VIEW_REPORT'
);

-- Gán quyền cho STAFF
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'STAFF' AND p.name IN (
    'VIEW_COURSE'
);

-- Gán quyền cho ADMIN_USER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN_USER' AND p.name IN (
    'VIEW_COURSE', 'VIEW_USER', 'CREATE_USER', 'EDIT_USER'
);

-- Gán quyền cho SUPER_ADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN';

-- Sample Data cho Courses
INSERT INTO courses (title, description, max_students, enrolled_count) VALUES
('Spring Boot Clean Architecture', 'Khóa học hướng dẫn xây dựng dự án thực tế với Spring Boot và Clean Architecture', 100, 5),
('Java Core từ cơ bản đến nâng cao', 'Nắm vững kiến thức nền tảng Java để dễ dàng học Spring Framework', 200, 45),
('Thiết kế Database chuẩn', 'Cách chuẩn hóa dữ liệu, index và tối ưu truy vấn MySQL', 50, 20),
('Vue.js 3 thực chiến', 'Kết hợp với Spring Boot làm backend tạo hệ thống trọn vẹn', 150, 10);
