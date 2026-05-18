# Learning System - Project Overview & Architecture

Đây là tài liệu tổng hợp toàn bộ cấu trúc dự án, kiến trúc thiết kế, và các tính năng đã được triển khai. Tài liệu này được tạo ra để giúp các AI Agent hoặc Developer mới nhanh chóng nắm bắt context của toàn bộ dự án Spring Boot.

## 1. Công nghệ & Kiến trúc tổng thể

- **Ngôn ngữ & Framework:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA.
- **Database:** MySQL.
- **Kiến trúc:** Clean Architecture / Hexagonal Architecture. Tuân thủ nghiêm ngặt nguyên tắc SOLID và Design Patterns.
- **Quản lý dependencies:** Maven.
- **Authentication:** Stateless JWT Token.

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

---

## 3. Cấu trúc thư mục (Directory Tree)

Cấu trúc phân lớp theo **Clean Architecture**, mỗi lớp tuân thủ chặt chẽ Dependency Rule (Dependency chỉ hướng vào trong Domain).

```text
src/main/java/com/example/learning_system_spring
|-- LearningSystemSpringApplication.java (Main class)
|
|-- adapter/  (Lớp ngoài cùng: Controllers, DTOs, DB Repositories, Mappers)
|   |-- controller/           # REST APIs (AuthController, UserController, CourseController)
|   |-- dto/                  # Data Transfer Objects cho Request/Response JSON của API
|   |-- mapper/               # Class chuyển đổi (MapStruct/Thủ công) giữa DTO và Domain
|   |-- repository/           # Repository Impl (cài đặt logic gọi tới Spring Data JPA)
|   |-- jpa/                  # JPA Entities (@Entity, @Table định nghĩa DB Schema)
|       |-- CourseEntity/     # courses, course_sections, course_lessons, enrollments
|       |-- UserEntity/       # users
|       |-- role_permissionEntity/ # roles, permissions, role_permissions
|
|-- application/ (Lớp Use Case: Chứa logic nghiệp vụ ứng dụng, độc lập với Framework Web/DB)
|   |-- dto/                  # Data objects nội bộ di chuyển giữa Adapter và Use Case
|   |-- repository/           # Interfaces của Repository (Adapter sẽ implement)
|   |-- usecase/              # Các UseCase chính yếu thực thi tính năng
|       |-- Auth/             # LoginUseCase, RegisterUseCase
|       |-- Course/           # CreateCourse, DeleteCourse, PurchaseCourse...
|       |-- User/             # TopUpBalanceUseCase, AdminCreateUser...
|       |-- strategy/         # Áp dụng Design Pattern Strategy (Course Strategy, Username Generator)
|
|-- domain/ (Lớp Lõi: Chứa Model nghiệp vụ tinh khiết, Business rules)
|   |-- exception/            # Custom Exceptions của Domain (UserNotFound, CourseAccessDenied...)
|   |-- model/                # Thực thể Domain tinh khiết (User, Course, Role, Enrollment) không dính líu Annotation JPA
|   |-- service/              # Domain Services (Xử lý logic liên quan nhiều model, VD: CourseAuthorizationService)
|
|-- infrastructure/ (Lớp Cơ sở hạ tầng: Cấu hình Framework, Utils, External Services)
|   |-- config/               # Cấu hình Spring Security, JWT Filter, Data Initializer
|   |-- exception/            # GlobalExceptionHandler (Bắt lỗi và map ra HTTP Response)
|   |-- service/              # Các external service (VD: PurchaseLedgerService ghi file audit log)
```

## 4. Database Schema (Các bảng cốt lõi)

- **`users`**: Quản lý tài khoản, mật khẩu (Bcrypt), số dư `balance` và cờ `is_internal`.
- **`roles` / `permissions` / `role_permissions`**: Cấu trúc RBAC n-n để cấp quyền linh hoạt.
- **`courses`**: Thông tin khóa học, giá `price`, số lượng học viên `max_students`, `instructor_id`...
- **`course_sections` / `course_lessons`**: Nội dung chương trình học (Quan hệ 1-N).
- **`enrollments`**: Lưu lịch sử mua khóa học. Chứa `user_id`, `course_id`, `paid_price` và ngày đăng ký.

## 5. Quy tắc chung khi tiếp tục phát triển (For AI Agents)

1. **Clean Architecture Strictness:** Model trong `domain/model` tuyệt đối **không** dùng bất kỳ annotation của JPA hay Spring nào. JPA Entity phải nằm ở `adapter/repository/jpa`. Chuyển đổi giữa 2 dạng này thông qua các phương thức `toDomain()` và `fromDomain()`.
2. **Business Logic Location:** Logic nghiệp vụ chính (kiểm tra điều kiện, thao tác tiền) luôn nằm ở `application/usecase`. 
3. **Concurrency:** Đối với các API liên quan đến tài chính (Nạp ví, Mua khóa học), luôn gọi Repository với `@Lock(LockModeType.PESSIMISTIC_WRITE)` để khóa Row DB, ngăn Race Condition.
4. **Exception Handling:** Ném ra Domain Exception (kế thừa `RuntimeException` hoặc `IllegalStateException`) trong UseCase. Sau đó, `GlobalExceptionHandler` ở layer Infrastructure sẽ tự động catch và map ra API Response format thống nhất (VD: HTTP 400 kèm mã `BAD_REQUEST`).
5. **Tooling & Code Edits:** Thay thế nội dung file bằng công cụ `multi_replace_file_content` với matching line cực kỳ chính xác.
