# Roles, Permissions & Database Schema

[← Quay lại mục lục](./README.md)

---

## 4. Roles & Permissions

| Role | Mặc định khi | Mô tả |
|------|-------------|-------|
| MEMBER | Đăng ký tự do | Học viên (Có thể là nội bộ hoặc ngoài) |
| INSTRUCTOR | Admin/Staff gán | Giảng viên |
| STAFF | Admin gán | Nhân viên / Trợ lý nội dung |
| ADMIN_USER | Super Admin gán | Quản lý tài khoản |
| SUPER_ADMIN| System | Quản trị viên toàn quyền |

> Xem chi tiết ma trận phân quyền tại `docs/permission-matrix.md`

---

## 5. Database Schema

### Bảng `users`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt hashed) |
| name | VARCHAR(200) | NOT NULL |
| role_id | BIGINT | FK → roles.id, NOT NULL |
| is_internal | BOOLEAN | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### Bảng `roles`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | |

### Bảng `permissions`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | |

### Bảng `role_permissions`
| Column | Type | Constraint |
|--------|------|-----------|
| id | BIGINT | PK, AUTO_INCREMENT |
| role_id | BIGINT | FK → roles.id |
| permission_id | BIGINT | FK → permissions.id |
