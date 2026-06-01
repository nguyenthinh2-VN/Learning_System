# Phân quyền động (Dynamic RBAC Management)

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 18. Phân quyền động

Tính năng cho phép quản trị viên **gán / gỡ permission cho từng role** qua API/UI, và hệ thống enforce theo permission động — đổi ma trận quyền **không cần deploy lại code**.

Tham khảo: ma trận quyền tại [permission-matrix.md](../permission-matrix.md); kế hoạch chi tiết tại `docs/plan/plan-dynamic-permission.md`.

### Cơ chế enforce (Hybrid cache)

```
Login  → JWT chỉ chứa `role` (KHÔNG chứa permission)
Request → JwtAuthenticationFilter đọc role từ JWT
        → PermissionCacheService.getPermissions(role)   (cache in-memory, O(1))
        → set authorities = [ ROLE_<role> ] ∪ [ từng permission ]
        → @PreAuthorize("hasAuthority('PERMISSION')") đọc được
Update  → khi admin đổi ma trận → cache reload ngay → có hiệu lực tức thì
```

- JWT giữ nguyên cấu trúc → user **không cần đăng nhập lại** khi quyền thay đổi.
- Filter set **cả** `ROLE_<role>` (giữ tương thích `hasAnyRole(...)` cũ) **lẫn** từng permission (cho `hasAuthority(...)` mới). Các endpoint đang được migrate dần sang `hasAuthority(...)`.
- Cache là per-instance; khi scale nhiều instance cần cơ chế refresh phân tán (ngoài phạm vi hiện tại).

### Phân quyền cho chính nhóm endpoint này

| Hành động | Permission yêu cầu |
|-----------|--------------------|
| Toàn bộ endpoint trong tài liệu này | `MANAGE_ROLE` |

> Theo ma trận hiện tại, chỉ `SUPER_ADMIN` có `MANAGE_ROLE`. Nhóm endpoint này enforce bằng `@PreAuthorize("hasAuthority('MANAGE_ROLE')")` — role khác → 403 `ACCESS_DENIED`.

### Quy tắc bất biến (chống tự khóa)

- Role `SUPER_ADMIN` **luôn full quyền** và **không sửa được** qua API — mọi thao tác cập nhật ma trận của `SUPER_ADMIN` → 400 `PROTECTED_ROLE`.
- Trong ma trận trả về, role `SUPER_ADMIN` có cờ `locked = true` (FE hiển thị read-only).

### Audit

Mỗi lần cập nhật ma trận được ghi vào `logs/permission_audit.jsonl` (event `ROLE_PERMISSIONS_UPDATED`) gồm: `actorId`, `roleName`, `before`, `after`, `granted`, `revoked`, `timestamp`.

---

### 18.1. Danh sách tất cả permission

**Endpoint:** `GET /api/v1/admin/permissions`

**Yêu cầu quyền:** `MANAGE_ROLE`.

Trả về toàn bộ permission khả dụng trong hệ thống (để FE render cột/danh sách).

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    { "id": 1, "name": "VIEW_COURSE", "description": "Xem khóa học" },
    { "id": 2, "name": "ENROLL_COURSE", "description": "Đăng ký khóa học" },
    { "id": 3, "name": "CREATE_COURSE", "description": "Tạo khóa học mới" }
  ],
  "timestamp": "2026-06-01T09:00:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID permission |
| name | String | Mã permission (UPPER_SNAKE_CASE) |
| description | String | Mô tả tiếng Việt |

---

### 18.2. Toàn bộ ma trận role → permission

**Endpoint:** `GET /api/v1/admin/roles/permissions`

**Yêu cầu quyền:** `MANAGE_ROLE`.

Trả về tất cả permission khả dụng + permission của từng role. FE dùng để render bảng checkbox role × permission.

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "allPermissions": [
      { "id": 1, "name": "VIEW_COURSE", "description": "Xem khóa học" },
      { "id": 17, "name": "MANAGE_VOUCHER", "description": "Tạo / sửa / xóa / xem voucher" }
    ],
    "roles": [
      {
        "roleName": "MEMBER",
        "permissions": ["USE_VOUCHER", "VIEW_COURSE", "VIEW_LESSON", "TRACK_PROGRESS"],
        "locked": false
      },
      {
        "roleName": "STAFF",
        "permissions": ["CREATE_COURSE", "MANAGE_VOUCHER", "PUBLISH_COURSE", "VIEW_COURSE"],
        "locked": false
      },
      {
        "roleName": "SUPER_ADMIN",
        "permissions": ["VIEW_COURSE", "MANAGE_ROLE", "MANAGE_VOUCHER"],
        "locked": true
      }
    ]
  },
  "timestamp": "2026-06-01T09:00:00"
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| allPermissions | Object[] | Tất cả permission khả dụng (id, name, description) |
| roles[].roleName | String | Tên role |
| roles[].permissions | String[] | Danh sách permission role đang có (sắp xếp A→Z) |
| roles[].locked | Boolean | `true` = role được bảo vệ (SUPER_ADMIN), read-only, không sửa được |

---

### 18.3. Permission của một role

**Endpoint:** `GET /api/v1/admin/roles/{roleName}/permissions`

**Yêu cầu quyền:** `MANAGE_ROLE`.

**Path Parameter:** `roleName` — một trong `MEMBER`, `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`.

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "roleName": "STAFF",
    "permissions": ["CREATE_COURSE", "EDIT_COURSE", "MANAGE_VOUCHER", "PUBLISH_COURSE", "VIEW_COURSE"],
    "locked": false
  },
  "timestamp": "2026-06-01T09:00:00"
}
```

**Response 404 (Role không tồn tại):**
```json
{
  "code": "ROLE_NOT_FOUND",
  "message": "Không tìm thấy vai trò với tên: GHOST",
  "timestamp": "2026-06-01T09:00:00"
}
```

---

### 18.4. Cập nhật permission cho một role (replace-all)

**Endpoint:** `PUT /api/v1/admin/roles/{roleName}/permissions`

**Yêu cầu quyền:** `MANAGE_ROLE`.

Thay thế **toàn bộ** permission của role bằng danh sách mới (không phải toggle từng cái). Gửi danh sách rỗng `[]` = gỡ hết quyền của role.

**Path Parameter:** `roleName` — role cần sửa (không được là `SUPER_ADMIN`).

**Request Body:**
```json
{
  "permissions": ["VIEW_COURSE", "CREATE_COURSE", "EDIT_COURSE", "PUBLISH_COURSE", "MANAGE_VOUCHER"]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| permissions | String[] | Yes (`@NotNull`) | Danh sách tên permission. Cho phép rỗng `[]`. Tên được chuẩn hóa UPPER_CASE + khử trùng lặp. Mọi tên phải tồn tại trong hệ thống. |

**Hành vi:**
- Validate role tồn tại → 404 `ROLE_NOT_FOUND` nếu không.
- Chặn nếu role bị bảo vệ (`SUPER_ADMIN`) → 400 `PROTECTED_ROLE`.
- Validate mọi permission tồn tại → 404 `PERMISSION_NOT_FOUND` nếu có tên lạ (không lưu gì cả).
- Replace-all trong một transaction (xóa hết row cũ → insert mới).
- Ghi audit + reload cache → **hiệu lực tức thì**.

**Response 200:**
```json
{
  "status": 200,
  "message": "Cập nhật phân quyền thành công",
  "data": {
    "roleName": "STAFF",
    "permissions": ["CREATE_COURSE", "EDIT_COURSE", "MANAGE_VOUCHER", "PUBLISH_COURSE", "VIEW_COURSE"],
    "locked": false
  },
  "timestamp": "2026-06-01T09:05:00"
}
```

**Response 400 — sửa role được bảo vệ (SUPER_ADMIN):**
```json
{
  "code": "PROTECTED_ROLE",
  "message": "Không thể chỉnh sửa phân quyền của vai trò được bảo vệ: SUPER_ADMIN",
  "timestamp": "2026-06-01T09:05:00"
}
```

**Response 404 — permission không tồn tại:**
```json
{
  "code": "PERMISSION_NOT_FOUND",
  "message": "Không tìm thấy permission với tên: GHOST_PERM",
  "timestamp": "2026-06-01T09:05:00"
}
```

**Response 404 — role không tồn tại:**
```json
{
  "code": "ROLE_NOT_FOUND",
  "message": "Không tìm thấy vai trò với tên: GHOST",
  "timestamp": "2026-06-01T09:05:00"
}
```

**Response 400 — body thiếu `permissions`:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "permissions: permissions không được null",
  "timestamp": "2026-06-01T09:05:00"
}
```

---

### Mã lỗi (Phân quyền động)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 404 | `ROLE_NOT_FOUND` | Không tìm thấy role theo tên |
| 404 | `PERMISSION_NOT_FOUND` | Có permission name không tồn tại trong danh sách gửi lên |
| 400 | `PROTECTED_ROLE` | Cố sửa ma trận của role được bảo vệ (SUPER_ADMIN) |
| 400 | `VALIDATION_ERROR` | Body thiếu field `permissions` |
| 403 | `ACCESS_DENIED` | Không có permission `MANAGE_ROLE` |

---

### Tóm tắt endpoint

| Method | URL | Permission | Mô tả |
|--------|-----|------------|-------|
| `GET` | `/api/v1/admin/permissions` | `MANAGE_ROLE` | Danh sách tất cả permission |
| `GET` | `/api/v1/admin/roles/permissions` | `MANAGE_ROLE` | Toàn bộ ma trận role → permission |
| `GET` | `/api/v1/admin/roles/{roleName}/permissions` | `MANAGE_ROLE` | Permission của một role |
| `PUT` | `/api/v1/admin/roles/{roleName}/permissions` | `MANAGE_ROLE` | Thay thế toàn bộ permission của role (replace-all) |
