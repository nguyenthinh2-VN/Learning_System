# Kế hoạch: Quản lý Section (Course Section Management)

## 1. Tổng quan yêu cầu

Xây dựng CRUD cho **CourseSection** (Chương học) theo cấu trúc:
- `Course` → chứa nhiều → `Section` → chứa nhiều → `Lesson`

**Phân quyền:**
| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Xem sections của course | [x] | [x] | [x] | [x] | [x] |
| Tạo section | [ ] | [x] (chỉ course của mình) | [x] | [ ] | [x] |
| Sửa section | [ ] | [x] (chỉ course của mình) | [x] | [ ] | [x] |
| Xóa section | [ ] | [x] (chỉ course của mình) | [x] | [ ] | [x] |

> **Lưu ý:** `ADMIN_USER` **không có quyền** thao tác Section. Chỉ `INSTRUCTOR` (course của mình), `STAFF`, `SUPER_ADMIN`.

---

## 2. Cập nhật Permission Matrix

Thêm 2 permission mới vào `docs/permission-matrix.md`:

| # | Permission | Mô tả | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|---|-----------|-------|--------|------------|-------|------------|-------------|
| 13 | `CREATE_SECTION` | Tạo chương học trong khóa học | [ ] | [x] | [x] | [ ] | [x] |
| 14 | `EDIT_SECTION` | Sửa / Xóa chương học | [ ] | [x] | [x] | [ ] | [x] |

> Các permission hiện có (`CREATE_COURSE`, `EDIT_COURSE`, `DELETE_COURSE`) dùng cho Course-level, không bao gồm Section.

**Cập nhật `DataInitializer`:** Seed thêm 2 permission mới + gán vào đúng role.

---

## 3. API Endpoints

Base URL: `/api/v1/courses/{courseId}/sections`

| Method | URL | Mô tả | Permission |
|--------|-----|-------|------------|
| `GET` | `/api/v1/courses/{courseId}/sections` | Lấy danh sách sections (kèm lessons) | `VIEW_COURSE` |
| `POST` | `/api/v1/courses/{courseId}/sections` | Tạo section mới | `CREATE_SECTION` |
| `PUT` | `/api/v1/courses/{courseId}/sections/{sectionId}` | Cập nhật section | `EDIT_SECTION` |
| `DELETE` | `/api/v1/courses/{courseId}/sections/{sectionId}` | Xóa section | `EDIT_SECTION` |
| `PATCH` | `/api/v1/courses/{courseId}/sections/reorder` | Đổi thứ tự sections | `EDIT_SECTION` |

---

## 4. Các file cần tạo mới

### 4A. Domain Layer — không cần tạo thêm
- `CourseSection` và `CourseLesson` đã có sẵn và đầy đủ.
- Cần thêm method `updateSection(title, orderIndex)` vào `CourseSection` để hỗ trợ update.

### 4B. Domain Exception
```
domain/exception/
  └── SectionNotFoundException.java       (NEW)
  └── SectionAccessDeniedException.java   (NEW)
```

### 4C. Domain Service
```
domain/service/
  └── SectionAuthorizationService.java    (NEW)
```
Logic: Kiểm tra người dùng có quyền tạo/sửa/xóa section không.
- `INSTRUCTOR`: chỉ được thao tác section thuộc course mà `instructorId == requesterId`
- `STAFF` / `SUPER_ADMIN`: toàn quyền
- `ADMIN_USER` / `MEMBER`: ném `SectionAccessDeniedException`

### 4D. Application Layer — Repository Interface
```
application/repository/Course/
  └── CourseSectionRepository.java        (NEW)
```
Methods:
```java
Optional<CourseSection> findById(Long id);
List<CourseSection> findByCourseId(Long courseId);
CourseSection save(CourseSection section, Long courseId);
void deleteById(Long id);
boolean existsByCourseIdAndOrderIndex(Long courseId, int orderIndex);
```

### 4E. Application Layer — DTOs
```
application/dto/Section/
  └── CreateSectionInput.java             (NEW)  — record(courseId, requesterId, requesterRole, title, orderIndex)
  └── UpdateSectionInput.java             (NEW)  — record(sectionId, courseId, requesterId, requesterRole, title, orderIndex)
  └── DeleteSectionInput.java             (NEW)  — record(sectionId, courseId, requesterId, requesterRole)
  └── SectionOutput.java                  (NEW)  — record(id, title, orderIndex, List<LessonOutput>)
  └── LessonOutput.java                   (NEW)  — record(id, title, contentUrl, orderIndex)
```

### 4F. Application Layer — UseCases
```
application/usecase/Section/
  └── CreateSectionUseCase.java           (NEW)
  └── UpdateSectionUseCase.java           (NEW)
  └── DeleteSectionUseCase.java           (NEW)
  └── GetSectionsUseCase.java             (NEW)
```

### 4G. Adapter Layer — JPA
- `CourseSectionJpaEntity` đã có, cần thêm:
  - `toDomain()` method (hiện chưa có, mapping đang nằm trong `CourseMapper`)
  - `fromDomain()` static method

### 4H. Adapter Layer — Repository Implementation
```
adapter/repository/
  └── CourseSectionRepositoryImpl.java    (NEW)
  └── JpaCourseSectionRepository.java     (NEW — Spring Data interface, package-private)
```

### 4I. Adapter Layer — Request/Response DTOs
```
adapter/dto/request/Section/
  └── CreateSectionRequest.java           (NEW)  — @NotBlank title, @Min(0) orderIndex
  └── UpdateSectionRequest.java           (NEW)  — @NotBlank title, @Min(0) orderIndex

adapter/dto/response/
  └── SectionResponse.java                (NEW)  — id, title, orderIndex, List<LessonResponse>
  └── LessonResponse.java                 (NEW)  — id, title, contentUrl, orderIndex
```

### 4J. Adapter Layer — Controller
```
adapter/controller/Course/
  └── CourseSectionController.java        (NEW)
```
Dùng `@PreAuthorize` với permission check:
- `GET` → `hasAnyRole('MEMBER','INSTRUCTOR','STAFF','ADMIN_USER','SUPER_ADMIN')`
- `POST` → `hasAnyRole('INSTRUCTOR','STAFF','SUPER_ADMIN')`
- `PUT` / `DELETE` / `PATCH` → `hasAnyRole('INSTRUCTOR','STAFF','SUPER_ADMIN')`

### 4K. Infrastructure — Exception Handler
Thêm 2 handler mới vào `GlobalExceptionHandler`:
- `SectionNotFoundException` → 404
- `SectionAccessDeniedException` → 403

Thêm 2 error code vào `ErrorCode` enum:
- `SECTION_NOT_FOUND`
- `SECTION_ACCESS_DENIED`

---

## 5. Luồng xử lý chi tiết

### POST /courses/{courseId}/sections (Tạo section)
```
Request → CourseSectionController
  → parse JWT → lấy requesterId, requesterRole
  → CreateSectionRequest.toInput()
  → CreateSectionUseCase.execute(input)
    → CourseRepository.findById(courseId)  // kiểm tra course tồn tại
    → SectionAuthorizationService.authorizeCreate(course, requesterId, requesterRole)
    → CourseSection.create(title, orderIndex, [])
    → CourseSectionRepository.save(section, courseId)
    → return SectionOutput
  → SectionResponse.from(output)
  → 201 Created
```

### PUT /courses/{courseId}/sections/{sectionId} (Sửa section)
```
Request → CourseSectionController
  → UpdateSectionUseCase.execute(input)
    → CourseSectionRepository.findById(sectionId)  // kiểm tra section tồn tại
    → CourseRepository.findById(courseId)           // kiểm tra course tồn tại + lấy instructorId
    → SectionAuthorizationService.authorizeEditOrDelete(course, requesterId, requesterRole)
    → CourseSection.reconstitute(id, newTitle, newOrderIndex, existingLessons)
    → CourseSectionRepository.save(section, courseId)
    → return SectionOutput
```

### DELETE /courses/{courseId}/sections/{sectionId} (Xóa section)
```
Request → CourseSectionController
  → DeleteSectionUseCase.execute(input)
    → CourseSectionRepository.findById(sectionId)
    → CourseRepository.findById(courseId)
    → SectionAuthorizationService.authorizeEditOrDelete(course, requesterId, requesterRole)
    → CourseSectionRepository.deleteById(sectionId)
    → return void
```

---

## 6. Thứ tự implement

1. **Domain exceptions** — `SectionNotFoundException`, `SectionAccessDeniedException`
2. **Domain service** — `SectionAuthorizationService`
3. **Application DTOs** — `CreateSectionInput`, `UpdateSectionInput`, `DeleteSectionInput`, `SectionOutput`, `LessonOutput`
4. **Application repository interface** — `CourseSectionRepository`
5. **Application UseCases** — `GetSectionsUseCase`, `CreateSectionUseCase`, `UpdateSectionUseCase`, `DeleteSectionUseCase`
6. **Adapter JPA** — `JpaCourseSectionRepository`
7. **Adapter repository impl** — `CourseSectionRepositoryImpl`
8. **Adapter request/response DTOs** — `CreateSectionRequest`, `UpdateSectionRequest`, `SectionResponse`, `LessonResponse`
9. **Adapter controller** — `CourseSectionController`
10. **Infrastructure** — thêm `ErrorCode`, `GlobalExceptionHandler` handlers
11. **DataInitializer** — seed `CREATE_SECTION`, `EDIT_SECTION` permissions + gán role
12. **Permission matrix** — cập nhật `docs/permission-matrix.md`

---

## 7. Quyết định thiết kế cần xác nhận

- [ ] **Reorder API**: Có cần endpoint `PATCH /sections/reorder` để đổi thứ tự không, hay chỉ cần sửa `orderIndex` qua `PUT`?
- [ ] **Cascade delete**: Khi xóa section, tự động xóa tất cả lessons bên trong không? (Hiện `CourseSectionJpaEntity` đã có `orphanRemoval = true` → mặc định là có)
- [ ] **Duplicate orderIndex**: Có cần validate không cho phép 2 sections cùng `orderIndex` trong 1 course không?
- [ ] **Section khi tạo Course**: Hiện tại `CreateCourseUseCase` đã cho phép tạo sections cùng lúc với course. Sau khi có API riêng, có giữ lại tính năng này không?

---

## 8. Ghi chú kỹ thuật

- `CourseSectionJpaEntity` đã có `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` → xóa section sẽ tự xóa lessons.
- `CourseJpaEntity` đã có `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` → không cần thêm gì.
- `CourseMapper` hiện đang map section/lesson thủ công — sẽ tái sử dụng logic này trong `CourseSectionRepositoryImpl`.
- Không dùng `CourseRepository.save(course)` để thêm section (tránh load toàn bộ course + sections vào memory) — thay vào đó dùng `CourseSectionRepository` riêng để thao tác trực tiếp trên bảng `course_sections`.
