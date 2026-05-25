# Kế hoạch: Course Visibility & Approval Workflow

## 1. Tổng quan yêu cầu

Hiện tại, khi `INSTRUCTOR` tạo khóa học, course **lập tức hiển thị công khai** với giá do giảng viên tự đặt — điều này có rủi ro:

1. **Giá chưa được duyệt**: Giảng viên có thể đặt giá tùy ý (quá cao, quá thấp, hoặc nhập sai số). Nếu công khai ngay, có thể bị "hack" mua với giá rẻ trước khi admin sửa.
2. **Nội dung chưa kiểm duyệt**: Section/Lesson chưa hoàn thiện vẫn xuất hiện trong danh sách công khai.
3. **Không có quy trình duyệt**: Hệ thống cần ít nhất một bước review chất lượng trước khi cho học viên mua.

**Giải pháp:**

Thêm 2 trạng thái cho Course thông qua 2 cờ boolean độc lập:

| Cờ | Ý nghĩa | Ai set |
|----|---------|--------|
| `published` | Khóa học đã được duyệt và đang công khai cho MEMBER xem/mua | STAFF / SUPER_ADMIN duyệt |
| `priceLocked` | Giá đã được khóa, INSTRUCTOR không sửa được (chỉ admin) | tự động set khi published, hoặc set thủ công |

**Quy trình end-to-end:**

```
1. INSTRUCTOR tạo course
   └─> course = { published: false, priceLocked: false, price: <tự nhập hoặc 0> }
       Mặc định ẨN, không xuất hiện trong /api/v1/courses (public listing).
       INSTRUCTOR vẫn xem được course của mình qua /api/v1/instructor/courses (sẽ thêm).

2. INSTRUCTOR thêm sections, lessons
   └─> Vẫn ở trạng thái draft (published = false)

3. INSTRUCTOR submit course để duyệt
   └─> Có thể là 1 endpoint riêng "submit for review" hoặc admin tự thấy danh sách pending.
       (Quyết định ở mục 8)

4. STAFF/SUPER_ADMIN duyệt
   ├─> GET /api/v1/admin/courses/pending  → xem các course chưa publish
   ├─> PUT /api/v1/admin/courses/{id}/price  → cập nhật giá nếu cần
   └─> POST /api/v1/admin/courses/{id}/publish → set published = true, priceLocked = true
                                                  course xuất hiện public ngay

5. STAFF/SUPER_ADMIN có thể unpublish nếu phát hiện vấn đề
   └─> POST /api/v1/admin/courses/{id}/unpublish → set published = false
       course biến mất khỏi public listing nhưng KHÔNG xóa enrollment đã có
```

**Phân quyền:**

| Hành động | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|-----------|--------|------------|-------|------------|-------------|
| Tạo course (mặc định ẩn) | [ ] | [x] | [x] | [x] | [x] |
| Sửa course của mình (khi chưa published) | [ ] | [x] | [x] | [x] | [x] |
| Sửa course của mình (sau publish) — không sửa price | [ ] | [x] (trừ price) | [x] | [x] | [x] |
| Sửa price | [ ] | [x] (chỉ khi !priceLocked) | [x] | [x] | [x] |
| Publish / Unpublish | [ ] | [ ] | [x] | [ ] | [x] |
| Xem course pending (chưa publish) | [ ] | [x] (chỉ của mình) | [x] | [x] | [x] |
| Xem course public | [x] | [x] | [x] | [x] | [x] |
| Mua course | [x] (chỉ published) | [ ] | [ ] | [ ] | [x] |

---

## 2. Cập nhật Permission Matrix

Thêm 2 permission mới vào `docs/permission-matrix.md`:

| # | Permission | Mô tả | MEMBER | INSTRUCTOR | STAFF | ADMIN_USER | SUPER_ADMIN |
|---|-----------|-------|--------|------------|-------|------------|-------------|
| 18 | `PUBLISH_COURSE` | Duyệt và publish khóa học | [ ] | [ ] | [x] | [ ] | [x] |
| 19 | `LOCK_COURSE_PRICE` | Khóa giá / sửa giá đã khóa | [ ] | [ ] | [x] | [ ] | [x] |

> `LOCK_COURSE_PRICE` cho phép admin sửa giá ngay cả khi `priceLocked = true`. INSTRUCTOR không có permission này nên một khi giá bị khóa (sau publish), họ phải nhờ admin.

**Cập nhật `DataInitializer`:** seed thêm 2 permission, gán đúng role.

---

## 3. API Endpoints

### 3A. Public — đã có, cần update logic filter

| Method | URL | Thay đổi |
|--------|-----|----------|
| `GET` | `/api/v1/courses` | **CHỈ trả về course có `published = true`** |
| `GET` | `/api/v1/courses/{id}` | Trả 404 nếu `published = false` (trừ khi requester là owner / STAFF / SUPER_ADMIN / ADMIN_USER) |
| `POST` | `/api/v1/courses/{id}/purchase` | **Reject với HTTP 400 (`COURSE_NOT_PUBLISHED`) nếu `published = false`** |
| `POST` | `/api/v1/courses/{id}/quote` | **Reject với HTTP 400 (`COURSE_NOT_PUBLISHED`) nếu `published = false`** |

### 3B. Instructor — endpoint mới

| Method | URL | Mô tả | Permission |
|--------|-----|-------|------------|
| `GET` | `/api/v1/instructor/courses` | Danh sách course của instructor (kể cả chưa publish) | đã đăng nhập + role INSTRUCTOR |
| `GET` | `/api/v1/instructor/courses/{id}` | Chi tiết course của mình (kể cả chưa publish) | đã đăng nhập + ownership |

### 3C. Admin — endpoint mới

| Method | URL | Mô tả | Permission |
|--------|-----|-------|------------|
| `GET` | `/api/v1/admin/courses/pending` | Danh sách course chưa publish (toàn hệ thống) | `PUBLISH_COURSE` |
| `GET` | `/api/v1/admin/courses` | Danh sách course toàn bộ (cả publish và pending) | `PUBLISH_COURSE` |
| `POST` | `/api/v1/admin/courses/{id}/publish` | Duyệt và publish course (set `published=true`, `priceLocked=true`) | `PUBLISH_COURSE` |
| `POST` | `/api/v1/admin/courses/{id}/unpublish` | Ẩn course đã publish | `PUBLISH_COURSE` |
| `PUT` | `/api/v1/admin/courses/{id}/price` | Cập nhật giá course (kể cả khi đã `priceLocked`) | `LOCK_COURSE_PRICE` |

---

## 4. Mô hình dữ liệu

### 4A. Bảng `courses` — thêm 2 cột

```sql
ALTER TABLE courses
  ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN price_locked BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN published_at DATETIME NULL,
  ADD COLUMN published_by BIGINT NULL,
  ADD INDEX idx_course_published (published);
```

> `published_at` và `published_by` không bắt buộc cho MVP nhưng nên có để audit. `published_by` FK đến `users(id)`.

### 4B. Domain `Course` — thêm field

```java
@Getter
@Builder
public class Course {
    private Long id;
    private String title;
    private String description;
    private int maxStudents;
    private int enrolledCount;
    private BigDecimal price;
    private Long instructorId;
    private boolean published;          // NEW
    private boolean priceLocked;        // NEW
    private LocalDateTime publishedAt;  // NEW (nullable)
    private Long publishedBy;           // NEW (nullable)
    private List<CourseSection> sections;

    // Factory: create() → mặc định published = false, priceLocked = false
    // Domain methods:
    public void publish(Long publisherId) { ... }
    public void unpublish() { ... }
    public void updatePrice(BigDecimal newPrice, boolean isAdmin) {
        if (this.priceLocked && !isAdmin)
            throw new CoursePriceLockedException();
        this.price = newPrice;
    }
}
```

---

## 5. Các file cần tạo mới / cập nhật

### 5A. Domain Layer

```
domain/exception/
  ├── CourseNotPublishedException.java       (NEW) — 400, mã COURSE_NOT_PUBLISHED
  ├── CoursePriceLockedException.java        (NEW) — 400, mã COURSE_PRICE_LOCKED
  └── CourseAlreadyPublishedException.java   (NEW) — 400, khi publish lần 2

domain/model/Course.java                     (UPDATE) — thêm 4 field + 3 method
```

### 5B. Application Layer

```
application/dto/Course/
  ├── PublishCourseInput.java                (NEW)
  ├── UnpublishCourseInput.java              (NEW)
  ├── UpdateCoursePriceInput.java            (NEW)
  ├── GetPendingCoursesInput.java            (NEW)
  ├── GetInstructorCoursesInput.java         (NEW)
  ├── CourseOutput.java                      (UPDATE — thêm published, priceLocked, publishedAt, publishedBy)
  └── CreateCourseInput.java                 (UPDATE — bỏ price hoặc giữ nhưng ghi chú: mặc định ẩn)

application/usecase/Course/
  ├── PublishCourseUseCase.java              (NEW)
  ├── UnpublishCourseUseCase.java            (NEW)
  ├── UpdateCoursePriceUseCase.java          (NEW)
  ├── GetPendingCoursesUseCase.java          (NEW)
  ├── GetInstructorCoursesUseCase.java       (NEW)
  ├── CreateCourseUseCase.java               (UPDATE — set published = false mặc định)
  ├── UpdateCourseUseCase.java               (UPDATE — chặn sửa price nếu priceLocked && !isAdmin)
  ├── GetCourseListUseCase.java              (UPDATE — filter published = true cho public)
  └── GetCourseDetailUseCase.java            (UPDATE — chặn nếu chưa publish và requester không phải owner/admin)

application/repository/Course/
  └── CourseRepository.java                  (UPDATE)
       findAllPublished(int page, int size)
       findAllPending(int page, int size)
       findAllByInstructorId(Long instructorId, int page, int size)
       findByIdIncludingUnpublished(Long id)
```

### 5C. Adapter Layer

```
adapter/repository/jpa/CourseEntity/
  └── CourseJpaEntity.java                   (UPDATE — thêm published, priceLocked, publishedAt, publishedBy)

adapter/repository/
  ├── JpaCourseRepository.java               (UPDATE — thêm method findByPublished... )
  └── CourseRepositoryImpl.java              (UPDATE)

adapter/mapper/
  └── CourseMapper.java                      (UPDATE — map 4 field mới)

adapter/dto/request/Course/
  ├── PublishCourseRequest.java              (NEW) — body có thể rỗng hoặc kèm ghi chú duyệt
  ├── UpdateCoursePriceRequest.java          (NEW) — { price: BigDecimal }
  └── CreateCourseRequest.java               (UPDATE — chú ý price là tùy chọn, mặc định 0)

adapter/dto/response/
  ├── CourseDetailResponse.java              (UPDATE — thêm published, priceLocked)
  ├── CourseListResponse.java                (UPDATE — thêm published)
  └── PendingCourseResponse.java             (NEW)

adapter/controller/
  ├── Admin/AdminCourseController.java       (NEW) — /api/v1/admin/courses/**
  ├── Course/InstructorCourseController.java (NEW) — /api/v1/instructor/courses/**
  └── Course/CourseController.java           (UPDATE — public endpoint filter published)
```

### 5D. Infrastructure

```
infrastructure/exception/
  ├── ErrorCode.java                         (UPDATE — thêm COURSE_NOT_PUBLISHED, COURSE_PRICE_LOCKED, COURSE_ALREADY_PUBLISHED)
  └── GlobalExceptionHandler.java            (UPDATE — thêm 3 handler)

infrastructure/config/
  └── DataInitializer.java                   (UPDATE — seed PUBLISH_COURSE, LOCK_COURSE_PRICE permissions + gán role)

infrastructure/service/
  └── CoursePublishLedgerService.java        (NEW, optional) — audit log JSONL cho event PUBLISH/UNPUBLISH
                                                                hoặc gộp vào PurchaseLedgerService
```

---

## 6. Luồng xử lý chi tiết

### 6A. POST /api/v1/courses (Tạo course bởi INSTRUCTOR)

```
Request → CourseController.create
  → CreateCourseUseCase.execute(input)
    → Course course = Course.create(...)
       course.published = false      // MẶC ĐỊNH ẨN
       course.priceLocked = false
    → CourseRepository.save(course)
    → return CourseOutput
  → 201 Created
```

### 6B. POST /api/v1/admin/courses/{id}/publish (Admin duyệt)

```
Request → AdminCourseController.publish
  → @PreAuthorize("hasAuthority('PUBLISH_COURSE')")
  → PublishCourseUseCase.execute({ courseId, publisherId })
    → CourseRepository.findById(courseId)         // 404 nếu không có
    → if (course.published) → CourseAlreadyPublishedException
    → if (course.price == null || course.price < 0) → IllegalStateException ("Giá không hợp lệ")
    → course.publish(publisherId)
       course.published = true
       course.priceLocked = true
       course.publishedAt = now()
       course.publishedBy = publisherId
    → CourseRepository.save(course)
    → audit log "COURSE_PUBLISHED"
    → return CourseOutput
  → 200 OK
```

### 6C. POST /api/v1/admin/courses/{id}/unpublish

```
Request → AdminCourseController.unpublish
  → UnpublishCourseUseCase.execute(...)
    → load course
    → course.unpublish()
       course.published = false
       (giữ priceLocked = true để khi publish lại không phải set giá lại)
    → save, audit log "COURSE_UNPUBLISHED"
    → return CourseOutput
```

### 6D. PUT /api/v1/admin/courses/{id}/price

```
Request → AdminCourseController.updatePrice
  → @PreAuthorize("hasAuthority('LOCK_COURSE_PRICE')")
  → UpdateCoursePriceUseCase.execute({ courseId, newPrice, isAdmin = true })
    → load course
    → course.updatePrice(newPrice, isAdmin = true)  // bypass priceLocked
    → save, audit log "COURSE_PRICE_UPDATED"
    → return CourseOutput
```

### 6E. PUT /api/v1/courses/{id} (INSTRUCTOR sửa course của mình)

```
Request → CourseController.update
  → UpdateCourseUseCase.execute(...)
    → load course, check ownership (đã có CourseOwnershipPolicy)
    → if (request.price != null && course.priceLocked && !requester.isAdmin())
        → throw CoursePriceLockedException
    → cho phép sửa các field khác (title, description, maxStudents)
    → save
```

### 6F. GET /api/v1/courses (Public list)

```
Request → CourseController.list (không cần JWT)
  → GetCourseListUseCase.execute({ filter: PUBLIC })
    → CourseRepository.findAllPublished(page, size)  // CHỈ published = true
    → return PageResult<CourseOutput>
```

### 6G. GET /api/v1/instructor/courses

```
Request → InstructorCourseController.list
  → @PreAuthorize("hasRole('INSTRUCTOR')")
  → GetInstructorCoursesUseCase.execute({ instructorId = JWT.userId, page, size })
    → CourseRepository.findAllByInstructorId(instructorId, page, size)  // KHÔNG filter published
    → return PageResult<CourseOutput> (kèm field published, priceLocked)
```

### 6H. GET /api/v1/admin/courses/pending

```
Request → AdminCourseController.pending
  → @PreAuthorize("hasAuthority('PUBLISH_COURSE')")
  → GetPendingCoursesUseCase.execute({ page, size })
    → CourseRepository.findAllPending(page, size)  // published = false
    → return PageResult<CourseOutput>
```

### 6I. POST /api/v1/courses/{id}/purchase (cập nhật check published)

```
Request → CourseController.purchase
  → ApplyVoucherCheckoutUseCase.execute(...)
    → CourseRepository.findById(courseId)
    → if (!course.published) → CourseNotPublishedException  ← MỚI
    → ... (logic cũ)
```

### 6J. POST /api/v1/courses/{id}/quote (cập nhật check published)

```
→ if (!course.published) → CourseNotPublishedException  ← MỚI
```

---

## 7. Thứ tự implement (10 bước)

1. **Domain exceptions** — `CourseNotPublishedException`, `CoursePriceLockedException`, `CourseAlreadyPublishedException`
2. **Domain model `Course`** — thêm 4 field + 3 method (`publish`, `unpublish`, `updatePrice`)
3. **Adapter JPA** — thêm 4 cột vào `CourseJpaEntity` + update `toDomain()`/`fromDomain()` + `CourseMapper`
4. **Application repository** — thêm `findAllPublished`, `findAllPending`, `findAllByInstructorId`, `findByIdIncludingUnpublished` vào `CourseRepository` + impl
5. **Application use cases** — `PublishCourseUseCase`, `UnpublishCourseUseCase`, `UpdateCoursePriceUseCase`, `GetPendingCoursesUseCase`, `GetInstructorCoursesUseCase`
6. **Update existing use cases** — `CreateCourseUseCase` (mặc định published = false), `UpdateCourseUseCase` (chặn sửa price khi locked), `GetCourseListUseCase` (filter), `GetCourseDetailUseCase` (chặn xem detail nếu chưa publish và requester không phải owner/admin)
7. **Adapter request/response DTOs** — thêm các DTO mới + update các response cũ
8. **Adapter controllers** — `AdminCourseController`, `InstructorCourseController` mới + update `CourseController`
9. **Infrastructure** — thêm `ErrorCode`, `GlobalExceptionHandler` cho 3 exception mới; cập nhật `DataInitializer` seed 2 permission + gán role
10. **Documentation** — cập nhật `docs/permission-matrix.md`, `docs/api-docs.md`, `docs/business-requirements.md` (mục 3.1) để mô tả workflow duyệt

---

## 8. Quyết định thiết kế cần xác nhận

- [ ] **Submit-for-review endpoint riêng?** Có cần `POST /api/v1/instructor/courses/{id}/submit-for-review` để đánh dấu trạng thái "ready to review" không, hay chỉ cần admin tự xem `/api/v1/admin/courses/pending` là đủ? Khuyến nghị: **MVP không cần submit endpoint**, admin tự duyệt. Có thể thêm sau (status = DRAFT / PENDING / PUBLISHED).
- [ ] **Trạng thái rõ hơn (enum thay vì 2 boolean)?** Thay vì `published` + `priceLocked` (4 tổ hợp), có thể dùng enum `CourseStatus { DRAFT, PENDING_REVIEW, PUBLISHED, UNPUBLISHED, REJECTED }`. Khuyến nghị **2 boolean trước** cho MVP đơn giản, refactor sau khi cần.
- [ ] **Course detail bị unpublish — học viên đã mua thì sao?** Học viên đã có enrollment vẫn truy cập được khóa học (qua endpoint riêng dạng `/api/v1/me/enrollments/{enrollmentId}` nếu có), nhưng không hiện trong public listing. Khuyến nghị **giữ enrollment**, chỉ ẩn khỏi public.
- [ ] **Section/Lesson trong course chưa publish**: Có cần publish riêng từng section/lesson không? Khuyến nghị **không**, gắn theo course là đủ cho MVP.
- [ ] **Instructor sửa course đã published**: Có cho phép sửa `title`, `description`, `maxStudents` sau publish không, hay phải unpublish trước? Khuyến nghị **cho phép sửa các field metadata** (trừ price), không cần unpublish, để tránh phải xét duyệt lại nhiều lần.
- [ ] **Audit log riêng cho course event?** Có cần file `logs/course_publish_ledger.jsonl` riêng hay gộp vào `purchase_ledger.jsonl` với `event = COURSE_PUBLISHED`? Khuyến nghị **gộp**, một ledger duy nhất cho mọi monetization event.
- [ ] **`published = false` thì có cho instructor preview/test không?** Khuyến nghị **có** — instructor xem được course của mình qua `/api/v1/instructor/courses/{id}` (kể cả chưa publish), vẫn xem được sections/lessons của mình.

---

## 9. Ghi chú kỹ thuật

- **Domain method placement**: `publish()`, `unpublish()`, `updatePrice(price, isAdmin)` đặt ở `Course` domain model. KHÔNG đặt ở use case (use case chỉ orchestrate, business rule ở domain).
- **`CourseOwnershipPolicy` mở rộng**: Thêm method `canEditPrice(Course course, Role role)` — return true nếu role là STAFF/SUPER_ADMIN/ADMIN_USER hoặc (INSTRUCTOR + isOwner + !course.priceLocked).
- **Migration**: Khi deploy schema mới, course hiện tại (đang public) → `UPDATE courses SET published = TRUE, price_locked = TRUE WHERE id IN (...)` để giữ behavior cũ. MVP không có data thật nên có thể bỏ qua, nhưng cần ghi rõ trong release notes.
- **Test concurrency**: Hai admin publish cùng 1 course đồng thời → chỉ 1 thành công, lần 2 ném `CourseAlreadyPublishedException`. Có thể dùng `@Version` (optimistic lock) trên `Course` để detect.
- **Reuse `PurchaseLedgerService`**: Mở rộng để log event `COURSE_PUBLISHED`, `COURSE_UNPUBLISHED`, `COURSE_PRICE_UPDATED` thay vì viết service mới.
- **Cross-feature với voucher**: `QuotePricingUseCase` và `ApplyVoucherCheckoutUseCase` (xem `plan-voucher-pricing.md`) phải kiểm tra `course.published` trước khi xử lý. Ném `CourseNotPublishedException` nếu chưa publish.
- **Frontend impact**: Các trang admin cần thêm tab "Pending courses" để duyệt; trang instructor cần hiển thị badge "Draft" cho course chưa publish; trang public chỉ hiển thị published course.

---

## 10. Files mới (tổng cộng ~20 files)

```
src/main/java/com/example/learning_system_spring/
├── domain/
│   ├── exception/
│   │   ├── CourseNotPublishedException.java          (NEW)
│   │   ├── CoursePriceLockedException.java           (NEW)
│   │   └── CourseAlreadyPublishedException.java      (NEW)
│   ├── model/
│   │   └── Course.java                                (UPDATE — 4 field + 3 method)
│   └── service/
│       └── CourseOwnershipPolicy.java                 (UPDATE — thêm canEditPrice)
├── application/
│   ├── dto/Course/
│   │   ├── PublishCourseInput.java                    (NEW)
│   │   ├── UnpublishCourseInput.java                  (NEW)
│   │   ├── UpdateCoursePriceInput.java                (NEW)
│   │   ├── GetPendingCoursesInput.java                (NEW)
│   │   ├── GetInstructorCoursesInput.java             (NEW)
│   │   └── CourseOutput.java                          (UPDATE — 4 field)
│   ├── repository/Course/
│   │   └── CourseRepository.java                      (UPDATE — 4 method)
│   └── usecase/Course/
│       ├── PublishCourseUseCase.java                  (NEW)
│       ├── UnpublishCourseUseCase.java                (NEW)
│       ├── UpdateCoursePriceUseCase.java              (NEW)
│       ├── GetPendingCoursesUseCase.java              (NEW)
│       ├── GetInstructorCoursesUseCase.java           (NEW)
│       ├── CreateCourseUseCase.java                   (UPDATE)
│       ├── UpdateCourseUseCase.java                   (UPDATE)
│       ├── GetCourseListUseCase.java                  (UPDATE)
│       └── GetCourseDetailUseCase.java                (UPDATE)
├── adapter/
│   ├── repository/jpa/CourseEntity/
│   │   └── CourseJpaEntity.java                       (UPDATE — 4 column)
│   ├── repository/
│   │   ├── JpaCourseRepository.java                   (UPDATE — query mới)
│   │   └── CourseRepositoryImpl.java                  (UPDATE)
│   ├── mapper/
│   │   └── CourseMapper.java                          (UPDATE)
│   ├── dto/request/Course/
│   │   ├── PublishCourseRequest.java                  (NEW)
│   │   ├── UpdateCoursePriceRequest.java              (NEW)
│   │   └── CreateCourseRequest.java                   (UPDATE)
│   ├── dto/response/
│   │   ├── CourseDetailResponse.java                  (UPDATE)
│   │   ├── CourseListResponse.java                    (UPDATE)
│   │   └── PendingCourseResponse.java                 (NEW)
│   └── controller/
│       ├── Admin/AdminCourseController.java           (NEW)
│       ├── Course/InstructorCourseController.java     (NEW)
│       └── Course/CourseController.java               (UPDATE)
└── infrastructure/
    ├── exception/
    │   ├── ErrorCode.java                             (UPDATE — 3 mã)
    │   └── GlobalExceptionHandler.java                (UPDATE — 3 handler)
    ├── service/
    │   └── PurchaseLedgerService.java                 (UPDATE — thêm event COURSE_*)
    └── config/
        └── DataInitializer.java                       (UPDATE — 2 permission)

docs/
├── plan-course-approval.md                            (THIS FILE)
├── permission-matrix.md                               (UPDATE)
├── api-docs.md                                        (UPDATE)
└── business-requirements.md                           (UPDATE — mục 3.1 ghi rõ workflow duyệt)
```

---

## 11. Mối quan hệ với `plan-voucher-pricing.md`

Hai feature **độc lập về kiến trúc** nhưng **giao điểm về luồng nghiệp vụ**:

| Giao điểm | Cách xử lý |
|-----------|------------|
| Quote / Checkout phải biết course đã publish | Thêm check `course.published` ở đầu `QuotePricingUseCase` và `ApplyVoucherCheckoutUseCase` → ném `CourseNotPublishedException` |
| Voucher `scope = SPECIFIC_COURSES` áp dụng cho course chưa publish | Cho phép tạo voucher trỏ đến course pending, nhưng khi quote/checkout vẫn bị chặn bởi `CourseNotPublishedException` ở bước trước voucher validation |
| Audit log | Cùng dùng `PurchaseLedgerService` mở rộng — event `COURSE_PUBLISHED`, `COURSE_UNPUBLISHED`, `VOUCHER_APPLIED`, ... |

**Thứ tự triển khai gợi ý:**
1. Course Approval trước (vì voucher phụ thuộc check `published`)
2. Voucher Pricing sau (build trên top của Course Approval)

Hoặc làm song song nếu có nhiều tay code, nhưng integration test cuối phải verify hai feature không xung đột.
