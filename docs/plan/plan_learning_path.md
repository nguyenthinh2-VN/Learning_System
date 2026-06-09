# Kế Hoạch Chức Năng: Lộ Trình Học Tập (Learning Paths) với Giỏ Hàng

## 1. Mục tiêu
Quản trị viên (Admin/Instructor) tạo các **Lộ trình học tập (Learning Path)** kết hợp nhiều khóa học lại với nhau theo chủ đề hoặc định hướng nghề nghiệp. 
Thay vì tự động gán, User sẽ lên hệ thống chọn Lộ trình, **đưa toàn bộ khóa học trong lộ trình đó vào Giỏ hàng** và thanh toán hàng loạt.
Khi học, User **bắt buộc phải học tuần tự** theo cấu trúc của lộ trình.

---

## 2. Thiết kế Database

1. **`learning_paths`**:
   - `id` (PK), `title`, `description`, `thumbnail_url`
   - `created_by`, `created_at`, `updated_at`

2. **`learning_path_courses`**:
   - `path_id` (FK), `course_id` (FK)
   - `order_index`: Thứ tự khóa học.

3. **Bỏ bảng `user_learning_paths`**: 
   - Vì user đã mua đứt khóa học qua Giỏ hàng (chuyển thành `enrollments`), việc kiểm soát học tuần tự sẽ được thực hiện trực tiếp khi user truy cập bài giảng (check `enrollments` của khóa học trước đó).

---

## 3. Luồng Mua Lộ Trình (Shopping Cart Integration)

1. Admin tạo Lộ trình gồm các khóa A (order=1), B (order=2), C (order=3).
2. Frontend có trang "Danh sách Lộ Trình". User bấm vào xem chi tiết Lộ trình.
3. User bấm nút **"Mua Lộ Trình / Thêm Lộ Trình vào Giỏ"**.
4. Frontend gọi API của Giỏ hàng truyền lên `course_ids` của khóa A, B, C.
5. User vào Giỏ hàng để tiến hành **Thanh toán hàng loạt (Bulk Checkout)**. Toàn bộ A, B, C được đưa vào My Enrollments.

---

## 4. Luồng Học Tuần Tự (Sequential Learning)

Sau khi user đã sở hữu khóa học (có trong My Enrollments), khi họ mở khóa B để học:
1. Domain Service (`LessonAuthorizationService`) sẽ kiểm tra: Khóa B có thuộc Lộ trình nào không?
2. Nếu có, nó là khóa thứ 2 (`order_index = 2`).
3. Dò tìm khóa học thứ 1 (`order_index = 1` của lộ trình đó). Kiểm tra xem User đã hoàn thành 100% khóa học thứ 1 chưa (Dựa trên tiến độ Lesson).
4. Nếu chưa hoàn thành khóa 1: Hệ thống trả về lỗi 403 / Báo cho Frontend khóa giao diện khóa B lại. Màn hình hiện thông báo *"Bạn phải hoàn thành Khóa A trước khi học Khóa B"*.
5. Nếu đã hoàn thành khóa 1: Được phép học khóa B bình thường.

---

## 5. API Endpoints Dự Kiến

### Dành cho Admin / Staff
- `POST /api/v1/admin/learning-paths`: Tạo lộ trình mới.
- `PUT /api/v1/admin/learning-paths/{id}`: Sửa lộ trình.
- `POST /api/v1/admin/learning-paths/{id}/courses`: Thêm/xóa khóa học vào lộ trình.

### Dành cho Public / User
- `GET /api/v1/learning-paths`: Xem danh sách các lộ trình có sẵn để mua.
- `GET /api/v1/learning-paths/{id}`: Xem chi tiết cấu trúc (gồm các khóa học nào) để quyết định bỏ vào giỏ hàng.

---

## > [!IMPORTANT]
## Trạng Thái: Đang Lên Kế Hoạch (Chưa Code)

Kế hoạch này đã được làm lại hoàn toàn theo hướng: **Không ép gán (Auto-assign) nữa**, mà **bán Lộ trình như một Gói (Bundle)**. User đưa vào Giỏ hàng -> Mua SLL -> Áp dụng luật học tuần tự.
Bạn xem lại luồng mới này đã hoàn toàn khớp với nghiệp vụ mong muốn chưa nhé!
