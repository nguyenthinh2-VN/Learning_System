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
- **19 permissions** đã seed (xem `docs/permission-matrix.md`).
- Cấu trúc bảo mật lồng ghép trong `SecurityConfig` và kiểm tra quyền cụ thể tại UseCase / Controller.
- Permissions được seed tự động qua `DataInitializer` khi khởi động lần đầu.

### 2.3. Course Management (Quản lý Khóa học)
- CRUD Khóa học (Tạo, Sửa, Xóa, Xem danh sách phân trang, Xem chi tiết).
- **Strategy Pattern (`CourseStrategyFactory`)** để kiểm soát quyền sửa/xóa/tạo khóa học:
  - `InstructorCourseStrategy`: Giảng viên chỉ thao tác được khóa học của chính mình.
  - `StaffAdminCourseStrategy`: Admin/Staff thao tác được toàn quyền khóa học của người khác.

### 2.4. Wallet, Monetization & Checkout (Nạp ví & Thanh toán)
- Quản lý số dư (`balance`) bằng `BigDecimal` trong Domain `User`.
- **Wallet Top-up (Strategy Pattern / Open-Closed):**
  - `InitTopUpUseCase`: Tạo pending `WalletTransaction` + gọi `PaymentGateway.initPayment()`. Không biết provider cụ thể.
  - `CompleteTopUpUseCase`: Use case **dùng chung** cho mọi provider — verify pending tx → cộng tiền → push WebSocket. Cả Mock lẫn VietQR đều gọi use case này.
  - `AdminTopUpUseCase`: Admin cộng tiền trực tiếp (COMPLETED ngay, không qua pending) → push WebSocket.
  - `PaymentGateway` (interface port): `MockPaymentGateway` active khi `payment.provider=mock`. Khi ghép VietQR chỉ thêm `VietQrGateway` + `VietQrWebhookController`, không sửa code cũ.
  - `WalletNotificationService`: Push WebSocket STOMP event `WALLET_UPDATED` tới đúng user qua `SimpMessagingTemplate`.
  - Bảng `wallet_transactions`: Lưu audit trail mọi giao dịch nạp tiền (PENDING/COMPLETED/EXPIRED/FAILED, source: MOCK/VIETQR/ADMIN).
- `TopUpBalanceUseCase` (cũ): Vẫn còn, dùng cho `/api/v1/users/me/top-up` (nạp trực tiếp không qua gateway — có thể deprecated sau).
- `ApplyVoucherCheckoutUseCase`: Mua khóa học (gộp luồng có / không voucher).
  - Giữ pessimistic lock theo thứ tự cố định `User → Course → Voucher` để tránh deadlock.
  - Server tự đọc giá từ DB, tính lại bằng `PricingEngine`, không tin giá client.
  - Internal Member luôn `paidPrice = 0`, voucher bị bỏ qua, không tạo `VoucherUsage`.
- **Audit Logging:** `PurchaseLedgerService` ghi JSONL append-only vào `logs/purchase_ledger.jsonl` với 3 event: `PURCHASE_COMPLETED`, `VOUCHER_APPLIED`, `VOUCHER_REJECTED`.

### 2.5. Course Section Management (Quản lý Chương học)
- CRUD Section theo cấu trúc phân cấp `Course → Section → Lesson`.
- `ADMIN_USER` có quyền sửa/xóa Course nhưng **không** thao tác Section. Chỉ INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN.
- `CourseOwnershipPolicy` — pure static policy class ở Domain layer, tập trung logic kiểm tra ownership. Cả `CourseAuthorizationService`, `SectionAuthorizationService`, `LessonAuthorizationService` đều gọi vào đây.
- Cascade delete: Xóa Section → tự động xóa toàn bộ Lessons (`orphanRemoval = true`).
- 2 permissions: `CREATE_SECTION`, `EDIT_SECTION`.

### 2.6. Course Lesson Management (Quản lý Bài giảng)
- CRUD Lesson theo cấu trúc phân cấp `Course → Section → Lesson`.
- Phân quyền tương tự Section: ADMIN_USER **không có quyền**, chỉ INSTRUCTOR (course của mình), STAFF, SUPER_ADMIN.
- `LessonAuthorizationService` tái sử dụng `CourseOwnershipPolicy`.
- 2 permissions: `CREATE_LESSON`, `EDIT_LESSON`.

### 2.7. Course Approval & Visibility (MỚI)
- Tách "tạo course" với "công khai course". Course mới mặc định `published = false`, KHÔNG xuất hiện ở public listing.
- 2 cờ trên course:
  - `published`: course đã duyệt và công khai.
  - `priceLocked`: giá bị khóa, INSTRUCTOR không sửa được sau khi publish.
- 4 endpoint mới:
  - `GET /api/v1/admin/courses/pending` — danh sách course chờ duyệt.
  - `POST /api/v1/admin/courses/{id}/publish` — duyệt và công khai (set `published = true`, `priceLocked = true`).
  - `POST /api/v1/admin/courses/{id}/unpublish` — ẩn course đã publish, giữ enrollment đã có.
  - `PUT /api/v1/admin/courses/{id}/price` — admin sửa giá kể cả khi đã `priceLocked`.
- 2 endpoint cho INSTRUCTOR xem course của mình kể cả pending: `GET /api/v1/instructor/courses` và `GET /api/v1/instructor/courses/{id}`.
- 3 domain exceptions: `CourseNotPublishedException`, `CoursePriceLockedException`, `CourseAlreadyPublishedException`.
- 2 permissions: `PUBLISH_COURSE` (STAFF, SUPER_ADMIN), `LOCK_COURSE_PRICE` (STAFF, SUPER_ADMIN).
- Audit: course publish / unpublish / price update có thể log vào `purchase_ledger.jsonl` (mở rộng từ `PurchaseLedgerService`).

### 2.8. Voucher Pricing & Management (MỚI)
- Cơ chế mã giảm giá cho luồng mua khóa học. Hỗ trợ 2 loại: `PERCENT` (giảm theo phần trăm, có `maxDiscount` cap) và `FIXED` (giảm số tiền cố định).
- 2 endpoint nghiệp vụ:
  - `POST /api/v1/courses/{courseId}/quote` — tính giá xem trước, read-only, không tiêu thụ voucher.
  - `POST /api/v1/courses/{courseId}/purchase` — checkout với `voucherCode` tùy chọn, server tính lại giá độc lập.
- 4 endpoint admin CRUD:
  - `POST /api/v1/admin/vouchers` (tạo).
  - `GET /api/v1/admin/vouchers` (danh sách phân trang, kèm `usedCount`).
  - `PUT /api/v1/admin/vouchers/{id}` (cập nhật, từ chối sửa `code`/`type`/`value` khi đã có usage).
  - `DELETE /api/v1/admin/vouchers/{id}` (soft-delete: set `status = INACTIVE`).
- **Pure domain service:**
  - `PricingEngine.compute(originalPrice, voucher) → PriceQuote` — pure function, không Spring/JPA, dùng `BigDecimal` scale 2 HALF_UP.
  - `VoucherValidator.validate(voucher, courseId, originalPrice, now, globalUsedCount, perUserUsedCount)` — kiểm tra theo thứ tự cố định: status → validFrom → validTo → scope → minOrder → usageLimit → usagePerUser.
  - Đăng ký bean qua `DomainServiceConfig` để inject vào use case.
- **Anti-tampering (4 mặt):**
  1. Body request DTO chỉ khai báo `voucherCode`. Mọi field giá (`price`, `discount`, `finalPrice`...) bị Spring bỏ qua không bind.
  2. Server đọc giá từ DB theo `courseId` ở path, không tin client.
  3. Mỗi `/purchase` tính lại giá độc lập, không có khái niệm "quote token" / "preview cache".
  4. Pessimistic write lock trên hàng voucher + UNIQUE `(voucher_id, enrollment_id)` ở DB chống race condition.
- **Quy tắc role:**
  - Internal Member luôn `paidPrice = 0`, voucher bị ignore, không tạo `VoucherUsage`.
  - INSTRUCTOR / STAFF / ADMIN_USER gửi `voucherCode` → 403 (`VOUCHER_USE_DENIED`).
  - Chỉ MEMBER và SUPER_ADMIN có `USE_VOUCHER`.
- 12+ domain exceptions: `VoucherNotFoundException`, `VoucherInactiveException`, `VoucherNotYetActiveException`, `VoucherExpiredException`, `VoucherNotApplicableException`, `VoucherMinOrderNotMetException`, `VoucherUsageLimitReachedException`, `VoucherUsagePerUserExceededException`, `VoucherUseDeniedException`, `VoucherCodeAlreadyExistsException`, `VoucherUsageLimitTooLowException`, `VoucherImmutableFieldException`, `AlreadyEnrolledException`, `InsufficientBalanceException`.
- 2 permissions: `MANAGE_VOUCHER` (STAFF, SUPER_ADMIN), `USE_VOUCHER` (MEMBER, SUPER_ADMIN).
- Audit: `VOUCHER_APPLIED` và `VOUCHER_REJECTED` ghi vào `logs/purchase_ledger.jsonl` với đầy đủ `voucherCode`, `originalPrice`, `discountAmount`, `finalPrice`, `enrollmentId`.

---

## 3. Cấu trúc thư mục (Directory Tree)

Cấu trúc phân lớp theo **Clean Architecture**, mỗi lớp tuân thủ chặt chẽ Dependency Rule (Dependency chỉ hướng vào trong Domain).

```text
src/main/java/com/example/learning_system_spring
|-- LearningSystemSpringApplication.java (Main class)
|
|-- adapter/  (Lớp ngoài cùng: Controllers, DTOs, DB Repositories, Mappers)
|   |-- controller/
|   |   |-- Auth/AuthController.java
|   |   |-- Course/
|   |   |   |-- CourseController                # /api/v1/courses (public + purchase + quote)
|   |   |   |-- CourseQuoteController            # POST /api/v1/courses/{id}/quote
|   |   |   |-- CourseSectionController          # /api/v1/courses/{id}/sections
|   |   |   |-- InstructorCourseController       # /api/v1/instructor/courses (kể cả pending)
|   |   |   |-- Lesson/CourseLessonController    # /api/v1/courses/{id}/sections/{id}/lessons
|   |   |-- AdminCourseController                # /api/v1/admin/courses/** (publish, price...)
|   |   |-- AdminUserController                  # /api/v1/admin/users
|   |   |-- AdminVoucherController               # /api/v1/admin/vouchers (CRUD)
|   |   |-- UserController                       # /api/v1/users/me/top-up
|   |-- dto/
|   |   |-- request/
|   |   |   |-- Course/                          # CreateCourseRequest, UpdateCourseRequest,
|   |   |   |                                    #   PurchaseCourseRequest (chỉ voucherCode),
|   |   |   |                                    #   UpdateCoursePriceRequest
|   |   |   |-- Lesson/                          # CreateLessonRequest, UpdateLessonRequest
|   |   |   |-- Section/                         # CreateSectionRequest, UpdateSectionRequest
|   |   |   |-- Voucher/                         # CreateVoucherRequest, UpdateVoucherRequest
|   |   |   |-- LoginRequest, RegisterRequest, CreateUserRequest
|   |   |-- response/                            # ApiResponse, CourseDetailResponse,
|   |                                            #   CourseListResponse, SectionResponse,
|   |                                            #   LessonResponse, GetLessonsResponse,
|   |                                            #   PurchaseCourseResponse, QuotePricingResponse,
|   |                                            #   VoucherResponse, LoginResponse, RegisterResponse
|   |-- mapper/                                  # CourseMapper (JPA Entity ↔ Domain)
|   |-- repository/
|       |-- jpa/
|       |   |-- CourseEntity/                    # CourseJpaEntity (4 cờ: published, priceLocked,
|       |   |                                    #   publishedAt, publishedBy), CourseSectionJpaEntity,
|       |   |                                    #   CourseLessonJpaEntity, EnrollmentJpaEntity
|       |   |-- UserEntity/UserJpaEntity
|       |   |-- VoucherEntity/                   # VoucherJpaEntity, VoucherUsageJpaEntity
|       |   |-- role_permissionEntity/           # RoleJpaEntity, PermissionJpaEntity, RolePermissionJpaEntity
|       |-- JpaCourseRepository, JpaCourseSectionRepository, JpaCourseLessonRepository,
|       |   JpaUserRepository, JpaRoleRepository, JpaEnrollmentRepository,
|       |   JpaVoucherRepository, JpaVoucherUsageRepository (Spring Data interfaces)
|       |-- *RepositoryImpl                      # Implements application.repository.* (chuyển domain ↔ JPA)
|
|-- application/ (Lớp Use Case: Chứa logic nghiệp vụ ứng dụng, độc lập với Framework Web/DB)
|   |-- dto/
|   |   |-- Auth/                                # LoginInput/Output, RegisterInput/Output
|   |   |-- Course/                              # CreateCourseInput, UpdateCourseInput, DeleteCourseInput,
|   |   |                                        #   GetCourseListInput (Scope: PUBLIC/PENDING/ALL/INSTRUCTOR),
|   |   |                                        #   GetCourseDetailInput, CourseOutput,
|   |   |                                        #   PublishCourseInput, UnpublishCourseInput,
|   |   |                                        #   UpdateCoursePriceInput
|   |   |-- Section/                             # CreateSectionInput, UpdateSectionInput,
|   |   |                                        #   DeleteSectionInput, SectionOutput, LessonOutput
|   |   |-- Lesson/                              # CreateLessonInput, UpdateLessonInput,
|   |   |                                        #   DeleteLessonInput, GetLessonsInput, GetLessonsOutput
|   |   |-- Voucher/                             # CreateVoucherInput, UpdateVoucherInput, DeleteVoucherInput,
|   |   |                                        #   GetVouchersInput, VoucherOutput,
|   |   |                                        #   QuotePricingInput, QuotePricingOutput,
|   |   |                                        #   PurchaseCourseInput, PurchaseCourseOutput
|   |   |-- CreateUserInput, PageResult
|   |-- repository/                              # Repository Interfaces (Adapter implement)
|   |   |-- Course/                              # CourseRepository, CourseSectionRepository,
|   |   |                                        #   CourseLessonRepository, EnrollmentRepository
|   |   |-- User/UserRepository
|   |   |-- Voucher/                             # VoucherRepository, VoucherUsageRepository
|   |   |-- RoleRepository
|   |-- usecase/
|       |-- Auth/                                # LoginUseCase, RegisterUseCase
|       |-- Course/                              # CreateCourse, UpdateCourse, DeleteCourse,
|       |                                        #   GetCourseList, GetCourseDetail,
|       |                                        #   PublishCourseUseCase, UnpublishCourseUseCase,
|       |                                        #   UpdateCoursePriceUseCase
|       |-- Section/                             # GetSections, CreateSection, UpdateSection, DeleteSection
|       |-- Lesson/                              # GetLessons, CreateLesson, UpdateLesson, DeleteLesson
|       |-- User/                                # TopUpBalanceUseCase, AdminCreateUserUseCase
|       |-- Voucher/                             # CreateVoucherUseCase, UpdateVoucherUseCase,
|       |                                        #   DeleteVoucherUseCase, GetVouchersUseCase,
|       |                                        #   QuotePricingUseCase (read-only),
|       |                                        #   ApplyVoucherCheckoutUseCase (entry point DUY NHẤT cho purchase)
|       |-- strategy/                            # CourseStrategyFactory, InstructorCourseStrategy,
|                                                #   StaffAdminCourseStrategy,
|                                                #   UsernameGeneratorFactory + *UsernameGeneratorStrategy
|
|-- domain/ (Lớp Lõi: Chứa Model nghiệp vụ tinh khiết, Business rules)
|   |-- exception/                               # 27+ domain exceptions:
|   |                                            #   - Course: CourseNotFoundException, CourseAccessDeniedException,
|   |                                            #     CourseNotPublishedException, CoursePriceLockedException,
|   |                                            #     CourseAlreadyPublishedException
|   |                                            #   - Section/Lesson/User: SectionNotFoundException,
|   |                                            #     SectionAccessDeniedException, LessonNotFoundException,
|   |                                            #     LessonAccessDeniedException, UserNotFoundException,
|   |                                            #     EmailAlreadyExistsException, InvalidCredentialsException,
|   |                                            #     InvalidEmailException, InsufficientBalanceException,
|   |                                            #     AlreadyEnrolledException
|   |                                            #   - Voucher: VoucherNotFoundException, VoucherInactiveException,
|   |                                            #     VoucherNotYetActiveException, VoucherExpiredException,
|   |                                            #     VoucherNotApplicableException, VoucherMinOrderNotMetException,
|   |                                            #     VoucherUsageLimitReachedException,
|   |                                            #     VoucherUsagePerUserExceededException,
|   |                                            #     VoucherUseDeniedException, VoucherCodeAlreadyExistsException,
|   |                                            #     VoucherUsageLimitTooLowException, VoucherImmutableFieldException
|   |-- model/                                   # User, Course (4 cờ approval), CourseSection, CourseLesson,
|   |                                            #   Enrollment, Role, Permission
|   |-- model/Voucher/                           # Voucher, VoucherType (PERCENT/FIXED),
|   |                                            #   VoucherStatus (ACTIVE/INACTIVE),
|   |                                            #   VoucherScope (ALL_COURSES/SPECIFIC_COURSES),
|   |                                            #   VoucherUsage, PriceQuote (value object)
|   |-- service/
|       |-- CourseOwnershipPolicy                # Pure static policy — kiểm tra ownership
|       |-- CourseAuthorizationService           # Kiểm quyền Course
|       |-- SectionAuthorizationService          # Kiểm quyền Section
|       |-- LessonAuthorizationService           # Kiểm quyền Lesson
|       |-- PricingEngine                        # Pure domain — tính giá (PERCENT/FIXED, BigDecimal)
|       |-- VoucherValidator                     # Pure domain — validate voucher theo thứ tự cố định
|
|-- infrastructure/ (Lớp Cơ sở hạ tầng: Cấu hình Framework, Utils, External Services)
    |-- config/
    |   |-- SecurityConfig                       # Filter chain, public endpoints (login/register, GET /courses)
    |   |-- JwtAuthenticationFilter, JwtService
    |   |-- CustomAuthenticationEntryPoint
    |   |-- DataInitializer                      # Seed 5 roles + 19 permissions + role-permission matrix
    |   |-- DomainServiceConfig                  # Đăng ký PricingEngine + VoucherValidator làm Spring bean
    |-- exception/
    |   |-- ErrorCode                            # 30+ enum codes (course, section, lesson, voucher, purchase)
    |   |-- ErrorResponse, GlobalExceptionHandler# Map mọi domain exception → HTTP response
    |-- service/
        |-- PurchaseLedgerService                # JSONL audit log: PURCHASE_COMPLETED, VOUCHER_APPLIED, VOUCHER_REJECTED
```

---

## 4. Database Schema (Các bảng cốt lõi)

- **`users`**: Quản lý tài khoản, mật khẩu (Bcrypt), số dư `balance` và cờ `is_internal`.
- **`roles` / `permissions` / `role_permissions`**: Cấu trúc RBAC n-n. 19 permissions seed sẵn.
- **`courses`**: Thông tin khóa học, `price`, `max_students`, `instructor_id`, **+ 4 cột mới**: `published`, `price_locked`, `published_at`, `published_by`. INDEX `(published)` để filter public listing nhanh.
- **`course_sections`**: Chương học, quan hệ N-1 với `courses`. Có `order_index`.
- **`course_lessons`**: Bài giảng, quan hệ N-1 với `course_sections`. Có `content_url` và `order_index`.
- **`enrollments`**: Lưu lịch sử mua khóa học. Chứa `user_id`, `course_id`, `paid_price`, `enrolled_at`.
- **`vouchers`** (MỚI): `code` UNIQUE (lưu UPPERCASE), `type` (PERCENT/FIXED), `value`, `status` (ACTIVE/INACTIVE), `scope` (ALL_COURSES/SPECIFIC_COURSES), `valid_from`, `valid_to`, `min_order_amount`, `max_discount`, `usage_limit`, `usage_per_user`. INDEX `(status, valid_to)` để filter voucher còn hạn nhanh.
- **`voucher_courses`** (MỚI, mapping cho `scope = SPECIFIC_COURSES`): composite PK `(voucher_id, course_id)` với FK CASCADE.
- **`voucher_usages`** (MỚI): bản ghi tiêu thụ voucher với `voucher_id`, `user_id`, `course_id`, `enrollment_id`, `original_price`, `discount_amount`, `final_price`, `applied_at`. **UNIQUE `(voucher_id, enrollment_id)`** chống race condition. INDEX `(voucher_id)` và `(voucher_id, user_id)` để đếm `usedCount` hiệu quả.
- **`wallet_transactions`** (MỚI): Audit trail mọi giao dịch nạp tiền. `reference_code` UNIQUE, `status` (PENDING/COMPLETED/EXPIRED/FAILED), `source` (MOCK/VIETQR/ADMIN), `expired_at` cho pending TTL.

---

## 5. Design Patterns đang sử dụng

| Pattern | Nơi áp dụng | Mục đích |
|---------|-------------|---------|
| **Strategy** | `CourseStrategyFactory` + `InstructorCourseStrategy` / `StaffAdminCourseStrategy` | Phân quyền tạo/sửa/xóa Course theo Role |
| **Strategy** | `UsernameGeneratorFactory` + các `*UsernameGeneratorStrategy` | Sinh Username tự động theo Role prefix |
| **Policy (Static)** | `CourseOwnershipPolicy` | Tập trung logic kiểm tra ownership, tái sử dụng ở nhiều Service |
| **Pure Domain Service** | `PricingEngine`, `VoucherValidator` | Logic tính giá / validate voucher độc lập với Spring/JPA — testable bằng unit test thuần |
| **Repository** | `CourseRepository`, `UserRepository`, `VoucherRepository`... | Abstraction giữa UseCase và DB |
| **Factory** | `CourseStrategyFactory`, `UsernameGeneratorFactory` | Chọn Strategy phù hợp theo Role |
| **Strategy** | `PaymentGateway` interface + `MockPaymentGateway` / `VietQrGateway` | Tách payment provider — thêm provider mới không sửa code cũ |
| **Value Object** | `PriceQuote` (`originalPrice`, `discountAmount`, `finalPrice`) | Đại diện kết quả tính giá bất biến |
| **Soft-delete** | Voucher (set `status = INACTIVE`) | Bảo toàn lịch sử Voucher_Usage |

---

## 6. Tổng quan API Endpoints

| Nhóm | Số endpoints | Tham chiếu |
|------|--------------|------------|
| Auth | 3 | `docs/api-docs.md` mục 1 |
| Course public | 6 (list, detail, create, update, delete, purchase) | mục 6 + 7 |
| Course quote | 1 (`POST /api/v1/courses/{id}/quote`) | mục 11.5 |
| Course Approval (admin + instructor) | 7 (pending, all, publish, unpublish, price + 2 instructor endpoints) | mục 10 |
| Wallet | 3 (init top-up, mock webhook, admin top-up) | mục 14 |
| Section | 4 (CRUD) | mục 8 |
| Lesson | 4 (CRUD) | mục 9 |
| Voucher Admin | 4 (CRUD) | mục 11.1–11.4 |

> Chi tiết request / response xem `docs/api-docs.md`. Ma trận phân quyền xem `docs/permission-matrix.md`. Kế hoạch chi tiết xem `docs/plan-voucher-pricing.md` và `docs/plan-course-approval.md`.

---

## 7. Quy tắc chung khi tiếp tục phát triển (For AI Agents)

1. **Clean Architecture Strictness:** Model trong `domain/model` tuyệt đối **không** dùng bất kỳ annotation của JPA hay Spring nào. JPA Entity phải nằm ở `adapter/repository/jpa`. Chuyển đổi giữa 2 dạng này thông qua các phương thức `toDomain()` và `fromDomain()`.
2. **Pure Domain Service:** `PricingEngine`, `VoucherValidator`, `CourseOwnershipPolicy` là class thuần Java, KHÔNG dùng `@Service` / `@Component`. Đăng ký bean qua `DomainServiceConfig` (`@Bean`).
3. **Business Logic Location:** Logic nghiệp vụ chính (kiểm tra điều kiện, thao tác tiền, validate voucher) luôn nằm ở `application/usecase` hoặc `domain/service`. Domain method (như `Course.publish()`, `Course.updatePrice()`, `User.deductBalance()`) đặt ở domain model.
4. **Concurrency:** Đối với các API tài chính (top-up, mua khóa học, voucher), luôn dùng `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Thứ tự lock cố định `User → Course → Voucher` để tránh deadlock.
5. **Anti-tampering:** DTO request cho luồng pricing CHỈ khai báo trường nghiệp vụ (`voucherCode`). KHÔNG bao giờ khai báo field giá tiền (`price`, `discount`, `finalPrice`) — Spring sẽ tự bỏ qua field thừa từ client.
6. **Server-side Recomputation:** Mọi luồng định giá (`/quote`, `/purchase`) PHẢI đọc giá từ DB và tính lại bằng `PricingEngine`. Không tin kết quả của lần gọi trước.
7. **Exception Handling:** Ném Domain Exception (kế thừa `RuntimeException` / `IllegalStateException`) trong UseCase. `GlobalExceptionHandler` ở Infrastructure layer map ra HTTP response thống nhất.
8. **Authorization Pattern:** Mọi logic kiểm tra ownership trên Course đi qua `CourseOwnershipPolicy` (static). Không lặp `instructorId.equals(requesterId)` ở nhiều nơi.
9. **Permission Seeding:** Khi thêm permission mới, cập nhật đồng thời 3 nơi: `DataInitializer` (seed DB + gán role), `docs/permission-matrix.md`, `GlobalExceptionHandler` + `ErrorCode` (nếu có exception mới).
10. **Audit Logging:** Mọi event tài chính (purchase, voucher applied, voucher rejected) ghi qua `PurchaseLedgerService` để có audit trail bất biến trong `logs/purchase_ledger.jsonl`.
11. **Constructor Injection Only:** Tuyệt đối cấm `@Autowired` field/setter injection. Chỉ dùng constructor injection hoặc Lombok `@RequiredArgsConstructor`.
12. **Tooling & Code Edits:** Thay thế nội dung file bằng `str_replace` với matching chính xác. Tạo file mới bằng `fs_write`.

---

## 8. Cập nhật mới nhất (24/05/2026) — Voucher Pricing & Course Approval

### 8.1. Course Approval Workflow (mới)

**Mục tiêu:** Tách "tạo course" với "công khai course" để chống lộ giá trước khi admin duyệt và tránh course chưa hoàn thiện hiển thị công khai.

**Triển khai:**
- Thêm 4 cột vào `courses`: `published`, `price_locked`, `published_at`, `published_by`.
- 3 use case mới: `PublishCourseUseCase`, `UnpublishCourseUseCase`, `UpdateCoursePriceUseCase`.
- `GetCourseListUseCase` mở rộng với 4 scope: `PUBLIC` (public listing chỉ trả published), `PENDING`, `ALL`, `INSTRUCTOR`.
- 3 domain exception mới + 3 error code: `COURSE_NOT_PUBLISHED`, `COURSE_PRICE_LOCKED`, `COURSE_ALREADY_PUBLISHED`.
- 2 controller mới: `AdminCourseController` (5 endpoints), `InstructorCourseController` (2 endpoints).
- 2 permission mới: `PUBLISH_COURSE`, `LOCK_COURSE_PRICE`. Gán cho STAFF + SUPER_ADMIN.
- `CourseController.purchase` và `CourseQuoteController` đã thêm check `course.published` đầu luồng — ném `CourseNotPublishedException` nếu chưa duyệt.

### 8.2. Voucher Pricing & Management (mới)

**Mục tiêu:** Cơ chế mã giảm giá an toàn cho luồng mua khóa học. Anti-tampering 4 mặt + concurrency-safe.

**Triển khai:**
- 6 domain model: `Voucher`, `VoucherType`, `VoucherStatus`, `VoucherScope`, `VoucherUsage`, `PriceQuote`.
- 12 voucher exceptions + `AlreadyEnrolledException` mới.
- 2 pure domain service: `PricingEngine` (compute), `VoucherValidator` (validate). Đăng ký bean qua `DomainServiceConfig`.
- 2 application repository interface + 2 JPA entity + 2 repository impl.
- 6 use case: 4 admin CRUD (`CreateVoucherUseCase`, `UpdateVoucherUseCase`, `DeleteVoucherUseCase`, `GetVouchersUseCase`) + `QuotePricingUseCase` (read-only) + `ApplyVoucherCheckoutUseCase` (gộp luồng có / không voucher, thay thế `PurchaseCourseUseCase` cũ — hiện đã không còn).
- 2 controller mới: `AdminVoucherController` (4 CRUD endpoints), `CourseQuoteController` (1 endpoint).
- `CourseController.purchase` cập nhật để gọi `ApplyVoucherCheckoutUseCase`, chấp nhận `voucherCode` tùy chọn trong body.
- `PurchaseCourseRequest` DTO chỉ khai báo `voucherCode` (regex `^[A-Za-z0-9_-]*$`, max 32) — chống tampering.
- `PurchaseLedgerService` mở rộng với 2 method mới: `logVoucherApplied`, `logVoucherRejected`.
- 12 error code mới trong `ErrorCode` enum + 12 handler trong `GlobalExceptionHandler`.
- 2 permission mới: `MANAGE_VOUCHER` (STAFF, SUPER_ADMIN), `USE_VOUCHER` (MEMBER, SUPER_ADMIN).

### 8.3. Anti-tampering & Concurrency Properties

**Anti-tampering (4 mặt):**

| Mối lo | Cách giải quyết |
|--------|-----------------|
| Client tampering `courseId` | Server đọc giá từ DB theo `courseId` ở path |
| Client tampering `price` / `finalPrice` | DTO chỉ khai báo `voucherCode`, Spring bỏ qua field thừa |
| Replay quote cũ | Mỗi `/purchase` tính lại giá độc lập, không có quote token |
| Race condition voucher quota | Pessimistic lock `User → Course → Voucher` + UNIQUE `(voucher_id, enrollment_id)` |

**Pricing Engine invariants** (cho mọi đầu vào hợp lệ):
- `0 ≤ discountAmount ≤ originalPrice`
- `finalPrice = originalPrice − discountAmount`
- `0 ≤ finalPrice ≤ originalPrice`
- BigDecimal scale 2, rounding HALF_UP
- `originalPrice = 0` → cả discount và final đều 0 (voucher vô nghĩa với khóa miễn phí)

**Voucher Validator** kiểm tra theo thứ tự cố định:
`status → validFrom → validTo → scope → minOrder → usageLimit → usagePerUser`

→ Cùng đầu vào luôn ném cùng exception (deterministic).

### 8.4. Permission Matrix sau update

5 roles × 19 permissions. Highlight các permission mới:

| Permission | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|:------:|:----------:|:-----:|:----------:|:-----------:|
| `PUBLISH_COURSE` | | | ✅ | | ✅ |
| `LOCK_COURSE_PRICE` | | | ✅ | | ✅ |
| `MANAGE_VOUCHER` | | | ✅ | | ✅ |
| `USE_VOUCHER` | ✅ | | | | ✅ |

> Lưu ý: INSTRUCTOR / ADMIN_USER không có `USE_VOUCHER` vì họ không phải đối tượng mua khóa học. STAFF có `MANAGE_VOUCHER` (quản lý) nhưng không có `USE_VOUCHER` (không tự mua).

### 8.5. Files mới và sửa (~70 files cộng dồn cho 2 feature)

**Domain layer:**
- 7 voucher model + 6 voucher domain exceptions mới + 6 voucher exceptions phụ trợ.
- 3 course approval exceptions: `CourseNotPublishedException`, `CoursePriceLockedException`, `CourseAlreadyPublishedException`.
- 2 pure domain service: `PricingEngine`, `VoucherValidator`.
- `Course.java` cập nhật: 4 field mới + 3 method (`publish`, `unpublish`, `updatePrice`).

**Application layer:**
- 9 voucher DTO record + 3 course approval DTO + cập nhật `GetCourseListInput` với enum `Scope`.
- 2 voucher repository interface mới.
- 6 voucher use case + 3 course approval use case mới.

**Adapter layer:**
- 2 voucher JPA entity + 2 Spring Data interface + 2 repository impl.
- 4 voucher request/response DTO + cập nhật `PurchaseCourseRequest`.
- 3 controller mới: `AdminVoucherController`, `CourseQuoteController`, `AdminCourseController`, `InstructorCourseController`.

**Infrastructure:**
- `ErrorCode`: thêm 15 enum mới.
- `GlobalExceptionHandler`: thêm 15 handler mới.
- `DataInitializer`: seed 4 permission mới + gán role.
- `DomainServiceConfig` (mới): đăng ký `PricingEngine` + `VoucherValidator` làm Spring bean.
- `PurchaseLedgerService`: thêm 2 method `logVoucherApplied`, `logVoucherRejected`.

**Documentation:**
- `docs/api-docs.md`: thêm section 12 (Course Approval) và section 13 (Voucher Pricing).
- `docs/permission-matrix.md`: cập nhật 4 permission mới.
- `docs/plan-voucher-pricing.md`, `docs/plan-course-approval.md`: kế hoạch chi tiết đã có sẵn.
- `.kiro/specs/voucher-pricing/requirements.md` + `.kiro/specs/voucher-management/requirements.md`: requirements EARS với 12 yêu cầu + 19 correctness properties cho property-based testing.

### 8.6. Ghi chú cho AI Agents tiếp theo

- **Code đã build và chạy** — entry point duy nhất cho purchase là `ApplyVoucherCheckoutUseCase` (gộp cả luồng có / không voucher). `PurchaseCourseUseCase` cũ đã không còn.
- **Pure domain service đăng ký bean ở `DomainServiceConfig`** — không thêm `@Service`/`@Component` vào `PricingEngine` / `VoucherValidator`.
- **Audit log chỉ qua `PurchaseLedgerService`** — không viết file JSONL trực tiếp ở use case.
- **`@PreAuthorize` đã có tại method-level** thông qua `@EnableMethodSecurity`. Nếu thêm endpoint admin mới, dùng `@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")` thay vì check trong use case.
- **Property-based testing**: Pricing engine có 19 correctness property liệt kê trong `.kiro/specs/voucher-pricing/requirements.md` (Pricing engine: 8, Validator: 3, Quote-Checkout consistency: 3, Race: 2, Audit roundtrip: 1, anti-tampering example tests: 2). Khi viết test, tham khảo trực tiếp file đó.
- **Next steps có thể tiếp tục:** Course Submit-for-review endpoint, Voucher detail endpoint (`GET /admin/vouchers/{id}`), Voucher usage history (`GET /admin/vouchers/{id}/usages`), Progress Tracking, Notification System.
