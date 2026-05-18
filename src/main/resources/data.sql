-- Bảng permissions (Dựa vào permission-matrix.md)
INSERT IGNORE INTO permissions (name, description) VALUES
('VIEW_COURSE', 'Xem danh sách và chi tiết khóa học'),
('ENROLL_COURSE', 'Đăng ký tham gia khóa học'),
('CREATE_COURSE', 'Tạo khóa học mới'),
('EDIT_COURSE', 'Chỉnh sửa thông tin khóa học'),
('DELETE_COURSE', 'Xóa khóa học'),
('VIEW_USER', 'Xem thông tin người dùng'),
('EDIT_USER', 'Chỉnh sửa thông tin người dùng'),
('DELETE_USER', 'Xóa người dùng'),
('MANAGE_ROLE', 'Quản lý vai trò và phân quyền'),
('VIEW_REPORT', 'Xem báo cáo, thống kê');

-- Gán quyền (Dựa trên check list)
-- MEMBER: VIEW_COURSE
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'MEMBER' AND p.name IN ('VIEW_COURSE');

-- STAFF: VIEW_COURSE
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'STAFF' AND p.name IN ('VIEW_COURSE');

-- ADMIN: VIEW_COURSE
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN' AND p.name IN ('VIEW_COURSE');

-- Sample Data cho Courses
INSERT INTO courses (title, description, max_students, enrolled_count) VALUES
('Spring Boot Clean Architecture', 'Khóa học hướng dẫn xây dựng dự án thực tế với Spring Boot và Clean Architecture', 100, 5),
('Java Core từ cơ bản đến nâng cao', 'Nắm vững kiến thức nền tảng Java để dễ dàng học Spring Framework', 200, 45),
('Thiết kế Database chuẩn', 'Cách chuẩn hóa dữ liệu, index và tối ưu truy vấn MySQL', 50, 20),
('Vue.js 3 thực chiến', 'Kết hợp với Spring Boot làm backend tạo hệ thống trọn vẹn', 150, 10);
