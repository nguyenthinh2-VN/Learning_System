# Kế hoạch: Admin Course/Section/Lesson Management + User List

**Ngày:** 2026-05-28  
**Phạm vi:** Frontend admin + Backend endpoint mới  
**Ưu tiên:** Cao

---

## Tổng quan

Bổ sung 2 nhóm tính năng vào Admin Portal:

| Nhóm | Mô tả | Phía |
|------|-------|------|
| A. Course Management | CRUD khóa học + Section + Lesson trong 1 trang | Frontend |
| B. User List | Danh sách người dùng + tìm kiếm/phân trang | Frontend + Backend |

---

## Phần A — Course Management (Admin)

### A.1. Phân tích hiện trạng

**Đã có:**
- `AdminCoursesPage.jsx` — bảng list courses, filter, publish/unpublish, sửa giá
- `AdminPendingCoursesPage.jsx` — duyệt courses chờ
- `adminApi.js` — `getAdminCoursesApi`, `publishCourseApi`, `unpublishCourseApi`, `updateCoursePriceApi`
- `course.js` — `createCourseApi`, `updateCourseApi`, `deleteCourseApi`
- `section.js` — full CRUD sections
- `lesson.js` — full CRUD lessons

**Chưa có:**
- UI tạo/sửa/xóa course trong admin
- UI quản lý sections và lessons (nested trong course)
- Thêm `createCourseApi`, `updateCourseApi`, `deleteCourseApi` vào `adminApi.js` (hiện dùng `api` từ `auth.js` — cần dùng `adminApi` với admin token)

### A.2. Thiết kế UI

**Trang `AdminCoursesPage.jsx` — mở rộng:**

```
┌─────────────────────────────────────────────────────────┐
│ Header: Quản lý khóa học          [+ Tạo khóa học]      │
├─────────────────────────────────────────────────────────┤
│ Filter: [Search] [Tất cả|Published|Pending|Draft]        │
├─────────────────────────────────────────────────────────┤
│ Table row:                                               │
│  Title | Status | Price | Enrolled | Actions            │
│  [Sửa giá] [Duyệt/Ẩn] [✏️ Sửa] [🗑️ Xóa] [📋 Nội dung] │
└─────────────────────────────────────────────────────────┘
```

**Nút "📋 Nội dung"** → mở trang/modal quản lý Section + Lesson của course đó.

**Modal/Drawer "Tạo/Sửa khóa học":**
```
┌─────────────────────────────────────────────────────────┐
│ Tạo khóa học mới                                    [X] │
├─────────────────────────────────────────────────────────┤
│ Tiêu đề *                                               │
│ [_________________________________]                     │
│ Mô tả                                                   │
│ [_________________________________]                     │
│ Giá (VNĐ) *          Số học viên tối đa *               │
│ [__________]          [__________]                      │
│ ID Giảng viên (nếu tạo thay)                            │
│ [__________]                                            │
│                          [Hủy]  [Tạo khóa học]         │
└─────────────────────────────────────────────────────────┘
```

**Trang `AdminCourseContentPage.jsx`** (route: `/admin/courses/:id/content`):
```
┌─────────────────────────────────────────────────────────┐
│ ← Quay lại | Nội dung: [Tên khóa học]                   │
├─────────────────────────────────────────────────────────┤
│ [+ Thêm chương]                                         │
│                                                         │
│ ▼ Chương 1: Giới thiệu                    [✏️] [🗑️]    │
│   ├─ Bài 1: Cài đặt môi trường            [✏️] [🗑️]    │
│   ├─ Bài 2: Tổng quan                     [✏️] [🗑️]    │
│   └─ [+ Thêm bài giảng]                               │
│                                                         │
│ ▼ Chương 2: Clean Architecture            [✏️] [🗑️]    │
│   └─ [+ Thêm bài giảng]                               │
└─────────────────────────────────────────────────────────┘
```

### A.3. API cần thêm vào `adminApi.js`

```js
// Dùng adminApi (admin token) thay vì api (public token)
export const adminCreateCourseApi = (data) =>
  adminApi.post('/courses', data);

export const adminUpdateCourseApi = (id, data) =>
  adminApi.put(`/courses/${id}`, data);

export const adminDeleteCourseApi = (id) =>
  adminApi.delete(`/courses/${id}`);

export const adminGetCourseDetailApi = (id) =>
  adminApi.get(`/courses/${id}`);

// Sections
export const adminGetSectionsApi = (courseId) =>
  adminApi.get(`/courses/${courseId}/sections`);

export const adminCreateSectionApi = (courseId, data) =>
  adminApi.post(`/courses/${courseId}/sections`, data);

export const adminUpdateSectionApi = (courseId, sectionId, data) =>
  adminApi.put(`/courses/${courseId}/sections/${sectionId}`, data);

export const adminDeleteSectionApi = (courseId, sectionId) =>
  adminApi.delete(`/courses/${courseId}/sections/${sectionId}`);

// Lessons
export const adminCreateLessonApi = (courseId, sectionId, data) =>
  adminApi.post(`/courses/${courseId}/sections/${sectionId}/lessons`, data);

export const adminUpdateLessonApi = (courseId, sectionId, lessonId, data) =>
  adminApi.put(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`, data);

export const adminDeleteLessonApi = (courseId, sectionId, lessonId) =>
  adminApi.delete(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`);
```

### A.4. File mới cần tạo

| File | Mô tả |
|------|-------|
| `fe/src/pages/admin/AdminCourseContentPage.jsx` | Trang quản lý Section + Lesson |

### A.5. File cần sửa

| File | Thay đổi |
|------|---------|
| `fe/src/api/adminApi.js` | Thêm các API course/section/lesson dùng adminApi |
| `fe/src/pages/admin/AdminCoursesPage.jsx` | Thêm nút Tạo/Sửa/Xóa course, nút "Nội dung" |
| `fe/src/App.jsx` | Thêm route `/admin/courses/:id/content` |
| `fe/src/components/layout/AdminSidebar.jsx` | Không cần sửa (menu đã có) |

---

## Phần B — User List (Admin)

### B.1. Phân tích hiện trạng

**Đã có:**
- `AdminUsersPage.jsx` — modal tạo user, placeholder "chờ BE endpoint"
- `POST /api/v1/admin/users` — tạo user (đã có)

**Chưa có:**
- `GET /api/v1/admin/users` — **endpoint BE chưa tồn tại**, cần tạo mới
- UI danh sách users với search + phân trang

### B.2. Thiết kế Backend endpoint mới

**Endpoint:** `GET /api/v1/admin/users`

**Yêu cầu quyền:** `ADMIN_USER`, `SUPER_ADMIN`

**Query Parameters:**
| Field | Type | Default | Mô tả |
|-------|------|---------|-------|
| keyword | String | (rỗng) | Tìm theo tên hoặc email |
| page | Integer | 0 | 0-indexed |
| size | Integer | 20 | Số user/trang |

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 50,
    "totalPages": 3,
    "page": 0,
    "size": 20,
    "items": [
      {
        "id": 1,
        "username": "MEM2B4A1D",
        "email": "user@example.com",
        "name": "Nguyễn Văn A",
        "role": "MEMBER",
        "isInternal": false,
        "createdAt": "2026-05-15T15:30:00"
      }
    ]
  }
}
```

**Các layer BE cần tạo/sửa:**

| Layer | File | Thay đổi |
|-------|------|---------|
| Controller | `UserController.java` | Thêm `GET /admin/users` handler |
| UseCase | `GetUsersUseCase.java` | Tạo mới — query users với keyword + phân trang |
| Repository | `UserRepository.java` (interface) | Thêm `findAll(keyword, page, size)` |
| Repository Impl | `UserRepositoryImpl.java` | Implement query với LIKE search |
| JPA Repository | `JpaUserRepository.java` | Thêm `findByNameContainingOrEmailContaining` |
| DTO | `UserListItemResponse.java` | Tạo mới — response DTO |

### B.3. Thiết kế UI

**`AdminUsersPage.jsx` — mở rộng:**

```
┌─────────────────────────────────────────────────────────┐
│ Header: Quản lý người dùng        [+ Thêm tài khoản]    │
├─────────────────────────────────────────────────────────┤
│ [🔍 Tìm theo tên hoặc email...]   [Làm mới]             │
├─────────────────────────────────────────────────────────┤
│ Table:                                                   │
│  Họ tên / Email | Vai trò | Loại TK | Ngày tạo | Action │
│  ...            | MEMBER  | Ngoài   | 15/05/26 | [...]  │
├─────────────────────────────────────────────────────────┤
│ Phân trang: < 1 2 3 >                                   │
└─────────────────────────────────────────────────────────┘
```

---

## Phần C — Routing mới

```jsx
// App.jsx — thêm route
<Route path="/admin/courses/:id/content" element={<AdminCourseContentPage />} />
```

---

## Thứ tự thực hiện

### Bước 1 — Backend: `GET /api/v1/admin/users`
1. Tạo `UserListItemResponse.java` (DTO)
2. Tạo `GetUsersUseCase.java`
3. Thêm method vào `UserRepository` interface + `UserRepositoryImpl`
4. Thêm query vào `JpaUserRepository`
5. Thêm handler vào `UserController.java`

### Bước 2 — Frontend API layer
6. Thêm `getUsersApi` vào `adminApi.js`
7. Thêm các API course/section/lesson dùng adminApi

### Bước 3 — Frontend UI
8. Sửa `AdminCoursesPage.jsx` — thêm Tạo/Sửa/Xóa course
9. Tạo `AdminCourseContentPage.jsx` — quản lý Section + Lesson
10. Sửa `AdminUsersPage.jsx` — thêm danh sách users
11. Sửa `App.jsx` — thêm route mới

---

## Phân quyền tóm tắt

| Tính năng | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|-------|------------|-------------|
| Xem danh sách users | ❌ | ✅ | ✅ |
| Tạo user | ✅ | ✅ | ✅ |
| Tạo/Sửa/Xóa course | ✅ | ✅ | ✅ |
| Quản lý Section/Lesson | ✅ | ❌ | ✅ |
| Duyệt course | ✅ | ❌ | ✅ |

> `ADMIN_USER` **không có quyền** thao tác Section/Lesson theo API docs. UI sẽ ẩn nút "Nội dung" với role này.

---

## Ghi chú kỹ thuật

- Section/Lesson API dùng `adminApi` (admin token) thay vì `api` (public token) để đảm bảo auth đúng
- Trang `AdminCourseContentPage` load sections kèm lessons (1 call `GET /courses/{id}/sections` trả về nested)
- Inline edit cho Section/Lesson (click vào tên để sửa trực tiếp) — UX tốt hơn modal
- Confirm dialog trước khi xóa Section (cảnh báo cascade xóa lessons)
- Phân trang users: debounce 300ms cho search input
- BE endpoint `GET /admin/users` cần `@PreAuthorize` với role `ADMIN_USER` hoặc `SUPER_ADMIN`
