# Learning System - Project Overview & Architecture

Đây là tài liệu tổng hợp toàn bộ cấu trúc dự án, kiến trúc thiết kế, và các tính năng đã được triển khai. Tài liệu này được tạo ra để giúp các AI Agent hoặc Developer mới nhanh chóng nắm bắt context của toàn bộ dự án Spring Boot.

---

## 1. Công nghệ & Kiến trúc tổng thể

- **Ngôn ngữ & Framework:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA.
- **Database:** MySQL.
- **Kiến trúc:** Clean Architecture / Hexagonal Architecture. Tuân thủ nghiêm ngặt nguyên tắc SOLID và Design Patterns.
- **Quản lý dependencies:** Maven.
- **Authentication:** Stateless JWT Token.

---

## 2. Các Tính năng đã hoàn thiện

### 2.1. Authentication & User Management
- Đăng ký tài khoản (Register) với mặc định Role `MEMBER`.
- Đăng nhập (Login) bằng `email` hoặc `username`. Trả về JWT Token (24h).
- **Strategy Pattern** cho việc tạo Username ngẫu nhiên tùy theo Role (VD: `MEM...`, `GV...`, `ST...`, `AD...`).
- Admin có thể tạo tài khoản cho nhân sự nội bộ (Instructor, Staff).

### 2.2. Phân quyền (Roles & Permissions)
- Thiết lập Role-Based Access Control (RBAC) linh hoạt.
- **Roles:** `MEMBER`, `INSTRUCTOR`, `STAFF`, `ADMIN_USER`, `SUPER_ADMIN`.
- Cấu trúc bảo mật lồng ghép trong `SecurityConfig` và kiểm tra quyền cụ thể tại UseCase / Controller.
- Permissions được seed tự động qua `DataInitializer` khi khởi động lần đầu.

### 2.3. Course Management (Quản lý Khóa học)
- CRUD Khóa học (Tạo, Sửa, Xóa, Xem danh sách phân trang, Xem chi tiết).
- **Strategy Pattern (`CourseStrategyFactory`)** để kiểm soát quyền sửa/xóa/tạo khóa học:
  - `InstructorCourseStrategy`: Giảng viên chỉ thao tác được khóa học của chính mình.
  - `StaffAdminCourseStrategy`: Admin/Staff thao tác được toàn quyền khóa học của người khác.

### 2.4. Wallet, Monetization & Checkout (Nạp ví & Thanh toán)
- Quản lý số dư (`balance`) bằng `BigDecimal` trong Domain `User`.
- `TopUpBalanceUseCase`: Nạp tiền vào ví. Sử dụng Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) để chống Race Condition khi nạp tiền đồng thời.
- `PurchaseCourseUseCase`: Mua khóa học.
  - Kiểm tra `isInternal` (Nội bộ thì giá mua = 0đ).
  - Trừ tiền an toàn với DB Lock.
  - Tạo Record vào bảng `enrollments` lưu lại khóa học và số tiền đã thanh toán (`paid_price`).
- **Audit Logging:** Ghi log giao dịch Append-only định dạng JSONL vào File Local (`logs/purchase_ledger.jsonl`) để đề phòng mất mát dữ liệu DB.

### 2.5. Course Section Management (Quản lý Chương học)
- CRUD Section (Tạo, Sửa, Xóa, Xem danh sách kèm Lessons) theo cấu trúc phân cấp: `Course → Section → Lesson`.
- **Phân quyền Section tách biệt với Course:** `ADMIN_USER` có quyền sửa/xóa Course nhưng **không** có quyền thao tác Section. Chỉ `INSTRUCTOR` (course của mình), `STAFF`, `SUPER_ADMIN`.
- **`CourseOwnershipPolicy`** — Pure static policy class ở Domain layer, tập trung toàn bộ logic kiểm tra ownership (`isOwner`, `hasFullAccess`, `hasFullCourseAccess`, `isInstructorOwner`). Cả `CourseAuthorizationService` và `SectionAuthorizationService` đều gọi vào đây, tránh lặp code.
- Cascade delete: Xóa Section tự động xóa toàn bộ Lessons bên trong (`orphanRemoval = true`).
- Thêm 2 permissions mới: `CREATE_SECTION`, `EDIT_SECTION` — đã seed vào `DataInitializer`.

### 2.6. Course Lesson Management (Quản lý Bài giảng) - **MỚI HOÀN THIỆN**
- CRUD Lesson (Tạo, Sửa, Xóa, Xem danh sách) theo cấu trúc phân cấp: `Course → Section → Lesson`.
- **Phân quyền Lesson tương tự Section:** `ADMIN_USER` **không có quyền** thao tác Lesson. Chỉ `INSTRUCTOR` (course của mình), `STAFF`, `SUPER_ADMIN`.
- **`LessonAuthorizationService`** — Pure static utility class ở Domain layer, tái sử dụng logic từ `CourseOwnershipPolicy` để kiểm tra quyền trên Lesson.
- **Pattern consistency:** Follow chính xác pattern của Section Management đã triển khai thành công.
- Thêm 2 permissions mới: `CREATE_LESSON`, `EDIT_LESSON` — đã seed vào `DataInitializer` và gán đúng role theo permission matrix.

---

## 3. Cấu trúc thư mục (Directory Tree)

Cấu trúc phân lớp theo **Clean Architecture**, mỗi lớp tuân thủ chặt chẽ Dependency Rule (Dependency chỉ hướng vào trong Domain).

```text
src/main/java/com/example/learning_system_spring
|-- LearningSystemSpringApplication.java (Main class)
|
|-- adapter/  (Lớp ngoài cùng: Controllers, DTOs, DB Repositories, Mappers)
|   |-- controller/           # REST APIs
|   |   |-- Auth/             # AuthController
|   |   |-- Course/           # CourseController, CourseSectionController
|   |   |-- AdminUserController, UserController
|   |-- dto/
|   |   |-- request/          # Request DTOs (CreateCourseRequest, CreateSectionRequest...)
|   |   |-- response/         # Response DTOs (ApiResponse, CourseDetailResponse, SectionResponse...)
|   |-- mapper/               # CourseMapper (JPA Entity ↔ Domain)
|   |-- repository/           # Repository Impl (gọi tới Spring Data JPA)
|   |   |-- jpa/
|   |       |-- CourseEntity/ # CourseJpaEntity, CourseSectionJpaEntity, CourseLessonJpaEntity, EnrollmentJpaEntity
|   |       |-- UserEntity/   # UserJpaEntity
|   |       |-- role_permissionEntity/ # RoleJpaEntity, PermissionJpaEntity, RolePermissionJpaEntity
|
|-- application/ (Lớp Use Case: Chứa logic nghiệp vụ ứng dụng, độc lập với Framework Web/DB)
|   |-- dto/                  # Application DTOs (Input/Output của UseCase)
|   |   |-- Auth/             # LoginInput, LoginOutput, RegisterOutput
|   |   |-- Course/           # CreateCourseInput, CourseOutput, CourseSectionDto...
|   |   |-- Section/          # CreateSectionInput, UpdateSectionInput, SectionOutput, LessonOutput
|   |   |-- Lesson/           # CreateLessonInput, UpdateLessonInput, DeleteLessonInput, GetLessonsInput, GetLessonsOutput
|   |-- repository/           # Repository Interfaces (Adapter implement)
|   |   |-- Course/           # CourseRepository, EnrollmentRepository, CourseSectionRepository, CourseLessonRepository
|   |   |-- User/             # UserRepository
|   |   |-- RoleRepository
|   |-- usecase/
|       |-- Auth/             # LoginUseCase, RegisterUseCase
|       |-- Course/           # CreateCourse, UpdateCourse, DeleteCourse, GetCourseList, GetCourseDetail, PurchaseCourse
|       |-- Section/          # GetSections, CreateSection, UpdateSection, DeleteSection
|       |-- Lesson/           # GetLessonsUseCase, CreateLessonUseCase, UpdateLessonUseCase, DeleteLessonUseCase
|       |-- User/             # TopUpBalanceUseCase, AdminCreateUserUseCase
|       |-- strategy/         # CourseStrategyFactory, InstructorCourseStrategy, StaffAdminCourseStrategy
|                             # UsernameGeneratorFactory, *UsernameGeneratorStrategy
|
|-- domain/ (Lớp Lõi: Chứa Model nghiệp vụ tinh khiết, Business rules)
|   |-- exception/            # SectionNotFoundException, SectionAccessDeniedException,
|   |                         # LessonNotFoundException, LessonAccessDeniedException,
|   |                         # CourseNotFoundException, CourseAccessDeniedException,
|   |                         # UserNotFoundException, InvalidCredentialsException...
|   |-- model/                # User, Course, CourseSection, CourseLesson, Enrollment, Role, Permission
|   |-- service/
|       |-- CourseOwnershipPolicy       # Pure static policy — kiểm tra ownership, không phụ thuộc Spring
|       |-- CourseAuthorizationService  # Kiểm quyền Course (gọi CourseOwnershipPolicy)
|       |-- SectionAuthorizationService # Kiểm quyền Section (gọi CourseOwnershipPolicy)
|       |-- LessonAuthorizationService  # Kiểm quyền Lesson (gọi CourseOwnershipPolicy)
|
|-- infrastructure/ (Lớp Cơ sở hạ tầng: Cấu hình Framework, Utils, External Services)
    |-- config/               # SecurityConfig, JwtFilter, JwtService, DataInitializer
    |-- exception/            # GlobalExceptionHandler, ErrorResponse, ErrorCode (đã thêm LESSON_NOT_FOUND, LESSON_ACCESS_DENIED)
    |-- service/              # PurchaseLedgerService (ghi audit log JSONL)
```

---

## 4. Database Schema (Các bảng cốt lõi)

- **`users`**: Quản lý tài khoản, mật khẩu (Bcrypt), số dư `balance` và cờ `is_internal`.
- **`roles` / `permissions` / `role_permissions`**: Cấu trúc RBAC n-n để cấp quyền linh hoạt.
- **`courses`**: Thông tin khóa học, giá `price`, số lượng học viên `max_students`, `instructor_id`...
- **`course_sections`**: Chương học, quan hệ N-1 với `courses`. Có `order_index` để sắp xếp thứ tự.
- **`course_lessons`**: Bài giảng, quan hệ N-1 với `course_sections`. Có `content_url` và `order_index`.
- **`enrollments`**: Lưu lịch sử mua khóa học. Chứa `user_id`, `course_id`, `paid_price` và ngày đăng ký.

---

## 5. Design Patterns đang sử dụng

| Pattern | Nơi áp dụng | Mục đích |
|---------|-------------|---------|
| **Strategy** | `CourseStrategyFactory` + `InstructorCourseStrategy` / `StaffAdminCourseStrategy` | Phân quyền tạo/sửa/xóa Course theo Role |
| **Strategy** | `UsernameGeneratorFactory` + các `*UsernameGeneratorStrategy` | Sinh Username tự động theo Role prefix |
| **Policy (Static)** | `CourseOwnershipPolicy` | Tập trung logic kiểm tra ownership, tái sử dụng ở nhiều Service |
| **Repository** | `CourseRepository`, `UserRepository`, `CourseSectionRepository`, `CourseLessonRepository`... | Abstraction giữa UseCase và DB |
| **Factory** | `CourseStrategyFactory`, `UsernameGeneratorFactory` | Chọn Strategy phù hợp theo Role |
| **Consistent Layering** | Section & Lesson Management | Follow chính xác pattern của nhau, đảm bảo consistency trong kiến trúc |

---

## 6. Danh sách API Endpoints

| Method | URL | Mô tả | Quyền |
|--------|-----|-------|-------|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản | Public |
| POST | `/api/v1/auth/login` | Đăng nhập, lấy JWT | Public |
| POST | `/api/v1/admin/users` | Admin tạo tài khoản nội bộ | ADMIN_USER, STAFF, SUPER_ADMIN |
| GET | `/api/v1/courses` | Danh sách khóa học (phân trang) | Tất cả |
| GET | `/api/v1/courses/{id}` | Chi tiết khóa học | Tất cả |
| POST | `/api/v1/courses` | Tạo khóa học | INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN |
| PUT | `/api/v1/courses/{id}` | Sửa khóa học | INSTRUCTOR (của mình), STAFF, ADMIN_USER, SUPER_ADMIN |
| DELETE | `/api/v1/courses/{id}` | Xóa khóa học | INSTRUCTOR (của mình), STAFF, ADMIN_USER, SUPER_ADMIN |
| POST | `/api/v1/courses/{id}/purchase` | Mua khóa học | Đăng nhập |
| POST | `/api/v1/users/me/top-up` | Nạp tiền vào ví | Đăng nhập |
| GET | `/api/v1/courses/{courseId}/sections` | Danh sách sections (kèm lessons) | Tất cả |
| POST | `/api/v1/courses/{courseId}/sections` | Tạo section | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |
| PUT | `/api/v1/courses/{courseId}/sections/{id}` | Sửa section | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |
| DELETE | `/api/v1/courses/{courseId}/sections/{id}` | Xóa section (cascade xóa lessons) | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |
| GET | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons` | Danh sách lessons | Tất cả |
| POST | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons` | Tạo lesson | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |
| PUT | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` | Sửa lesson | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |
| DELETE | `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}` | Xóa lesson | INSTRUCTOR (của mình), STAFF, SUPER_ADMIN |

> Chi tiết request/response xem tại `docs/api-docs.md`. Ma trận phân quyền xem tại `docs/permission-matrix.md`.

---

## 7. Quy tắc chung khi tiếp tục phát triển (For AI Agents)

1. **Clean Architecture Strictness:** Model trong `domain/model` tuyệt đối **không** dùng bất kỳ annotation của JPA hay Spring nào. JPA Entity phải nằm ở `adapter/repository/jpa`. Chuyển đổi giữa 2 dạng này thông qua các phương thức `toDomain()` và `fromDomain()`.
2. **Business Logic Location:** Logic nghiệp vụ chính (kiểm tra điều kiện, thao tác tiền) luôn nằm ở `application/usecase`.
3. **Concurrency:** Đối với các API liên quan đến tài chính (Nạp ví, Mua khóa học), luôn gọi Repository với `@Lock(LockModeType.PESSIMISTIC_WRITE)` để khóa Row DB, ngăn Race Condition.
4. **Exception Handling:** Ném ra Domain Exception (kế thừa `RuntimeException` hoặc `IllegalStateException`) trong UseCase. Sau đó, `GlobalExceptionHandler` ở layer Infrastructure sẽ tự động catch và map ra API Response format thống nhất (VD: HTTP 400 kèm mã `BAD_REQUEST`).
5. **Authorization Pattern:** Mọi logic kiểm tra ownership/quyền trên Course đều đi qua `CourseOwnershipPolicy` (static methods). Không viết lại điều kiện `instructorId.equals(requesterId)` ở nhiều nơi.
6. **Permission Seeding:** Khi thêm permission mới, cập nhật đồng thời 3 nơi: `DataInitializer` (seed DB), `docs/permission-matrix.md` (ma trận phân quyền), `GlobalExceptionHandler` + `ErrorCode` (nếu có exception mới).
7. **Constructor Injection Only:** Tuyệt đối cấm `@Autowired` field/setter injection. Chỉ dùng constructor injection hoặc Lombok `@RequiredArgsConstructor`.
8. **Tooling & Code Edits:** Thay thế nội dung file bằng công cụ `str_replace` với matching chính xác.


---

## 8. Cập nhật mới nhất (23/05/2026) - Hoàn thiện Lesson Management

### 8.1. Những gì đã được triển khai

**✅ Đã hoàn thiện CRUD cho Course Lesson Management:**

1. **Domain Layer:**
   - Thêm 2 domain exceptions: `LessonNotFoundException`, `LessonAccessDeniedException`
   - Thêm `LessonAuthorizationService` - pure static utility class tái sử dụng `CourseOwnershipPolicy`

2. **Application Layer:**
   - Thêm package `application/dto/Lesson/` với 5 DTOs: `CreateLessonInput`, `UpdateLessonInput`, `DeleteLessonInput`, `GetLessonsInput`, `GetLessonsOutput`
   - Thêm `CourseLessonRepository` interface
   - Thêm package `application/usecase/Lesson/` với 4 UseCases: `GetLessonsUseCase`, `CreateLessonUseCase`, `UpdateLessonUseCase`, `DeleteLessonUseCase`

3. **Adapter Layer:**
   - Thêm `JpaCourseLessonRepository` (Spring Data interface)
   - Thêm `CourseLessonRepositoryImpl` implementation
   - Thêm package `adapter/dto/request/Lesson/` với 2 request DTOs: `CreateLessonRequest`, `UpdateLessonRequest`
   - Thêm `GetLessonsResponse` trong `adapter/dto/response/`
   - Thêm `CourseLessonController` trong `adapter/controller/Course/Lesson/` với 4 endpoints CRUD

4. **Infrastructure Layer:**
   - Thêm 2 error codes: `LESSON_NOT_FOUND`, `LESSON_ACCESS_DENIED`
   - Thêm exception handlers trong `GlobalExceptionHandler`
   - Cập nhật `DataInitializer`: thêm 2 permissions mới `CREATE_LESSON`, `EDIT_LESSON` và gán đúng role

5. **Documentation:**
   - Cập nhật `docs/permission-matrix.md`: thêm 2 permissions mới (#8, #9)
   - Cập nhật `docs/api-docs.md`: thêm đầy đủ API documentation cho Lesson Management
   - Tạo `docs/plan-lesson-management.md`: kế hoạch triển khai chi tiết

### 8.2. Kiến trúc & Design Patterns áp dụng

- **Follow chính xác pattern của Section Management** đã triển khai thành công
- **Consistent Layering**: Tất cả các layer đều follow cùng pattern
- **Reuse Policy Pattern**: `LessonAuthorizationService` tái sử dụng `CourseOwnershipPolicy`
- **Clean Architecture Strictness**: Tuân thủ nghiêm ngặt dependency rule
- **Constructor Injection Only**: Tất cả class đều dùng `@RequiredArgsConstructor`

### 8.3. Phân quyền Lesson

| Role | Xem Lesson | Tạo/Sửa/Xóa Lesson |
|------|------------|-------------------|
| MEMBER | ✅ | ❌ |
| INSTRUCTOR | ✅ | ✅ (chỉ course của mình) |
| STAFF | ✅ | ✅ |
| ADMIN_USER | ✅ | ❌ (không có quyền) |
| SUPER_ADMIN | ✅ | ✅ |

> **Lưu ý:** `ADMIN_USER` không có quyền thao tác Lesson (giống với Section) - đây là design decision quan trọng.

### 8.4. Các file đã tạo mới (tổng cộng 16 files)

```
src/main/java/com/example/learning_system_spring/
├── domain/
│   ├── exception/
│   │   ├── LessonNotFoundException.java
│   │   └── LessonAccessDeniedException.java
│   └── service/
│       └── LessonAuthorizationService.java
├── application/
│   ├── dto/Lesson/
│   │   ├── CreateLessonInput.java
│   │   ├── UpdateLessonInput.java
│   │   ├── DeleteLessonInput.java
│   │   ├── GetLessonsInput.java
│   │   └── GetLessonsOutput.java
│   ├── repository/Course/
│   │   └── CourseLessonRepository.java
│   └── usecase/Lesson/
│       ├── GetLessonsUseCase.java
│       ├── CreateLessonUseCase.java
│       ├── UpdateLessonUseCase.java
│       └── DeleteLessonUseCase.java
├── adapter/
│   ├── repository/
│   │   ├── JpaCourseLessonRepository.java
│   │   └── CourseLessonRepositoryImpl.java
│   ├── dto/request/Lesson/
│   │   ├── CreateLessonRequest.java
│   │   └── UpdateLessonRequest.java
│   ├── dto/response/
│   │   └── GetLessonsResponse.java
│   └── controller/Course/Lesson/
│       └── CourseLessonController.java
└── infrastructure/
    └── exception/
        └── ErrorCode.java (updated)
        └── GlobalExceptionHandler.java (updated)
    └── config/
        └── DataInitializer.java (updated)

docs/
├── plan-lesson-management.md (new)
├── api-docs.md (updated)
└── permission-matrix.md (updated)
```

### 8.5. API Endpoints mới

1. **GET** `/api/v1/courses/{courseId}/sections/{sectionId}/lessons`
2. **POST** `/api/v1/courses/{courseId}/sections/{sectionId}/lessons`
3. **PUT** `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}`
4. **DELETE** `/api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}`

### 8.6. Ghi chú cho AI Agents tiếp theo

- **Code đã được viết theo đúng pattern** của Section Management - có thể tham khảo để hiểu structure
- **Permission seeding đã hoàn thiện**: 5 roles, 15 permissions, gán đúng theo permission matrix
- **Có compile errors cần fix**: 
  - `LessonAuthorizationService` cần truyền đủ 3 parameters cho `CourseOwnershipPolicy.isInstructorOwner()`
  - `DataInitializer.assignPermission()` cần sửa cách tạo `RolePermissionJpaEntity` (hiện dùng native query)
- **Testing**: Có thể test với Postman/Insomnia theo documentation trong `api-docs.md`
- **Next steps**: Có thể tiếp tục với Progress Tracking, Grade Management, hoặc Notification System