# Kế hoạch: Khóa học của tôi + Kiểm soát truy cập Lesson

## Tổng quan

Đóng hai lỗ hổng còn lại sau dự án voucher:

1. Member không có cách nào xem những gì họ đã mua.
2. Bất kỳ user đã đăng nhập nào cũng có thể lấy `contentUrl` của lesson từ khóa học họ chưa thanh toán (về cơ bản là xem lậu nội dung trả phí).

Tính năng này thêm endpoint tự phục vụ `Khóa học của tôi` và siết chặt quyền đọc lesson chỉ cho member đã đăng ký + các role đặc quyền.

---

## Yêu cầu

### A. `GET /api/v1/users/me/enrollments`

- Endpoint yêu cầu xác thực. **Mọi role** đều được phép (endpoint trả về enrollment của chính người gọi — trả về rỗng cho staff/instructor/admin chưa từng mua gì).
- Xác định người gọi từ JWT (claim `userId`), nhất quán với `/api/v1/users/me/top-up` hiện có.
- Tham số phân trang:
  - `page` — mặc định `0`, phải `>= 0`
  - `size` — mặc định `20`, phải trong khoảng `[1, 100]`
- Sắp xếp theo `enrolledAt DESC`.
- Mỗi item trả về:
  - `enrollmentId`
  - `courseId`
  - `paidPrice`
  - `enrolledAt`
- Response bọc trong envelope `ApiResponse` hiện có và dùng `PageResult<T>` cho metadata phân trang, theo đúng convention của các admin listing.
- Trả về trang rỗng (không phải `403`) khi người gọi chưa có enrollment nào.

### B. Kiểm soát truy cập Lesson (`GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons`)

Quy tắc mới được áp dụng bên trong `LessonAuthorizationService.authorizeView(course, requesterId, requesterRole, isEnrolled)`:

| Role | Quy tắc |
|------|---------|
| `SUPER_ADMIN` | Luôn được phép |
| `STAFF` | Luôn được phép |
| `INSTRUCTOR` | Được phép chỉ khi họ sở hữu khóa học (tái sử dụng `CourseOwnershipPolicy.isInstructorOwner`); instructor khác → `403` |
| `MEMBER` | Được phép chỉ khi `EnrollmentRepository.existsByUserIdAndCourseId(requesterId, courseId) == true`; ngược lại `403` |
| `ADMIN_USER` | `403` (không thuộc luồng học tập; nhất quán với posture `authorizeCreate`/`authorizeEditOrDelete` hiện tại) |

- **Không có ngoại lệ "khóa học miễn phí".** Giá `0` vẫn yêu cầu có enrollment row, vì `ApplyVoucherCheckoutUseCase` đã tạo enrollment cho giao dịch `0₫`.
- Tái sử dụng `LessonAccessDeniedException` hiện có → HTTP `403` qua `GlobalExceptionHandler`.
- `GetLessonsUseCase` được cập nhật để:
  1. Nhận `requesterId` + `requesterRole` trong `GetLessonsInput`.
  2. Load khóa học (đã có) → giữ nguyên `404` khi không tìm thấy.
  3. Với `MEMBER`, truy vấn `EnrollmentRepository.existsByUserIdAndCourseId`.
  4. Gọi `authorizeView` trước khi trả về danh sách lesson.
- `CourseLessonController.getLessons` truyền `requesterId`/`requesterRole` lấy từ JWT vào use case (controller đã parse claims sẵn; chỉ cần dừng việc bỏ qua chúng).
- `@PreAuthorize` trên endpoint GET vẫn để permissive (`MEMBER, INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN`); `403` chi tiết được raise bởi use case để thông báo lỗi vẫn mang tính domain-specific và được localize bằng tiếng Việt.

### C. Thay đổi Repository

- `EnrollmentRepository` (port) bổ sung:
  - `PageResult<Enrollment> findByUserId(Long userId, int page, int size)`
  - Giữ port không phụ thuộc framework; convert Spring `Page` bên trong impl.
- `JpaEnrollmentRepository` bổ sung:
  - `Page<EnrollmentJpaEntity> findByUserId(Long userId, Pageable pageable)`
- `EnrollmentRepositoryImpl` tạo `Pageable` (sort theo `enrolledAt DESC`), gọi JPA, và map sang `PageResult<Enrollment>`.

### D. Use Case

- Use case mới `GetMyEnrollmentsUseCase` trong `application/usecase/User/`:
  - Input: `requesterId`, `page`, `size`.
  - Validate `page >= 0`, `size trong [1, 100]` (throw `IllegalArgumentException` nếu input không hợp lệ — được xử lý bởi `GlobalExceptionHandler` thành `400`).
  - Trả về `PageResult<MyEnrollmentOutput>` với `MyEnrollmentOutput` gồm `enrollmentId, courseId, paidPrice, enrolledAt`.
  - `@Transactional(readOnly = true)`.

### E. Controller

- Mở rộng `UserController` hiện có với `GET /me/enrollments` (giữ namespace `/api/v1/users` nhất quán với `/me/top-up`).
- Tái sử dụng helper `getClaims(HttpServletRequest)` đã có sẵn.
- Trả về `ApiResponse<PageResult<MyEnrollmentResponse>>`.

---

## Ghi chú

### Bảo mật / Tính đúng đắn

- Authorization xảy ra **bên trong use case** (defense in depth), không chỉ dựa vào `@PreAuthorize`, theo đúng cách `authorizeEditOrDelete` được dùng trong `CreateLessonUseCase`/`UpdateLessonUseCase`.
- Kiểm tra enrollment dùng `existsByUserIdAndCourseId` đã có index — chỉ một query boolean, không có N+1.
- Từ chối truy cập lesson trả về cùng shape `403` như các `*AccessDeniedException` khác (được xử lý bởi `GlobalExceptionHandler`).
- Việc lookup khóa học trong `GetLessonsUseCase` đã throw `CourseNotFoundException` cho khóa học không tồn tại; thứ tự đó được giữ nguyên (`404` trước `403`).

### Ngoài phạm vi (rõ ràng)

- ❌ Thêm các trường tóm tắt khóa học (title/thumbnail/instructor) vào response enrollments — frontend sẽ join qua `GET /courses/{id}` hiện có.
- ❌ Thêm trường `completed` / tiến độ — đó là tính năng Theo dõi tiến độ trong tương lai.
- ❌ Thay đổi listing section (`/courses/{id}/sections`) — endpoint đó expose metadata section, không phải `contentUrl` trả phí. Ghi chú ở đây như một quyết định có chủ ý; có thể thêm sau nếu section listing bắt đầu lộ nội dung.
- ❌ Chạm vào các endpoint POST/PUT/DELETE lesson — authorization của chúng đã đúng.

### Kiểm thử

- Unit test `GetMyEnrollmentsUseCaseTest`:
  - Kết quả rỗng.
  - Kết quả một trang.
  - Kết quả nhiều trang với thứ tự sort được xác minh.
  - Từ chối `size` không hợp lệ (0, 101).
  - Từ chối `page` không hợp lệ (-1).
- Bổ sung unit test cho `LessonAuthorizationService.authorizeView` (hoặc qua `GetLessonsUseCaseTest`):
  - `SUPER_ADMIN` được phép.
  - `STAFF` được phép.
  - `INSTRUCTOR` chủ sở hữu được phép.
  - `INSTRUCTOR` không phải chủ sở hữu bị từ chối.
  - `MEMBER` đã đăng ký được phép.
  - `MEMBER` chưa đăng ký bị từ chối.
  - `ADMIN_USER` bị từ chối.
- Cập nhật `GetLessonsUseCaseTest` hiện có (tạo mới nếu chưa có) để mock `EnrollmentRepository` cho các nhánh mới.
- Cập nhật `docs/api-docs.md` và `docs/permission-matrix.md` để phản ánh:
  - Endpoint mới `GET /api/v1/users/me/enrollments`.
  - Quy tắc đọc lesson được siết chặt (MEMBER phải đã đăng ký; ADMIN_USER không còn được phép đọc).

---

## Các file liên quan

### Đọc / Chỉnh sửa

- `src/main/java/com/example/learning_system_spring/application/repository/Course/EnrollmentRepository.java`
- `src/main/java/com/example/learning_system_spring/adapter/repository/EnrollmentRepositoryImpl.java`
- `src/main/java/com/example/learning_system_spring/adapter/repository/JpaEnrollmentRepository.java`
- `src/main/java/com/example/learning_system_spring/application/usecase/Lesson/GetLessonsUseCase.java`
- `src/main/java/com/example/learning_system_spring/application/dto/Lesson/GetLessonsInput.java`
- `src/main/java/com/example/learning_system_spring/domain/service/LessonAuthorizationService.java`
- `src/main/java/com/example/learning_system_spring/adapter/controller/Course/Lesson/CourseLessonController.java`
- `src/main/java/com/example/learning_system_spring/adapter/controller/UserController.java`
- `src/main/java/com/example/learning_system_spring/application/dto/PageResult.java` *(tham khảo, có thể không cần thay đổi)*
- `src/main/java/com/example/learning_system_spring/adapter/dto/response/ApiResponse.java` *(tham khảo)*
- `docs/api-docs.md`
- `docs/permission-matrix.md`

### Tạo mới

- `src/main/java/com/example/learning_system_spring/application/usecase/User/GetMyEnrollmentsUseCase.java`
- `src/main/java/com/example/learning_system_spring/application/dto/User/MyEnrollmentOutput.java`
- `src/main/java/com/example/learning_system_spring/adapter/dto/response/MyEnrollmentResponse.java`
- `src/test/java/com/example/learning_system_spring/application/usecase/User/GetMyEnrollmentsUseCaseTest.java`
- `src/test/java/com/example/learning_system_spring/domain/service/LessonAuthorizationServiceTest.java` *(nếu chưa có)*
- `src/test/java/com/example/learning_system_spring/application/usecase/Lesson/GetLessonsUseCaseTest.java`

### Tham khảo (chỉ đọc)

- `src/main/java/com/example/learning_system_spring/application/usecase/Voucher/ApplyVoucherCheckoutUseCase.java` *(tạo ra các enrollment mà chúng ta sẽ liệt kê)*
- `src/main/java/com/example/learning_system_spring/domain/service/CourseOwnershipPolicy.java`
- `src/main/java/com/example/learning_system_spring/infrastructure/exception/GlobalExceptionHandler.java`
