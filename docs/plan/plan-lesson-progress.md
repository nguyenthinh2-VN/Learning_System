# Kế hoạch: Theo dõi tiến độ học (Lesson Progress)

## Tổng quan

Hiện member mua khóa học và xem được lesson (quyền truy cập đã siết theo enrollment), nhưng KHÔNG có cách:
1. Đánh dấu một lesson là "đã học xong".
2. Xem mình đã hoàn thành bao nhiêu % của một khóa học.
3. Tiếp tục học từ chỗ đang dở (resume).

Đây là tính năng LMS cốt lõi còn thiếu. `plan-my-enrollments.md` đã ghi nhận đây là "tính năng Theo dõi tiến độ trong tương lai" (ngoài phạm vi lúc đó).

Hạ tầng cần thiết ĐÃ có sẵn:
- `Enrollment` (quan hệ user ↔ course, kèm `findByUserId`, `existsByUserIdAndCourseId`).
- `CourseLesson` + `CourseLessonRepository.findBySectionId`.
- `LessonAuthorizationService.authorizeView` (kiểm soát ai được xem lesson của khóa học nào).
- Pattern `/me/*` trong `UserController`, `PageResult`, `ApiResponse`, Clean Architecture 4 tầng.

Tính năng này thêm bảng `lesson_progress` + endpoint đánh dấu hoàn thành/bỏ đánh dấu + endpoint tính % tiến độ.

---

## Phạm vi

### ✅ Trong phạm vi

| # | Endpoint | Mục đích |
|---|----------|----------|
| A | `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete` | Đánh dấu một lesson đã hoàn thành |
| B | `DELETE /api/v1/courses/{courseId}/lessons/{lessonId}/complete` | Bỏ đánh dấu (học lại) |
| C | `GET /api/v1/courses/{courseId}/progress` | Tiến độ tổng quan của khóa học: tổng lesson, số đã hoàn thành, %, danh sách lessonId đã hoàn thành |

### ❌ Ngoài phạm vi (cố ý)

- ❌ Theo dõi thời lượng xem video / vị trí giây đang xem (watch position) — chỉ làm "hoàn thành/chưa" ở mức lesson.
- ❌ Cấp chứng chỉ (certificate) khi đạt 100% — tính năng riêng sau này.
- ❌ Tự động đánh dấu hoàn thành khi xem hết video — chỉ làm đánh dấu thủ công (manual) trước.
- ❌ Tiến độ cho INSTRUCTOR/STAFF/ADMIN xem học viên nào học tới đâu — đây là analytics, làm riêng.
- ❌ Gắn % tiến độ vào response `GET /me/enrollments` — có thể thêm sau; giữ enrollment listing tối giản như hiện tại.

---

## Quy tắc nghiệp vụ

1. **Chỉ MEMBER đã enrolled mới được đánh dấu/xem tiến độ của khóa học đó.** Tái dùng `EnrollmentRepository.existsByUserIdAndCourseId`. Không enrolled → `403 LESSON_ACCESS_DENIED` (nhất quán với quyền xem lesson).
2. **Tiến độ thuộc về chính người gọi.** Xác định `userId` từ JWT, không nhận từ client. Một user chỉ thấy/sửa tiến độ của chính mình.
3. **Idempotent.** Đánh dấu hoàn thành một lesson đã hoàn thành rồi → không tạo bản ghi trùng (UNIQUE `(user_id, lesson_id)`), trả 200 bình thường. Bỏ đánh dấu lesson chưa đánh dấu → no-op, trả 200.
4. **Lesson phải thuộc course ở path.** Validate `lessonId` thuộc một section của `courseId`; nếu không → `404 LESSON_NOT_FOUND`. Chống đánh dấu lesson của khóa khác qua courseId mình đã mua.
5. **% tính theo tổng lesson hiện tại của khóa.** `progressPercent = round(completedCount / totalLessons × 100)`. Nếu `totalLessons = 0` → `progressPercent = 0` (tránh chia 0).
6. **Tiến độ không bị xóa khi unpublish course.** Giống enrollment, giữ lịch sử học.

---

## Thiết kế theo Clean Architecture

### 1. Domain Layer (`domain/`)

**`domain/model/LessonProgress.java`** (mới) — POJO thuần:
```java
public class LessonProgress {
    private Long id;
    private Long userId;
    private Long lessonId;
    private Long courseId;     // denormalize để query tiến độ theo course nhanh
    private LocalDateTime completedAt;

    public static LessonProgress create(Long userId, Long lessonId, Long courseId) { ... }
    public static LessonProgress reconstitute(...) { ... }
    // getters
}
```

> Lưu `courseId` trực tiếp trong bảng progress để đếm `completedCount` theo `(userId, courseId)` bằng một query, không phải join lesson→section→course.

### 2. Application Layer (`application/`)

**Repository port** (`application/repository/Course/LessonProgressRepository.java`):
```java
public interface LessonProgressRepository {
    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);
    void markComplete(LessonProgress progress);     // idempotent (insert-if-absent)
    void unmarkComplete(Long userId, Long lessonId);
    long countByUserIdAndCourseId(Long userId, Long courseId);
    List<Long> findCompletedLessonIds(Long userId, Long courseId);
}
```

**DTO** (`application/dto/Progress/`):
- `CourseProgressOutput(Long courseId, int totalLessons, int completedLessons, int progressPercent, List<Long> completedLessonIds)`.

**Use case** (`application/usecase/Progress/`):
- `MarkLessonCompleteUseCase.execute(userId, courseId, lessonId)`:
  - Load course (404 nếu thiếu) → check enrolled (403 nếu MEMBER chưa mua) → validate lesson thuộc course (404 nếu không) → `markComplete` idempotent.
  - `@Transactional`.
- `UnmarkLessonCompleteUseCase.execute(userId, courseId, lessonId)`: tương tự, gọi `unmarkComplete`. `@Transactional`.
- `GetCourseProgressUseCase.execute(userId, courseId) : CourseProgressOutput`:
  - Load course (404) → check enrolled (403) → đếm tổng lesson của course + `countByUserIdAndCourseId` → tính %. `@Transactional(readOnly = true)`.

> Cần một cách đếm tổng lesson của một course. Bổ sung `CourseLessonRepository.countByCourseId(Long courseId)` (JPQL join lesson→section→course) hoặc tái dùng cấu trúc section/lesson đã load. Ưu tiên thêm count query để tránh load toàn bộ lesson.

**Authorization**: tái dùng `LessonAuthorizationService.authorizeView(course, requesterId, role, isEnrolled)` cho cả 3 use case (member phải enrolled; staff/super_admin luôn được; instructor owner được; admin_user 403). Điều này giữ nhất quán với quyền xem lesson.

### 3. Adapter Layer (`adapter/`)

**JPA entity** (`adapter/repository/jpa/.../LessonProgressJpaEntity.java`):
- Bảng `lesson_progress`, cột `id, user_id, lesson_id, course_id, completed_at`.
- UNIQUE `(user_id, lesson_id)`; index `(user_id, course_id)`.

**Repository impl** + Spring Data JPA interface nội bộ (`JpaLessonProgressRepository`):
- `markComplete`: kiểm tra tồn tại trước, chỉ insert nếu chưa có (idempotent); dựa thêm UNIQUE constraint làm phòng tuyến cuối.
- `countByUserIdAndCourseId`, `findCompletedLessonIds` (chỉ trả lessonId).

**Request/Response DTO + Controller**:
- Endpoint A/B/C đặt ở controller mới `LessonProgressController` (`/api/v1/courses/{courseId}/...`) hoặc gộp vào `CourseLessonController` hiện có. Đề xuất: controller riêng `LessonProgressController` cho gọn trách nhiệm.
- `CourseProgressResponse` map từ `CourseProgressOutput`.
- Lấy `userId` + role từ JWT (pattern `getClaims` đã có).

### 4. Infrastructure Layer

- Không cần `ErrorCode` mới (tái dùng `LESSON_ACCESS_DENIED`, `LESSON_NOT_FOUND`, `COURSE_NOT_FOUND`).
- `SecurityConfig`: `/api/v1/courses/**` đã thuộc `authenticated()`; quyền chi tiết do use case raise. Không đổi.

---

## Data Model

### Bảng `lesson_progress` (mới)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | BIGINT PK | |
| user_id | BIGINT, index | FK → users.id |
| lesson_id | BIGINT | FK → lessons.id |
| course_id | BIGINT, index | denormalize để đếm nhanh |
| completed_at | DATETIME | thời điểm đánh dấu |
| | | UNIQUE `(user_id, lesson_id)` |

`ddl-auto: update` sẽ tạo bảng tự động.

---

## API (ghi vào `docs/API/`)

Thêm file `docs/API/progress.md` (hoặc mục trong `lesson.md`):
- `POST /courses/{courseId}/lessons/{lessonId}/complete` → 200 `{ completed: true }`.
- `DELETE /courses/{courseId}/lessons/{lessonId}/complete` → 200 `{ completed: false }`.
- `GET /courses/{courseId}/progress` → `{ courseId, totalLessons, completedLessons, progressPercent, completedLessonIds: [...] }`.
- Cập nhật `README.md` mục lục + `endpoints-summary.md`.

---

## Frontend

- `fe/src/api/` thêm `progress.js` (hoặc gộp vào `course.js`): `markLessonCompleteApi`, `unmarkLessonCompleteApi`, `getCourseProgressApi`.
- Trang học (course content / lesson viewer) — hiển thị:
  - Checkbox/nút "Đánh dấu đã học" trên mỗi lesson (toggle complete/uncomplete).
  - Thanh tiến độ % ở đầu khóa học (progress bar).
  - Icon ✓ cho các lesson đã hoàn thành trong danh sách section/lesson.
- (Tùy chọn) Trên `MyCoursesPage`, hiển thị % tiến độ mỗi khóa — cần gọi `GET /courses/{id}/progress` cho từng khóa hoặc thêm batch endpoint sau. Để sau nếu cần tối ưu.

---

## Kiểm thử

- `MarkLessonCompleteUseCaseTest`: member enrolled đánh dấu thành công; idempotent (đánh dấu 2 lần → 1 bản ghi); member chưa enrolled → `LessonAccessDeniedException`; lesson không thuộc course → `LessonNotFoundException`; course không tồn tại → `CourseNotFoundException`.
- `UnmarkLessonCompleteUseCaseTest`: bỏ đánh dấu thành công; bỏ đánh dấu lesson chưa đánh dấu → no-op không lỗi.
- `GetCourseProgressUseCaseTest`: 0 lesson → 0%; một phần hoàn thành tính % đúng (vd 3/4 = 75%); toàn bộ → 100%; member chưa enrolled → 403.
- Domain `LessonProgressTest`: `create` set đúng field, từ chối null.
- Chạy `mvnw test` xanh trước khi hoàn thành.

---

## Các file liên quan

### Tạo mới
- `domain/model/LessonProgress.java`
- `application/repository/Course/LessonProgressRepository.java`
- `application/dto/Progress/CourseProgressOutput.java`
- `application/usecase/Progress/MarkLessonCompleteUseCase.java`
- `application/usecase/Progress/UnmarkLessonCompleteUseCase.java`
- `application/usecase/Progress/GetCourseProgressUseCase.java`
- `adapter/repository/jpa/.../LessonProgressJpaEntity.java`
- `adapter/repository/LessonProgressRepositoryImpl.java`
- `adapter/repository/JpaLessonProgressRepository.java`
- `adapter/controller/Course/LessonProgressController.java`
- `adapter/dto/response/CourseProgressResponse.java`
- `src/test/.../MarkLessonCompleteUseCaseTest.java`, `UnmarkLessonCompleteUseCaseTest.java`, `GetCourseProgressUseCaseTest.java`
- `fe/src/api/progress.js` (hoặc bổ sung vào `course.js`)
- `docs/API/progress.md`

### Chỉnh sửa
- `application/repository/Course/CourseLessonRepository.java` (thêm `countByCourseId` + cách validate lesson thuộc course)
- Lesson viewer / course content page ở FE
- `docs/API/README.md`, `docs/API/endpoints-summary.md`

### Tham khảo (chỉ đọc)
- `application/usecase/Lesson/GetLessonsUseCase.java` (mẫu enrolled check + authorize)
- `domain/service/LessonAuthorizationService.java`
- `application/repository/Course/EnrollmentRepository.java`
