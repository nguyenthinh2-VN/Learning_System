# Kế hoạch triển khai Backend & Đổi mới giao diện Tiến độ (Progress Page)

Biểu đồ Heatmap (Activity Calendar) giống GitHub được sử dụng thay thế biểu đồ tuần tĩnh cũ, giúp hiển thị "Chuỗi ngày học" (streak) trực quan trong một năm, tạo động lực tốt hơn cho học viên. Dữ liệu mock sẽ được thay thế bằng API thật gọi từ Backend.

## 1. Backend: Domain, Repository & API

### 1.1 DTOs
**`ProgressDataOutput.java`**:
- `OverviewDTO`: `totalHours` (tính tạm: số bài học hoàn thành * 0.5h), `completedCourses` (số khóa học đã xong 100% bài học), `streakDays` (số ngày liên tục hoàn thành bài học, tính ngược từ hôm nay/hôm qua).
- `List<HeatmapDataDTO>`: `[{ "date": "YYYY-MM-DD", "count": 2 }]` - Nhóm số lượng `LessonProgress` theo ngày trong vòng 1 năm qua.
- `List<OngoingCourseDTO>`: `[{ "id": 1, "title": "Khóa A", "thumbnailUrl": "...", "progressPercentage": 75, "lastAccessed": "2026-07-01T..." }]` - Danh sách khóa học có `progressPercentage < 100`.

### 1.2 Repositories
- Bổ sung `List<LessonProgress> findByUserId(Long userId)` trong `LessonProgressRepository` để kéo toàn bộ lịch sử học của User, phục vụ việc tính toán streak, heatmap, % hoàn thành khóa học.
- (Đã có sẵn) `EnrollmentRepository.findByUserId` để kéo các khóa học User đã đăng ký.
- Bổ sung logic lấy danh sách `Course` dựa trên `courseIds` từ Enrollment.

### 1.3 Use Case
- **`GetProgressUseCase.java`**: Xử lý logic nghiệp vụ gom nhóm data, tính streak, đếm số bài học của khóa học (`countByCourseId`), và sinh `ProgressDataOutput`.

### 1.4 Controller
- **`ProgressController.java`**:
  - `GET /api/v1/progress` (Yêu cầu xác thực `hasRole('MEMBER')` hoặc `isAuthenticated()`).

## 2. Các bước triển khai Backend

1. Cập nhật `LessonProgressRepository` và Jpa Repository tương ứng.
2. Tạo các DTO.
3. Cài đặt `GetProgressUseCase`.
4. Viết `ProgressController`.

(Note: Hiện tại chưa có field duration cho bài học nên tạm tính 1 bài học = 0.5 giờ (30 phút). Các achievements tĩnh trên FE đã bị xóa.)
