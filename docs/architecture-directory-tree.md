# Cấu trúc thư mục — Learning System Spring

Cấu trúc phân lớp theo **Clean Architecture**, Dependency Rule chỉ hướng vào trong Domain.

```text
src/main/java/com/example/learning_system_spring
|-- LearningSystemSpringApplication.java
|
|-- adapter/
|   |-- controller/
|   |   |-- Auth/AuthController.java
|   |   |-- Course/
|   |   |   |-- CourseController                # /api/v1/courses (public + purchase + quote)
|   |   |   |-- CourseQuoteController            # POST /api/v1/courses/{id}/quote
|   |   |   |-- CourseSectionController          # /api/v1/courses/{id}/sections
|   |   |   |-- InstructorCourseController       # /api/v1/instructor/courses
|   |   |   |-- Lesson/CourseLessonController    # /api/v1/courses/{id}/sections/{id}/lessons
|   |   |-- AdminCourseController                # /api/v1/admin/courses/**
|   |   |-- AdminUserController                  # /api/v1/admin/users (+ /{id}/top-up)
|   |   |-- AdminVoucherController               # /api/v1/admin/vouchers
|   |   |-- UserController                       # /api/v1/users/me/top-up, /me/enrollments
|   |   |-- WalletController                     # /api/v1/wallet/top-up/init
|   |   |-- MockWebhookController                # /api/v1/webhook/mock (dev only)
|   |-- dto/
|   |   |-- request/
|   |   |   |-- Course/                          # CreateCourseRequest, UpdateCourseRequest,
|   |   |   |                                    #   PurchaseCourseRequest, UpdateCoursePriceRequest
|   |   |   |-- Lesson/                          # CreateLessonRequest, UpdateLessonRequest
|   |   |   |-- Section/                         # CreateSectionRequest, UpdateSectionRequest
|   |   |   |-- Voucher/                         # CreateVoucherRequest, UpdateVoucherRequest
|   |   |   |-- Wallet/                          # InitTopUpRequest, AdminTopUpRequest
|   |   |   |-- LoginRequest, RegisterRequest, CreateUserRequest
|   |   |-- response/
|   |       |-- ApiResponse, CourseDetailResponse, CourseListResponse
|   |       |-- SectionResponse, LessonResponse, GetLessonsResponse
|   |       |-- PurchaseCourseResponse, QuotePricingResponse
|   |       |-- VoucherResponse, LoginResponse, RegisterResponse
|   |       |-- MyEnrollmentResponse
|   |-- mapper/CourseMapper.java
|   |-- repository/
|       |-- jpa/
|       |   |-- CourseEntity/                    # CourseJpaEntity, CourseSectionJpaEntity,
|       |   |                                    #   CourseLessonJpaEntity, EnrollmentJpaEntity
|       |   |-- UserEntity/UserJpaEntity
|       |   |-- VoucherEntity/                   # VoucherJpaEntity, VoucherUsageJpaEntity
|       |   |-- WalletEntity/WalletTransactionJpaEntity
|       |   |-- role_permissionEntity/           # RoleJpaEntity, PermissionJpaEntity, RolePermissionJpaEntity
|       |-- Jpa*Repository (Spring Data interfaces)
|       |-- *RepositoryImpl (domain ↔ JPA adapters)
|
|-- application/
|   |-- dto/
|   |   |-- Auth/                                # LoginInput/Output, RegisterInput/Output
|   |   |-- Course/                              # CreateCourseInput, UpdateCourseInput, DeleteCourseInput,
|   |   |                                        #   GetCourseListInput (Scope enum), GetCourseDetailInput,
|   |   |                                        #   CourseOutput, PublishCourseInput, UnpublishCourseInput,
|   |   |                                        #   UpdateCoursePriceInput
|   |   |-- Section/                             # CreateSectionInput, UpdateSectionInput,
|   |   |                                        #   DeleteSectionInput, SectionOutput, LessonOutput
|   |   |-- Lesson/                              # CreateLessonInput, UpdateLessonInput, DeleteLessonInput,
|   |   |                                        #   GetLessonsInput (+ requesterId/role), GetLessonsOutput
|   |   |-- Voucher/                             # CreateVoucherInput, UpdateVoucherInput, DeleteVoucherInput,
|   |   |                                        #   GetVouchersInput, VoucherOutput,
|   |   |                                        #   QuotePricingInput/Output, PurchaseCourseInput/Output
|   |   |-- Wallet/                              # InitTopUpOutput, AdminTopUpOutput
|   |   |-- User/                                # MyEnrollmentOutput
|   |   |-- CreateUserInput, PageResult (+ map())
|   |-- port/
|   |   |-- PaymentGateway.java                  # Interface — không bao giờ sửa
|   |   |-- PaymentInitResult.java               # Record kết quả từ gateway
|   |-- repository/
|   |   |-- Course/                              # CourseRepository, CourseSectionRepository,
|   |   |                                        #   CourseLessonRepository, EnrollmentRepository (+ findByUserId)
|   |   |-- User/UserRepository
|   |   |-- Voucher/                             # VoucherRepository, VoucherUsageRepository
|   |   |-- Wallet/WalletTransactionRepository
|   |   |-- RoleRepository
|   |-- usecase/
|       |-- Auth/                                # LoginUseCase, RegisterUseCase
|       |-- Course/                              # CreateCourse, UpdateCourse, DeleteCourse,
|       |                                        #   GetCourseList, GetCourseDetail,
|       |                                        #   PublishCourseUseCase, UnpublishCourseUseCase,
|       |                                        #   UpdateCoursePriceUseCase
|       |-- Section/                             # GetSections, CreateSection, UpdateSection, DeleteSection
|       |-- Lesson/                              # GetLessons (+ enrollment check), CreateLesson,
|       |                                        #   UpdateLesson, DeleteLesson
|       |-- User/                                # TopUpBalanceUseCase, AdminCreateUserUseCase,
|       |                                        #   GetMyEnrollmentsUseCase
|       |-- Voucher/                             # CreateVoucherUseCase, UpdateVoucherUseCase,
|       |                                        #   DeleteVoucherUseCase, GetVouchersUseCase,
|       |                                        #   QuotePricingUseCase, ApplyVoucherCheckoutUseCase
|       |-- Wallet/                              # InitTopUpUseCase, CompleteTopUpUseCase (shared),
|       |                                        #   AdminTopUpUseCase
|       |-- strategy/                            # CourseStrategyFactory, InstructorCourseStrategy,
|                                                #   StaffAdminCourseStrategy,
|                                                #   UsernameGeneratorFactory + *UsernameGeneratorStrategy
|
|-- domain/
|   |-- exception/                               # 30+ domain exceptions (Course, Section, Lesson,
|   |                                            #   User, Voucher, Wallet)
|   |-- model/
|   |   |-- User, Course, CourseSection, CourseLesson, Enrollment, Role, Permission
|   |   |-- Wallet/                              # WalletTransaction, TxStatus, TxSource
|   |   |-- Voucher/                             # Voucher, VoucherType, VoucherStatus,
|   |                                            #   VoucherScope, VoucherUsage, PriceQuote
|   |-- service/
|       |-- CourseOwnershipPolicy                # Pure static — kiểm tra ownership
|       |-- CourseAuthorizationService           # Kiểm quyền Course
|       |-- SectionAuthorizationService          # Kiểm quyền Section
|       |-- LessonAuthorizationService           # Kiểm quyền Lesson (+ authorizeView với enrollment)
|       |-- PricingEngine                        # Pure domain — tính giá
|       |-- VoucherValidator                     # Pure domain — validate voucher
|
|-- infrastructure/
    |-- config/
    |   |-- SecurityConfig                       # Filter chain, permit /ws/**, /webhook/**
    |   |-- WebSocketConfig                      # STOMP + SockJS + JWT interceptor
    |   |-- JwtAuthenticationFilter, JwtService
    |   |-- CustomAuthenticationEntryPoint
    |   |-- DataInitializer                      # Seed roles + permissions
    |   |-- DomainServiceConfig                  # Bean: PricingEngine, VoucherValidator
    |-- exception/
    |   |-- ErrorCode, ErrorResponse, GlobalExceptionHandler
    |-- payment/
    |   |-- MockPaymentGateway                   # @ConditionalOnProperty(mock)
    |   |-- (VietQrGateway — thêm sau, không sửa code cũ)
    |-- service/
        |-- PurchaseLedgerService                # JSONL audit log
        |-- WalletNotificationService            # WebSocket push WALLET_UPDATED
```
