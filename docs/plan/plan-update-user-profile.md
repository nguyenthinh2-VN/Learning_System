# Kế hoạch: Cập nhật thông tin cá nhân (Self-service Update Profile)

## Tổng quan

Hiện tại user chỉ có thể **xem** thông tin cá nhân (`GET /api/v1/users/me/profile`) nhưng chưa có cách **cập nhật**. Frontend (`fe/src/pages/ProfilePage.jsx`) đã dựng sẵn UI cho hai luồng nhưng BE chưa có endpoint:

1. **Sửa họ tên / avatar** — `EditNameModal` đang mock (cố tình reject với thông báo "API chưa hỗ trợ").
2. **Đổi mật khẩu** — `ChangePasswordModal` đã gọi `api.put('/users/me/password', ...)` nhưng BE trả 404.

Tính năng này bổ sung các endpoint self-service để đóng khoảng trống đó, kèm **upload ảnh avatar lưu trên ổ đĩa của BE** (local filesystem, không dùng cloud), theo đúng Clean Architecture của dự án.

---

## Phạm vi

### ✅ Trong phạm vi

| # | Endpoint | Mục đích |
|---|----------|----------|
| A | `PUT /api/v1/users/me/profile` | Cập nhật `name` (và `avatarUrl` dạng string) của chính mình |
| B | `PUT /api/v1/users/me/password` | Đổi mật khẩu của chính mình (yêu cầu mật khẩu hiện tại) |
| C | `POST /api/v1/users/me/avatar` | Upload file ảnh avatar (multipart), lưu trên đĩa BE, tự gán `avatarUrl` |
| D | Refactor `GET /api/v1/users/me/profile` | Đổi từ `Map<String,Object>` sang `UserProfileResponse` (DTO chuẩn) |

### ❌ Ngoài phạm vi (cố ý)

- ❌ Sửa `email` / `username` — định danh đăng nhập, cần luồng xác minh riêng (verify email). Không cho self-service sửa. **(Đã chốt)**
- ❌ Sửa `role` / `isInternal` / `balance` — chỉ admin được phép. **(Đã chốt)**
- ❌ Lưu trữ cloud (S3, Cloudinary...) — đợt này chỉ lưu **local filesystem**. Khi cần chuyển cloud, chỉ thêm implementation mới của port `FileStorageService` (xem mục thiết kế), không sửa use case/controller.
- ❌ Rotate / vô hiệu hóa JWT sau khi đổi mật khẩu — JWT stateless, token cũ vẫn hợp lệ tới khi hết hạn 24h. Ghi chú để xử lý sau nếu cần.

---

## Yêu cầu chi tiết

### A. `PUT /api/v1/users/me/profile`

- Yêu cầu đăng nhập (mọi role). Xác định người gọi qua claim `userId` trong JWT, nhất quán với `/me/top-up`, `/me/enrollments`.
- Request body:
  - `name` — **bắt buộc**, `@NotBlank`, độ dài 1–200.
  - `avatarUrl` — **tùy chọn**, tối đa 500 ký tự. `null` = **giữ nguyên** giá trị cũ; chuỗi rỗng `""` = **xóa** avatar (về null). Semantics nhất quán với `thumbnailUrl` của Course PUT.
- Dùng pessimistic lock `findByIdForUpdate` (nhất quán với top-up) để tránh ghi đè đồng thời.
- Trả về `UserProfileResponse` sau cập nhật.
- `404 USER_NOT_FOUND` nếu user không tồn tại (phòng thủ).

> Endpoint này cho phép set `avatarUrl` thủ công (vd: dán URL ngoài). Luồng upload file riêng ở endpoint C.

### B. `PUT /api/v1/users/me/password`

- Yêu cầu đăng nhập (mọi role). Người gọi xác định qua `userId` trong JWT.
- Request body:
  - `currentPassword` — **bắt buộc**, `@NotBlank`.
  - `newPassword` — **bắt buộc**, `@NotBlank`, độ dài 6–100 (khớp ràng buộc đăng ký/tạo user).
- Quy tắc nghiệp vụ:
  1. Verify `currentPassword` khớp hash hiện tại qua `PasswordEncoder.matches`. Sai → `400 INVALID_PASSWORD`.
  2. `newPassword` phải **khác** `currentPassword`. Trùng → `400 BAD_REQUEST` ("Mật khẩu mới phải khác mật khẩu cũ").
  3. Encode `newPassword` bằng `PasswordEncoder.encode` rồi lưu (KHÔNG lưu plaintext).
- Dùng pessimistic lock `findByIdForUpdate`.
- Response `200` chỉ trả message thành công, **không** trả token mới, **không** trả mật khẩu.

### C. `POST /api/v1/users/me/avatar` (upload file)

- Yêu cầu đăng nhập (mọi role). Người gọi xác định qua `userId` trong JWT.
- Content-Type: `multipart/form-data`, part tên **`file`**.
- Validation (ở controller + use case):
  - Content type phải thuộc whitelist: `image/jpeg`, `image/png`, `image/webp`. Khác → `400 INVALID_FILE_TYPE`.
  - Kích thước tối đa **2MB** (cấu hình qua `spring.servlet.multipart.max-file-size`). Vượt → Spring ném `MaxUploadSizeExceededException` → handler trả `400 FILE_TOO_LARGE`.
  - File rỗng → `400 BAD_REQUEST`.
- Lưu file vào thư mục cấu hình (`storage.local.base-dir/avatars/`), tên file random `{uuid}.{ext}` để tránh trùng + tránh path traversal (KHÔNG dùng tên gốc của client).
- Cập nhật `user.avatarUrl` = public URL (vd: `http://localhost:8080/uploads/avatars/{uuid}.png`), lưu DB bằng `findByIdForUpdate`.
- Response `200` trả `UserProfileResponse` (đã có `avatarUrl` mới).

> **Bảo mật:** chỉ chấp nhận phần mở rộng suy ra từ content-type whitelist, không tin `originalFilename`. Ghi file bằng `uuid` nội bộ. Không cho ghi đè ngoài thư mục upload.

### D. Refactor `GET /api/v1/users/me/profile`

- Thay `Map<String,Object>` trong `UserController.getMyProfile` bằng `UserProfileResponse` (DTO chuẩn) — gọn, type-safe, nhất quán với các endpoint khác.
- Bổ sung `GetMyProfileUseCase` ở application để controller mỏng, tái dùng `UserProfileOutput`.
- Response shape giữ nguyên field như cũ (`id, username, email, name, role, isInternal, balance, avatarUrl`) → **không phá vỡ FE**.

---

## Thiết kế theo Clean Architecture

### 1. Domain Layer (`domain/`)

**`domain/model/User.java`** — bổ sung hành vi nghiệp vụ (không setter công khai):

```java
public void changeName(String newName) {
    if (newName == null || newName.isBlank()) {
        throw new IllegalArgumentException("Name must not be blank");
    }
    this.name = newName.trim();
}

public void changeAvatar(String newAvatarUrl) {
    // null = giữ nguyên (no-op); chuỗi rỗng "" = xóa avatar
    if (newAvatarUrl != null) {
        this.avatarUrl = newAvatarUrl.isBlank() ? null : newAvatarUrl.trim();
    }
}

public void changePassword(String newEncodedPassword) {
    if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
        throw new IllegalArgumentException("Password must not be blank");
    }
    this.password = newEncodedPassword;
}
```

> Domain chỉ nhận mật khẩu **đã encode**. So khớp `currentPassword` và encode dùng `PasswordEncoder` nằm ở use case (application), vì `PasswordEncoder` là hạ tầng Spring Security — domain không được phụ thuộc.

**`domain/exception/InvalidPasswordException.java`** (mới) — kế thừa `RuntimeException`, dùng khi `currentPassword` sai.

### 2. Application Layer (`application/`)

**Port mới** (`application/port/FileStorageService.java`) — abstraction lưu trữ, framework-agnostic (không phụ thuộc `MultipartFile`):

```java
public interface FileStorageService {
    /**
     * Lưu file và trả về public URL để truy cập.
     * @param content      nội dung file (bytes)
     * @param contentType  MIME type đã được validate (image/png...)
     * @param subDir       thư mục con (vd: "avatars")
     * @return public URL đầy đủ
     */
    String store(byte[] content, String contentType, String subDir);
}
```

> Controller đọc `MultipartFile` → lấy `bytes` + `contentType` → truyền vào use case. Nhờ vậy application/use case không biết HTTP/multipart, đúng DIP. Khi đổi sang S3 chỉ thêm `S3FileStorageService implements FileStorageService`.

**DTO** (`application/dto/User/`):
- `UpdateMyProfileInput(Long userId, String name, String avatarUrl)`
- `ChangePasswordInput(Long userId, String currentPassword, String newPassword)`
- `UploadAvatarInput(Long userId, byte[] content, String contentType)`
- `UserProfileOutput(Long id, String username, String email, String name, String role, boolean isInternal, BigDecimal balance, String avatarUrl)` + static `from(User)`.

**Use case** (`application/usecase/User/`):
- `GetMyProfileUseCase.execute(Long userId) : UserProfileOutput` — `@Transactional(readOnly = true)`, `findById` → `UserProfileOutput.from`.
- `UpdateMyProfileUseCase.execute(UpdateMyProfileInput) : UserProfileOutput`
  - `@Transactional`, `findByIdForUpdate` → `changeName` + `changeAvatar` → `save` → `from`.
- `ChangeMyPasswordUseCase.execute(ChangePasswordInput) : void`
  - `@Transactional`, inject `PasswordEncoder`.
  - `findByIdForUpdate` → `matches(current, hash)` (sai → `InvalidPasswordException`) → kiểm tra new ≠ current (trùng → `IllegalArgumentException`) → `encode(new)` → `changePassword` → `save`.
- `UploadMyAvatarUseCase.execute(UploadAvatarInput) : UserProfileOutput`
  - `@Transactional`, inject `FileStorageService`.
  - Validate `contentType` thuộc whitelist (sai → `InvalidFileTypeException`).
  - `store(content, contentType, "avatars")` → url → `findByIdForUpdate` → `changeAvatar(url)` → `save` → `from`.

### 3. Adapter Layer (`adapter/`)

**Request DTO** (`adapter/dto/request/User/`):
- `UpdateProfileRequest { @NotBlank @Size(1,200) String name; @Size(max=500) String avatarUrl; }` + `toInput(userId)`.
- `ChangePasswordRequest { @NotBlank String currentPassword; @NotBlank @Size(6,100) String newPassword; }` + `toInput(userId)`.
- (Avatar dùng `MultipartFile` trực tiếp ở controller, không cần request DTO record.)

**Response DTO** (`adapter/dto/response/UserProfileResponse.java`):
- `UserProfileResponse(id, username, email, name, role, isInternal, balance, avatarUrl)` + `from(UserProfileOutput)`. Dùng cho cả `GET`, `PUT /me/profile`, `POST /me/avatar`.

**Controller** (`adapter/controller/UserController.java`):
- Refactor `GET /me/profile` → gọi `GetMyProfileUseCase`, trả `ApiResponse<UserProfileResponse>`.
- `@PutMapping("/me/profile")` — `@Valid UpdateProfileRequest` → `UpdateMyProfileUseCase`.
- `@PutMapping("/me/password")` — `@Valid ChangePasswordRequest` → `ChangeMyPasswordUseCase`.
- `@PostMapping(value="/me/avatar", consumes=MULTIPART_FORM_DATA)` — `@RequestParam("file") MultipartFile file` → `UploadMyAvatarUseCase`.

### 4. Infrastructure Layer (`infrastructure/`)

**`infrastructure/storage/LocalFileStorageService.java`** (mới) — implement `FileStorageService`:
- `@Component`, đọc config `storage.local.base-dir` + `storage.public-base-url` qua `@Value`.
- Ghi bytes ra `{base-dir}/{subDir}/{uuid}.{ext}` (ext suy từ contentType: png/jpg/webp).
- Trả `{public-base-url}/uploads/{subDir}/{uuid}.{ext}`.
- Tạo thư mục nếu chưa tồn tại; xử lý IO lỗi → `RuntimeException` (→ 500).

**`infrastructure/config/WebMvcConfig.java`** (mới) — `WebMvcConfigurer.addResourceHandlers`:
- Map `/uploads/**` → `file:{base-dir}/` để serve ảnh tĩnh đã upload.

**`SecurityConfig.java`** — permitAll cho `GET /uploads/**` (ảnh public, không cần JWT).

**`application-dev.yaml`** — thêm:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
storage:
  local:
    base-dir: ./uploads
  public-base-url: http://localhost:8080
```
> `./uploads` nên thêm vào `.gitignore` để không commit ảnh người dùng.

**`ErrorCode.java`** — thêm `INVALID_PASSWORD`, `INVALID_FILE_TYPE`, `FILE_TOO_LARGE`.

**`GlobalExceptionHandler.java`** — thêm handler:
- `InvalidPasswordException` → `400 INVALID_PASSWORD`.
- `InvalidFileTypeException` → `400 INVALID_FILE_TYPE`.
- `MaxUploadSizeExceededException` (Spring) → `400 FILE_TOO_LARGE`.

**`domain/exception/InvalidFileTypeException.java`** (mới) — `RuntimeException` cho content type không hợp lệ.

---

## Tài liệu (ghi vào folder `docs/API`)

- **`docs/API/user.md`** — thêm:
  - 16.2 `PUT /me/profile`
  - 16.3 `PUT /me/password`
  - 16.4 `POST /me/avatar` (multipart, whitelist type, giới hạn 2MB)
  - Cập nhật mô tả `GET /me/profile` (đã có `avatarUrl`), cập nhật bảng tóm tắt `/me/*`.
- **`docs/API/endpoints-summary.md`** — thêm 3 endpoint mới vào nhóm User Profile.
- **`docs/API/README.md`** — thêm `INVALID_PASSWORD`, `INVALID_FILE_TYPE`, `FILE_TOO_LARGE` (cân nhắc để ở bảng mã lỗi của user.md cho gọn).

---

## Kiểm thử

- `UpdateMyProfileUseCaseTest`: đổi tên thành công; avatarUrl null = giữ nguyên; avatarUrl "" = xóa; user không tồn tại → `UserNotFoundException`; tên rỗng → `IllegalArgumentException`.
- `ChangeMyPasswordUseCaseTest`: đổi thành công (verify `encode` + `save` hash mới); sai current → `InvalidPasswordException`; new trùng current → `IllegalArgumentException`.
- `UploadMyAvatarUseCaseTest`: upload png/jpg/webp thành công (mock `FileStorageService` trả url, verify `changeAvatar` + `save`); content type sai → `InvalidFileTypeException`.
- Domain `UserBehaviorTest`: `changeName`, `changeAvatar` (null/blank/giá trị), `changePassword` hợp lệ/không hợp lệ.
- Chạy `mvnw test` xác nhận build + test xanh trước khi báo hoàn thành. (Lưu ý: upload test ở tầng use case dùng `byte[]` nên không cần MockMvc; integration test multipart là tùy chọn.)

---

## Các file liên quan

### Tạo mới
- `application/port/FileStorageService.java`
- `application/dto/User/UpdateMyProfileInput.java`
- `application/dto/User/ChangePasswordInput.java`
- `application/dto/User/UploadAvatarInput.java`
- `application/dto/User/UserProfileOutput.java`
- `application/usecase/User/GetMyProfileUseCase.java`
- `application/usecase/User/UpdateMyProfileUseCase.java`
- `application/usecase/User/ChangeMyPasswordUseCase.java`
- `application/usecase/User/UploadMyAvatarUseCase.java`
- `domain/exception/InvalidPasswordException.java`
- `domain/exception/InvalidFileTypeException.java`
- `adapter/dto/request/User/UpdateProfileRequest.java`
- `adapter/dto/request/User/ChangePasswordRequest.java`
- `adapter/dto/response/UserProfileResponse.java`
- `infrastructure/storage/LocalFileStorageService.java`
- `infrastructure/config/WebMvcConfig.java`
- `src/test/.../UpdateMyProfileUseCaseTest.java`
- `src/test/.../ChangeMyPasswordUseCaseTest.java`
- `src/test/.../UploadMyAvatarUseCaseTest.java`

### Chỉnh sửa
- `domain/model/User.java` (thêm hành vi)
- `adapter/controller/UserController.java` (refactor GET + thêm 3 endpoint)
- `infrastructure/exception/ErrorCode.java` (thêm 3 mã lỗi)
- `infrastructure/exception/GlobalExceptionHandler.java` (thêm 3 handler)
- `infrastructure/config/SecurityConfig.java` (permit GET /uploads/**)
- `src/main/resources/application-dev.yaml` (multipart + storage config)
- `.gitignore` (thêm `/uploads` hoặc `uploads/`)
- `docs/API/user.md`, `docs/API/endpoints-summary.md`, `docs/API/README.md`

### Tham khảo (chỉ đọc)
- `application/usecase/Auth/LoginUseCase.java` (mẫu `PasswordEncoder.matches`)
- `application/usecase/User/TopUpBalanceUseCase.java` (mẫu `findByIdForUpdate` + `@Transactional`)
- `application/port/PaymentGateway.java` + `infrastructure/payment/MockPaymentGateway.java` (mẫu port/adapter để dựng `FileStorageService`)
- `application/repository/User/UserRepository.java` (đã đủ method, không cần thêm)
