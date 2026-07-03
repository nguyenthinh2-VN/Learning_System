# Implementation Plan: Mandatory Courses & Department Assignment

## Overview
Cập nhật hệ thống để quản lý các khóa học bắt buộc đối với từng phòng ban cụ thể. Khi admin tạo khóa học, họ có thể:
- Gán khóa học cho một phòng ban (nhập text).
- Đánh dấu khóa học này là **Bắt buộc** (`is_mandatory`).
- Đặt **Hạn chót hoàn thành** (`mandatory_deadline`).

> [!IMPORTANT]
> **Khả năng hiển thị (Visibility):** Các khóa học bắt buộc này VẪN sẽ hiển thị công khai trên hệ thống. Bất kỳ user nào (dù thuộc phòng ban khác) nếu thích đều có thể đăng ký học như bình thường. Việc gán "Bắt buộc" và "Phòng ban" CHỈ dùng để theo dõi tiến độ bắt buộc.
> 
> **Theo dõi Tiến độ (Progress):** Trong trang Tiến độ của mỗi user, hệ thống sẽ đối chiếu `department` của user đó với `assignedDepartment` của các khóa học bắt buộc. Nếu trùng khớp mà user chưa đăng ký học hoặc chưa hoàn thành (chưa đạt 100%), tiến độ sẽ hiển thị cảnh báo: *"khóa học [Tên khóa học] chưa đạt kết quả, chưa được đăng ký học do khóa bắt buộc"*.

## Architecture Decisions
- **Text-based Department**: Admin/User sẽ dùng chuỗi `String` cho trường phòng ban (`assignedDepartment` trên Course, và `department` trên User) để nhập liệu tự do.
- **Tính toán tiến độ**: `GetProgressUseCase` sẽ tìm các khóa học có `isMandatory = true` và `assignedDepartment == user.department`. Với các khóa này, hệ thống sẽ kiểm tra Enrollment và LessonProgress. Nếu không đạt, sẽ gộp chung vào danh sách `ongoingCourses` (hoặc một danh sách riêng) kèm cờ `isMandatory` và `statusMessage`.

## Open Questions (Cần bạn xác nhận)
> [!WARNING]
> **1. Department của User:** 
> Để hệ thống biết user nào phải học khóa học của phòng ban nào, bắt buộc phải có một trường `department` ở bảng `User`. Tôi đưa việc thêm trường `department` vào **Task 2**. Xin xác nhận nếu bạn đồng ý.
>
> **2. Giao diện hiển thị tiến độ:**
> Việc hiển thị danh sách khóa học bắt buộc chưa hoàn thành nên đưa thẳng vào mục danh sách các khóa học đang học ("Ongoing Courses") hiện tại, hay tạo một bảng/danh sách riêng mang tên "Khóa học bắt buộc của bạn" trên màn hình Tiến độ?

## Task List

### Phase 1: Foundation (Database & Domain)
- [ ] **Task 1:** Cập nhật DB schema (JPA Entities) thêm cột `assigned_department` (String), `is_mandatory` (Boolean), `mandatory_deadline` (DateTime) cho bảng `courses`.
- [ ] **Task 2:** Thêm trường `department` (String) cho bảng `users`.
- [ ] **Task 3:** Cập nhật Domain models `Course.java` và `User.java` để bao gồm các trường mới, cập nhật các method create/reconstitute.

### Phase 2: Backend Use Cases & API
- [ ] **Task 4:** Cập nhật DTO và UseCase tạo/sửa Course để admin có thể cấu hình `isMandatory`, `assignedDepartment`, `mandatoryDeadline`.
- [ ] **Task 5:** Cập nhật `GetProgressUseCase`. Lấy ra danh sách các khóa học có `isMandatory = true` và `assignedDepartment` khớp với `user.department`. Kiểm tra tiến độ của user với các khóa học này.
- [ ] **Task 6:** Nếu chưa đăng ký hoặc đăng ký mà progress < 100%, gán thông báo `"chưa đạt kết quả, chưa được đăng ký học do khóa bắt buộc"` và trả về cho frontend thông qua `ProgressDataOutput`.

### Phase 3: Frontend UI
- [ ] **Task 7:** Sửa component tạo/chỉnh sửa khóa học (Course Form) thêm Input (Phòng ban), Checkbox (Khóa học bắt buộc), DatePicker (Hạn chót).
- [ ] **Task 8:** Cập nhật User Profile hoặc trang Admin quản lý user để cho phép điền thông tin `department`.
- [ ] **Task 9:** Cập nhật UI trang Tiến độ (`EnrolledCourseDetailPage` / `Progress`). Hiển thị cảnh báo cho các khóa học bắt buộc bị thiếu / chưa hoàn thành.
