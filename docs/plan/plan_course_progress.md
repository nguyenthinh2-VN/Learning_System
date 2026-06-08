# Kế Hoạch Chức Năng: Trang Tiến Độ Học Tập (Gamification & Analytics)

## 1. Mục Tiêu
Thiết kế trang "Tiến độ của bạn" với giao diện hiện đại mang tính chất **Gamification** (Trò chơi hóa) như: theo dõi chuỗi ngày học, điểm XP, thời gian học trong tuần và các huy hiệu thành tựu (Achievements). Cung cấp danh sách khóa học đang diễn ra cùng gợi ý bài học tiếp theo.

---

## 2. Kế Hoạch Frontend (Giao Diện)

### 2.1. Bố cục trang `/progress`
1. **Header (Breadcrumb):** `Trang chủ > Tiến độ` -> `Tiến độ học tập của bạn`.
2. **Tổng Quan (4 Cards):**
   - **Tổng thời gian học:** (VD: 124 giờ).
   - **Khóa học hoàn thành:** (VD: 12 / 15).
   - **Chuỗi ngày học (Streak):** (VD: 15 ngày).
   - **Điểm tích lũy (XP):** (VD: 2.4k XP).
3. **Phân Tích & Thành Tựu (Grid 2 cột):**
   - Cột trái: **Biểu đồ "Hoạt động trong tuần"** (Dùng thư viện Recharts hiển thị số giờ học từ T2 -> CN).
   - Cột phải: **Danh sách "Thành tựu" (Badges)** hiển thị các huy hiệu đã đạt và chưa đạt (Học giả chăm chỉ, Người mới năng nổ,...).
4. **Khóa Học Đang Diễn Ra (Grid 2 cột):**
   - Cột trái: Danh sách các khóa học đang học dở (Kèm % tiến độ và "Học lần cuối: X giờ trước").
   - Cột phải: **Banner "Gợi ý tiếp theo"** (Nút "Tiếp tục học" dẫn thẳng tới bài giảng đang học dở của khóa truy cập gần nhất).

### 2.2. Component Cần Tạo
- `ProgressOverviewCards.jsx`: 4 thẻ thống kê.
- `WeeklyActivityChart.jsx`: Biểu đồ cột (Recharts).
- `AchievementsList.jsx`: Grid các huy hiệu.
- `OngoingCourses.jsx`: List khóa học đang học dở.
- `NextLessonBanner.jsx`: Banner gợi ý học tiếp.

---

## 3. Thiết Kế Cơ Sở Dữ Liệu (Mở rộng cho Gamification)

Để lấy đủ dữ liệu hiển thị lên UI, Backend cần lưu vết rất nhiều thông tin.

### 3.1. Bảng `user_learning_stats` (Gộp chung Streak & XP)
- `user_id` (PK)
- `total_learning_minutes`: Tổng số phút đã học.
- `total_xp`: Điểm kinh nghiệm tích lũy.
- `current_streak`, `longest_streak`, `last_activity_date`: Chuỗi học tập.

### 3.2. Bảng `daily_learning_logs` (Dữ liệu cho Biểu đồ)
Lưu thời gian học theo từng ngày để vẽ biểu đồ T2-CN.
- `id` (PK)
- `user_id` (FK)
- `log_date` (DATE)
- `learning_minutes` (Số phút học trong ngày đó)

### 3.3. Logic tính chuỗi ngày học (Streaks) & Thời gian học
Mỗi khi user **xem xong và hoàn thành 1 bài giảng** (Không tính việc chỉ đăng nhập):
- **Cập nhật Streak:**
  - Nếu `last_activity_date` là ngày **hôm nay**: Không làm gì (đã điểm danh).
  - Nếu `last_activity_date` là ngày **hôm qua**: `current_streak += 1`, cập nhật lại `last_activity_date = today`. Nếu `current_streak > longest_streak` thì cập nhật `longest_streak`.
  - Nếu `last_activity_date` trước ngày hôm qua (bỏ học ít nhất 1 ngày): `current_streak = 1`, `last_activity_date = today`.
- **Cập nhật Thời gian & Biểu đồ:**
  - Lấy `duration` (thời lượng) của bài giảng vừa xem xong cộng vào `total_learning_minutes`.
  - Ghi nhận `duration` đó vào bảng `daily_learning_logs` cho ngày hôm nay để vẽ biểu đồ T2-CN.

### 3.4. Cập nhật bảng `enrollments` (Tiến độ khóa học)
- Thêm cột `progress_percent` (INT): Lưu % hoàn thành khóa học để lấy ra cho nhanh.
- Thêm cột `last_accessed_at` (TIMESTAMP): Để biết khóa học nào truy cập gần nhất ("Học lần cuối 2 giờ trước") và ưu tiên hiển thị lên Banner Gợi ý.

### 3.5. Các Bảng Về Thành Tựu (Achievements)
1. `achievements`: Định nghĩa các huy hiệu (`id`, `name`, `icon_url`, `description`, `required_xp` hoặc `rule_type`).
2. `user_achievements`: Lưu huy hiệu user đã đạt (`user_id`, `achievement_id`, `achieved_at`).

---

## 4. Luồng Xử Lý & API (Backend)

### 4.1. API Lấy Dữ Liệu Trang Tiến Độ
`GET /api/v1/users/me/progress-dashboard`
Sẽ trả về một cục JSON tổng hợp (Tránh gọi quá nhiều API lẻ tẻ):
```json
{
  "overview": {
    "total_hours": 124,
    "completed_courses": 12,
    "total_enrolled_courses": 15,
    "streak_days": 15,
    "total_xp": 2400
  },
  "weekly_chart": [
    { "day": "T2", "hours": 2 },
    { "day": "T3", "hours": 3.5 }
  ],
  "achievements": [ ... ],
  "ongoing_courses": [
    { "course_id": 1, "title": "Spring Boot Clean Architecture", "progress": 78, "last_accessed": "2026-06-08T10:00:00Z" }
  ],
  "next_lesson_suggestion": {
    "course_id": 1,
    "lesson_id": 45,
    "title": "Hoàn thiện dự án Spring Boot"
  }
}
```

### 4.2. Logic "Cày" XP & Thời Gian
Khi bài giảng kết thúc: Frontend gọi API `POST /api/v1/lessons/{id}/complete`. Backend sẽ:
- Cộng XP (ví dụ +50 XP).
- Kiểm tra xem có đạt Achievement nào không.
- Cộng thời lượng bài học vào tổng thời gian học và biểu đồ tuần.
- Tính lại streak.
- Cập nhật lại % tiến độ khóa học (`enrollments.progress_percent`).
- Trả về thông tin lesson tiếp theo để gắn vào nút **"Tiếp tục học"**.

---

## > [!IMPORTANT]
## Trạng Thái: Đã Chốt Kế Hoạch (Sẵn sàng Code)

Toàn bộ luồng Logic đã được chốt theo phương án: **"Chỉ khi hoàn thành bài giảng mới được tính Streak và Thời gian"**, đồng thời tích hợp nút **"Tiếp tục học" (Play)** chuyển thẳng đến bài đang học dở.
