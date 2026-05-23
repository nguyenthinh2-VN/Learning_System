# Kế hoạch: Quản lý Lesson (Course Lesson Management)

## 1. Tổng quan yêu cầu

Xây dựng CRUD cho **CourseLesson** (Bài giảng) theo cấu trúc:
- `Course` → chứa nhiều → `Section` → chứa nhiều → `Lesson`

**Phân quyền:**
| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Xem lessons của section | [x] | [x] | [x] | [x] | [x] |
| Tạo lesson | [ ] | [x] (chỉ section của course mình) | [x] | [ ] | [x] |
| Sửa lesson | [ ] | [x] (chỉ section của course mình) | [x] | [ ] | [x] |
| Xóa lesson | [ ] | [x] (chỉ section của course mình) | [x] | [ ] | [x] |

> **Lưu ý:** `ADMIN_USER` **không có quyền** thao tác Lesson. Chỉ `INSTRUCTOR` (course của mình), `STAFF`, `SUPER_ADMIN`.

---

## 2. Cập nhật Permission Matrix

Thêm 2 permission mới vào `docs/permission-matrix.md`:

| # | Permission | Mô tả | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|---|-----------|-------|--------|------------|-------|------------|-------------|
| 15 | `CREATE_LESSON` | Tạo bài giảng trong chương học | [ ] | [x] | [x] | [ ] | [x] |
| 16 | `EDIT_LESSON` | Sửa / Xóa bài giảng | [ ] | [x] | [x] | [ ] | [x] |

> Các permission hiện có (`CREATE_SECTION`, `EDIT_SECTION`) dùng cho Section-level, không bao gồm Lesson.

**Cập nhật `DataInitializer`:** Seed thêm 2 permission mới + gán vào đúng role.

---

## 3. API Endpoints

Base URL: `/api/v1/courses/{courseId}/sections/{sectionId}/lessons`

| Method | URL | Mô tả | Permission |
|--------|-----|-------|------------|
| `GET` | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons` | Lấy danh sách lessons | `VIEW_COURSE` |
| `POST` | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons` | Tạo lesson mới | `CREATE_LESSON` |
| `PUT` | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` | Cập nhật lesson | `EDIT_LESSON` |
| `DELETE` | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` | Xóa lesson | `EDIT_LESSON` |
| `PATCH` | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/reorder` | Đổi thứ tự lessons | `EDIT_LESSON` |

---

## 4. Các file cần tạo mới

### 4A. Domain Layer — không cần tạo thêm
- `CourseLesson` đã có sẵn và đầy đủ.
- Cần thêm method `updateLesson(title, contentUrl, orderIndex)` vào `CourseLesson` để hỗ trợ update.

### 4B. Domain Exception
```
domain/exception/
  └── LessonNotFoundException.java       (NEW)
  └── LessonAccessDeniedException.java   (NEW)
```

### 4C. Domain Service
```
domain/service/
  └── LessonAuthorizationService.java    (NEW)
```
Logic: Kiểm tra người dùng có quyền tạo/sửa/xóa lesson không.
- `INSTRUCTOR`: chỉ được thao tác lesson thuộc section mà `instructorId == requesterId`
- `STAFF` / `SUPER_ADMIN`: toàn quyền
- `ADMIN_USER` / `MEMBER`: ném `LessonAccessDeniedException`

### 4D. Application Layer — Repository Interface
```
application/repository/Course/
  └── CourseLessonRepository.java        (NEW)
```
Methods:
```java
Optional<CourseLesson> findById(Long id);
List<CourseLesson> findBySectionId(Long sectionId);
CourseLesson save(CourseLesson lesson, Long sectionId);
void deleteById(Long id);
boolean existsBySectionIdAndOrderIndex(Long sectionId, int orderIndex);
```

### 4E. Application Layer — DTOs
```
application/dto/Lesson/
  └── CreateLessonInput.java             (NEW)  — record(sectionId, courseId, requesterId, requesterRole, title, contentUrl, orderIndex)
  └── UpdateLessonInput.java             (NEW)  — record(lessonId, sectionId, courseId, requesterId, requesterRole, title, contentUrl, orderIndex)
  └── DeleteLessonInput.java             (NEW)  — record(lessonId, sectionId, courseId, requesterId, requesterRole)
  └── LessonOutput.java                  (EXISTING - cần mở rộng) — record(id, title, contentUrl, orderIndex, sectionId)
```

### 4F. Application Layer — UseCases
```
application/usecase/Lesson/
  └── CreateLessonUseCase.java           (NEW)
  └── UpdateLessonUseCase.java           (NEW)
  └── DeleteLessonUseCase.java           (NEW)
  └── GetLessonsUseCase.java             (NEW)
```

### 4G. Adapter Layer — JPA
- `CourseLessonJpaEntity` đã có, cần thêm:
  - `toDomain()` method (hiện đã có)
  - `fromDomain()` static method (hiện đã có)

### 4H. Adapter Layer — Repository Implementation
```
adapter/repository/
  └── CourseLessonRepositoryImpl.java    (NEW)
  └── JpaCourseLessonRepository.java     (NEW — Spring Data interface, package-private)
```

### 4I. Adapter Layer — Request/Response DTOs
```
adapter/dto/request/Lesson/
  └── CreateLessonRequest.java           (NEW)  — @NotBlank title, @NotBlank contentUrl, @Min(0) orderIndex
  └── UpdateLessonRequest.java           (NEW)  — @NotBlank title, @NotBlank contentUrl, @Min(0) orderIndex

adapter/dto/response/
  └── LessonResponse.java                (EXISTING - cần mở rộng) — id, title, contentUrl, orderIndex, sectionId
```

### 4J. Adapter Layer — Controller
```
adapter/controller/Course/
  └── CourseLessonController.java        (NEW)
```
Dùng `@PreAuthorize` với permission check:
- `GET` → `hasAnyRole('MEMBER','INSTRUCTOR','STAFF','ADMIN_USER','SUPER_ADMIN')`
- `POST` → `hasAnyRole('INSTRUCTOR','STAFF','SUPER_ADMIN')`
- `PUT` / `DELETE` / `PATCH` → `hasAnyRole('INSTRUCTOR','STAFF','SUPER_ADMIN')`

### 4K. Infrastructure — Exception Handler
Thêm 2 handler mới vào `GlobalExceptionHandler`:
- `LessonNotFoundException` → 404
- `LessonAccessDeniedException` → 403

Thêm 2 error code vào `ErrorCode` enum:
- `LESSON_NOT_FOUND`
- `LESSON_ACCESS_DENIED`

---

## 5. Luồng xử lý chi tiết

### POST /courses/{courseId}/sections/{sectionId}/lessons (Tạo lesson)
```
Request → CourseLessonController
  → parse JWT → lấy requesterId, requesterRole
  → CreateLessonRequest.toInput()
  → CreateLessonUseCase.execute(input)
    → CourseRepository.findById(courseId)  // kiểm tra course tồn tại
    → CourseSectionRepository.findById(sectionId)  // kiểm tra section tồn tại
    → LessonAuthorizationService.authorizeCreate(course, requesterId, requesterRole)
    → CourseLesson.create(title, contentUrl, orderIndex)
    → CourseLessonRepository.save(lesson, sectionId)
    → return LessonOutput
  → LessonResponse.from(output)
  → 201 Created
```

### PUT /courses/{courseId}/sections/{sectionId}/lessons/{lessonId} (Sửa lesson)
```
Request → CourseLessonController
  → UpdateLessonUseCase.execute(input)
    → CourseLessonRepository.findById(lessonId)  // kiểm tra lesson tồn tại
    → CourseSectionRepository.findById(sectionId)  // kiểm tra section tồn tại
    → CourseRepository.findById(courseId)  // kiểm tra course tồn tại + lấy instructorId
    → LessonAuthorizationService.authorizeEditOrDelete(course, requesterId, requesterRole)
    → CourseLesson.reconstitute(id, newTitle, newContentUrl, newOrderIndex)
    → CourseLessonRepository.save(lesson, sectionId)
    → return LessonOutput
```

### DELETE /courses/{courseId}/sections/{sectionId}/lessons/{lessonId} (Xóa lesson)
```
Request → CourseLessonController
  → DeleteLessonUseCase.execute(input)
    → CourseLessonRepository.findById(lessonId)
    → CourseSectionRepository.findById(sectionId)
    → CourseRepository.findById(courseId)
    → LessonAuthorizationService.authorizeEditOrDelete(course, requesterId, requesterRole)
    → CourseLessonRepository.deleteById(lessonId)
    → return void
```

---

## 6. Thứ tự implement

1. **Domain exceptions** — `LessonNotFoundException`, `LessonAccessDeniedException`
2. **Domain service** — `LessonAuthorizationService`
3. **Application DTOs** — `CreateLessonInput`, `UpdateLessonInput`, `DeleteLessonInput`, mở rộng `LessonOutput`
4. **Application repository interface** — `CourseLessonRepository`
5. **Application UseCases** — `GetLessonsUseCase`, `CreateLessonUseCase`, `UpdateLessonUseCase`, `DeleteLessonUseCase`
6. **Adapter JPA** — `JpaCourseLessonRepository`
7. **Adapter repository impl** — `CourseLessonRepositoryImpl`
8. **Adapter request/response DTOs** — `CreateLessonRequest`, `UpdateLessonRequest`, mở rộng `LessonResponse`
9. **Adapter controller** — `CourseLessonController`
10. **Infrastructure** — thêm `ErrorCode`, `GlobalExceptionHandler` handlers
11. **DataInitializer** — seed `CREATE_LESSON`, `EDIT_LESSON` permissions + gán role
12. **Permission matrix** — cập nhật `docs/permission-matrix.md`

---

## 7. Quyết định thiết kế cần xác nhận

- [ ] **Reorder API**: Có cần endpoint `PATCH /lessons/reorder` để đổi thứ tự không, hay chỉ cần sửa `orderIndex` qua `PUT`?
- [ ] **Duplicate orderIndex**: Có cần validate không cho phép 2 lessons cùng `orderIndex` trong 1 section không?
- [ ] **Content URL validation**: Có cần validate format của `contentUrl` (YouTube, Vimeo, S3 URL) không?
- [ ] **Lesson khi tạo Section**: Hiện tại `CreateSectionUseCase` đã cho phép tạo lessons cùng lúc với section. Sau khi có API riêng, có giữ lại tính năng này không?

---

## 8. Ghi chú kỹ thuật

- `CourseSectionJpaEntity` đã có `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` → xóa section sẽ tự xóa lessons.
- `CourseLessonJpaEntity` đã có `toDomain()` và `fromDomain()` method.
- `CourseMapper` hiện đang map lesson thủ công — sẽ tái sử dụng logic này trong `CourseLessonRepositoryImpl`.
- Không dùng `CourseSectionRepository.save(section)` để thêm lesson (tránh load toàn bộ section + lessons vào memory) — thay vào đó dùng `CourseLessonRepository` riêng để thao tác trực tiếp trên bảng `course_lessons`.
- **Reuse existing patterns**: Sẽ follow chính xác pattern của Section Management đã triển khai thành công.