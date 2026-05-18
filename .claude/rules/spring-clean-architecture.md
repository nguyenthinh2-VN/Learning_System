# Spring Clean Architecture Rules — Learning System

## Tổng quan kiến trúc

Dự án tuân theo **Clean Architecture** với 4 layer:

```
com.example.learning_system_spring
├── domain              // Enterprise business rules (thuần túy, không phụ thuộc ai)
├── application         // UseCase + Repository Interface + DTO
├── adapter             // Implement Repository Interface (Controller, JPA...)
└── infrastructure      // Config, Global Exception Handler
```

**Nguyên tắc phụ thuộc**: `adapter` → `application` → `domain` ← `infrastructure`
- Domain **không biết gì** về application, adapter, infrastructure.
- Application chỉ phụ thuộc vào Domain.
- Adapter implement Repository Interface từ application.

---

## Nguyên tắc SOLID (Bắt buộc)

### SRP — Single Responsibility Principle
- Mỗi class chỉ có **một lý do duy nhất để thay đổi**.
- **Domain Entity**: chỉ chứa dữ liệu + hành vi nghiệp vụ của chính entity đó. Không chứa logic persistence hay validation của tầng khác.
- **DomainService**: chỉ xử lý nghiệp vụ liên quan đến nhiều entity cùng loại, không gọi DB, không gọi external API.
- **UseCase**: mỗi UseCase = một class = một hành vi duy nhất. Không nhồi nhiều `execute()` vào chung một class.
- **Controller**: chỉ parse request + gọi UseCase + map response. Không chứa business logic.
- **Repository Interface**: chỉ định nghĩa contract, không chứa logic.

### DIP — Dependency Inversion Principle
- **UseCase (application) không phụ thuộc vào JPA implementation (adapter). Cả hai phụ thuộc vào Repository Interface (application).**
- UseCase chỉ phụ thuộc vào **Repository Interface** trong `application/repository/`, không biết đến JPA.
- Controller chỉ phụ thuộc vào **UseCase class**, không biết đến domain entity hay DB.
- Domain layer **không import bất kỳ class nào từ application, adapter hay infrastructure**.

### DI — Dependency Injection qua Constructor (Bắt buộc)

- **TUYỆT ĐỐI CẤM `@Autowired` field injection.**
- **TUYỆT ĐỐI CẤM `@Autowired` setter injection.**
- Chỉ dùng **Constructor Injection**. Spring tự resolve constructor param khi class chỉ có 1 constructor.
- Được phép dùng Lombok `@RequiredArgsConstructor` để sinh constructor tự động cho `final` field.

```java
// ĐÚNG — Constructor Injection thủ công
@Service
public class EnrollStudentUseCase {
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;

    public EnrollStudentUseCase(CourseRepository courseRepo, EnrollmentRepository enrollmentRepo) {
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
    }
}

// ĐÚNG — Lombok @RequiredArgsConstructor
@Service
@RequiredArgsConstructor
public class EnrollStudentUseCase {
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
}

// SAI — TUYỆT ĐỐI CẤM
@Service
public class EnrollStudentUseCase {
    @Autowired  // ← KHÔNG ĐƯỢC DÙNG
    private CourseRepository courseRepo;
}
```

---

## 1. Domain Layer (`domain/`)

Chứa **toàn bộ nghiệp vụ** (business rules). **Không import gì từ application, adapter, infrastructure.**

### Cấu trúc package

```
domain/
├── model/          // Entity, Value Object, Aggregate Root
├── service/        // DomainService (nghiệp vụ thuần túy)
└── exception/      // Domain exception (nghiệp vụ)
```

### Quy tắc

- **Entity**: Class thuần Java (POJO), **không annotation JPA**.
  - Constructor `private`/`protected` + factory method hoặc builder.
  - Mọi thay đổi trạng thái qua method có tên thể hiện hành vi nghiệp vụ (không setter công khai).
- **DomainService**: Chỉ chứa logic nghiệp vụ thuần túy. Không gọi repository, không biết đến DB.
  - Đặt tên theo hành vi: `EnrollmentValidator`, `CoursePricingPolicy`.
  - Không inject framework dependency.
- **Domain Exception**: Kế thừa `RuntimeException`. Tên: `<Entity><VấnĐề>Exception`.

### Ví dụ

```java
// domain/model/Course.java
public class Course {
    private Long id;
    private String title;
    private int maxStudents;
    private int enrolledCount;

    private Course(Long id, String title, int maxStudents) {
        this.id = id;
        this.title = title;
        this.maxStudents = maxStudents;
    }

    public static Course create(String title, int maxStudents) {
        if (maxStudents <= 0) throw new InvalidCourseCapacityException(maxStudents);
        return new Course(null, title, maxStudents);
    }

    // Dùng để tái tạo entity từ DB (chỉ repository impl mới gọi)
    public static Course reconstitute(Long id, String title, int maxStudents, int enrolledCount) {
        Course c = new Course(id, title, maxStudents);
        c.enrolledCount = enrolledCount;
        return c;
    }

    public boolean isFull() { return enrolledCount >= maxStudents; }

    public void enroll() {
        if (isFull()) throw new CourseFullException(id);
        enrolledCount++;
    }

    // getters...
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getMaxStudents() { return maxStudents; }
    public int getEnrolledCount() { return enrolledCount; }
}
```

---

## 2. Application Layer (`application/`)

Nơi chứa **UseCase** điều khiển toàn bộ quy trình, **Repository Interface** (abstraction), và **DTO**.

### Cấu trúc package

```
application/
├── usecase/        // UseCase implementation (điều khiển quy trình)
├── repository/     // Repository Interface (abstraction, không implement)
└── dto/            // Application DTO (input/output của UseCase)
```

### 2A. Repository Interface (`application/repository/`)

Là interface — định nghĩa **UseCase cần gì từ DB**, không quan tâm ai implement.
- Tên: `<Entity>Repository` (vd: `CourseRepository`, `UserRepository`).
- Chỉ chứa method cần cho nghiệp vụ, không lộ JPA detail.
- **KHÔNG** extends `JpaRepository` ở đây. `JpaRepository` chỉ dùng trong adapter.
- Adapter layer (`adapter/repository/`) sẽ implement interface này.

```java
// application/repository/CourseRepository.java
public interface CourseRepository {
    Optional<Course> findById(Long id);
    List<Course> findAll();
    Course save(Course course);
    void deleteById(Long id);
}
```

### 2B. UseCase (`application/usecase/`)

- Mỗi UseCase = **một class riêng** = một hành vi duy nhất.
- Đặt tên: `<ĐộngTừ><ĐốiTượng>UseCase` (vd: `EnrollStudentUseCase`, `CreateCourseUseCase`).
- Dùng `@Service` để Spring quản lý.
- Constructor injection: inject **Repository Interface** từ `application/repository/`.
- **Mỗi UseCase chỉ có MỘT public method** chính: `execute()`.
- **UseCase chỉ nhận và trả về DTO** — không nhận entity, không trả entity.
- `@Transactional` đặt trên method `execute()`.

### 2C. Application DTO (`application/dto/`)

- Input DTO: `<UseCaseName>Input` (vd: `EnrollStudentInput`).
- Output DTO: `<UseCaseName>Output` (vd: `EnrollStudentOutput`).
- DTO là record hoặc class `@Getter` Lombok, immutable.
- KHÔNG dùng entity làm DTO.

### Ví dụ UseCase

```java
// application/usecase/EnrollStudentUseCase.java
@Service
@RequiredArgsConstructor
public class EnrollStudentUseCase {
    private final CourseRepository courseRepo;        // Interface ở application/repository/
    private final EnrollmentRepository enrollmentRepo;

    @Transactional
    public EnrollStudentOutput execute(EnrollStudentInput input) {
        Course course = courseRepo.findById(input.courseId())
            .orElseThrow(() -> new CourseNotFoundException(input.courseId()));
        course.enroll();
        Enrollment enrollment = Enrollment.create(course.getId(), input.studentId());
        enrollmentRepo.save(enrollment);
        return EnrollStudentOutput.from(enrollment);
    }
}
```

```java
// application/dto/EnrollStudentOutput.java
public record EnrollStudentOutput(Long enrollmentId, Long courseId, Long studentId, LocalDateTime enrolledAt) {
    public static EnrollStudentOutput from(Enrollment e) {
        return new EnrollStudentOutput(e.getId(), e.getCourseId(), e.getStudentId(), e.getCreatedAt());
    }
}
```

---

## 3. Adapter Layer (`adapter/`)

Implement Repository Interface từ application. Đây là nơi duy nhất biết đến framework (JPA, HTTP...).

### Cấu trúc package

```
adapter/
├── controller/     // REST Controller
├── dto/
│   ├── request/    // Request DTO (từ client gửi lên)
│   └── response/   // Response DTO (trả về client)
├── repository/     // Implement Repository Interface từ application
│   └── jpa/        // JPA Entity (@Entity annotation)
└── mapper/         // JPA Entity ↔ Domain Entity mapper
```

### 3A. Controller

- **Nhiệm vụ**: Nhận HTTP request → parse sang Request DTO → gọi UseCase → nhận Output DTO → map sang Response DTO → trả JSON.
- KHÔNG chứa business logic. Chỉ làm: validate input, gọi usecase, map response.
- Đặt tên: `<ĐốiTượng>Controller` (vd: `CourseController`).
- Dùng `@RestController` + `@RequestMapping("/api/v1/...")`.
- **RESTful convention**:
  - URL dùng **số nhiều**, **kebab-case** (vd: `/api/v1/course-sections`).
  - `GET /api/v1/courses` — danh sách
  - `GET /api/v1/courses/{id}` — chi tiết
  - `POST /api/v1/courses` — tạo mới
  - `PUT /api/v1/courses/{id}` — cập nhật toàn phần
  - `PATCH /api/v1/courses/{id}` — cập nhật một phần
  - `DELETE /api/v1/courses/{id}` — xóa

### 3B. Controller DTO (`adapter/dto/`)

- **Request DTO**: Tên `<Entity><Action>Request` (vd: `CreateCourseRequest`). Dùng `@Valid` + Jakarta validation.
- **Response DTO**: Tên `<Entity><Action>Response` (vd: `CourseDetailResponse`).
- Controller DTO **riêng biệt** với Application DTO — không tái sử dụng để tránh coupling HTTP ↔ Business.
- Map qua method `toInput()` trên Request, `from()` static factory trên Response.

### Ví dụ Controller

```java
// adapter/controller/CourseController.java
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CreateCourseUseCase createCourseUseCase;
    private final GetCourseDetailUseCase getCourseDetailUseCase;

    @PostMapping
    public ResponseEntity<CreateCourseResponse> create(@Valid @RequestBody CreateCourseRequest req) {
        CreateCourseInput input = req.toInput();
        CreateCourseOutput output = createCourseUseCase.execute(input);
        return ResponseEntity.status(201).body(CreateCourseResponse.from(output));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponse> getDetail(@PathVariable Long id) {
        GetCourseDetailOutput output = getCourseDetailUseCase.execute(new GetCourseDetailInput(id));
        return ResponseEntity.ok(CourseDetailResponse.from(output));
    }
}
```

### 3C. Repository Implementation (`adapter/repository/`)

- **Implement Repository Interface** từ `application/repository/`.
- Dùng `@Repository`, constructor injection Spring Data JPA interface nội bộ.
- **JPA Entity** (có `@Entity`) nằm trong `adapter/repository/jpa/` — **tách biệt với Domain Entity**.
- Map JPA Entity ↔ Domain Entity ngay trong implementation.

```java
// adapter/repository/jpa/CourseJpaEntity.java
@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private int maxStudents;
    private int enrolledCount;

    public Course toDomain() {
        return Course.reconstitute(id, title, maxStudents, enrolledCount);
    }

    public static CourseJpaEntity fromDomain(Course course) {
        CourseJpaEntity e = new CourseJpaEntity();
        e.id = course.getId();
        e.title = course.getTitle();
        e.maxStudents = course.getMaxStudents();
        e.enrolledCount = course.getEnrolledCount();
        return e;
    }
}

// adapter/repository/CourseRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {  // ← implement từ application
    private final JpaCourseRepository jpaRepo;

    @Override
    public Optional<Course> findById(Long id) {
        return jpaRepo.findById(id).map(CourseJpaEntity::toDomain);
    }

    @Override
    public Course save(Course course) {
        return jpaRepo.save(CourseJpaEntity.fromDomain(course)).toDomain();
    }
}

// adapter/repository/JpaCourseRepository.java — Spring Data interface nội bộ, không public
interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
}
```

---

## 4. Infrastructure Layer (`infrastructure/`)

### Cấu trúc package

```
infrastructure/
├── config/         // @Configuration classes
└── exception/      // Global Exception Handler + ErrorResponse + ErrorCode
```

### 4A. Global Exception Handler (`infrastructure/exception/`)

**Đây là nơi tập trung xử lý toàn bộ exception** của hệ thống.

- Dùng `@RestControllerAdvice` + `@ExceptionHandler`.
- Bắt tất cả exception → map về HTTP status → trả JSON error thống nhất.
- Error Response format:

```json
{
  "error": {
    "code": "COURSE_NOT_FOUND",
    "message": "Không tìm thấy khóa học với id: 123",
    "timestamp": "2026-05-15T10:30:00Z"
  }
}
```

- **Cách tổ chức**:
  - Một class `GlobalExceptionHandler` (`@RestControllerAdvice`) — tập trung tất cả `@ExceptionHandler`.
  - Một record `ErrorResponse` — format lỗi chuẩn.
  - Một enum `ErrorCode` — mã lỗi (vd: `COURSE_FULL`, `ENROLLMENT_NOT_FOUND`).
- **Quy tắc mapping**:
  - Domain exception → 400 Bad Request hoặc 422 Unprocessable Entity
  - Không tìm thấy entity → 404 Not Found
  - Validation lỗi → 400 Bad Request
  - Lỗi chưa handled → 500 Internal Server Error (log đầy đủ, message generic)
- **KHÔNG** try-catch trong Controller hay UseCase. Tất cả để GlobalExceptionHandler lo.

### Ví dụ

```java
// infrastructure/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ErrorCode.COURSE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(CourseFullException.class)
    public ResponseEntity<ErrorResponse> handleCourseFull(CourseFullException ex) {
        return ResponseEntity.status(422).body(ErrorResponse.of(ErrorCode.COURSE_FULL, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống"));
    }
}

// infrastructure/exception/ErrorResponse.java
public record ErrorResponse(String code, String message, LocalDateTime timestamp) {
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, LocalDateTime.now());
    }
}
```

---

## 5. Quy tắc chung

- **Tên file**: PascalCase cho Java class, camelCase cho package.
- **Tên package**: lowercase, số ít (vd: `usecase`, không phải `usecases`).
- **Lombok**: Dùng `@Getter`, `@Builder`, `@RequiredArgsConstructor`. Tránh `@Data` (sinh setter không kiểm soát).
- **Không annotation JPA trên Domain Entity**: Domain entity là POJO thuần. JPA entity nằm trong `adapter/repository/jpa/`.
- **Không MapStruct/ModelMapper tự động**: Map thủ công qua method để tránh bug ẩn.
- **Không `@Autowired`**: Chỉ constructor injection.
- **Test**: Unit test cho DomainService và UseCase. Integration test cho Controller và Repository.

---

## 6. Luồng request điển hình

```
HTTP Request
  → Controller (adapter/controller)
    → Request DTO → Input DTO
    → UseCase.execute(input)  (application/usecase)
      → Repository Interface (application/repository)  ← Interface
      → Repository Impl (adapter/repository)           ← Implementation
        → JPA Entity ↔ Domain Entity map
      → Domain Entity / DomainService (domain)
      → Output DTO (application/dto)
    → Response DTO (map từ Output DTO)
  → JSON Response

Mọi exception → GlobalExceptionHandler → ErrorResponse JSON
```
