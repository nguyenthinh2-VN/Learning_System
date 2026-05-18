# Tài liệu Đặc tả Nghiệp vụ (Business Requirements)

Tài liệu này mô tả chi tiết các luồng nghiệp vụ cốt lõi của hệ thống Learning System, giúp định hướng cho quá trình thiết kế Database và phát triển các tính năng.

---

## 1. Hệ thống Phân quyền (Roles & Permissions)

Hệ thống xoay quanh 5 nhóm đối tượng (Roles) chính, mỗi nhóm có vai trò và trách nhiệm chuyên biệt:

1. **SUPER_ADMIN**: Người quản trị tối cao, nắm toàn quyền kiểm soát hệ thống (Thiết lập cấu hình, phân quyền, quản lý thanh toán...).
2. **ADMIN_USER**: Quản trị viên cấp trung, chuyên trách về mảng nhân sự và người dùng. Nhiệm vụ chính là tạo và cấp phát tài khoản mới cho các đối tượng khác.
3. **STAFF**: Trợ lý / Nhân viên vận hành. Hỗ trợ các công việc quản lý, gửi thông báo, theo dõi lịch trình.
4. **INSTRUCTOR (Giảng viên)**: Người chịu trách nhiệm về chuyên môn học thuật. Có quyền quản lý toàn bộ vòng đời của Khóa học (Tạo, Sửa, Xóa, Upload nội dung).
5. **MEMBER (Học viên)**: Người dùng đầu cuối tham gia học tập. Đặc biệt, học viên được chia làm 2 phân loại:
   - **Nội bộ (Internal)**: Nhân viên trong công ty/tổ chức tham gia đào tạo.
   - **Bên ngoài (External)**: Khách hàng mua khóa học hoặc người dùng vãng lai tự đăng ký.

---

## 2. Luồng Quản lý Tài khoản & Định danh (User Identity)

Đặc thù của hệ thống là **không sử dụng Email làm ID đăng nhập duy nhất** mà sử dụng **Mã người dùng (Username / Custom ID)**. 

### 2.1. Cấp phát tài khoản
Việc tạo tài khoản phần lớn được thực hiện bởi `ADMIN_USER` hoặc `STAFF` thay vì người dùng tự đăng ký tự do.
- Khi Admin tạo một tài khoản mới, họ phải chọn **Vai trò (Role)** cho người đó.
- Hệ thống sẽ dựa vào Role để **tự động sinh ra Mã đăng nhập (Username)** có tính tuần tự và duy nhất. Cụ thể:
  - Nếu là Học viên (MEMBER): Sinh mã bắt đầu bằng `MEM` (VD: `MEM001`, `MEM002`). Đồng thời, Admin bắt buộc phải chọn phân loại học viên này là **Nội bộ** hay **Bên ngoài**.
  - Nếu là Giảng viên (INSTRUCTOR): Sinh mã bắt đầu bằng `GV` (VD: `GV001`).
  - Tương tự cho các nhóm khác: `STAFF001`, `AD001`.

### 2.2. Đăng nhập (Login Flow)
- Người dùng sử dụng Mã sinh tự động (VD: `MEM001`) và Mật khẩu để đăng nhập.
- Trên giao diện Frontend (dành cho Học viên), sẽ có một tùy chọn (Checkbox hoặc Tab) để xác nhận họ là **Học viên Nội bộ** hay **Học viên Bên ngoài**. Việc check tùy chọn này giúp hệ thống backend rẽ nhánh logic xác thực nhanh chóng và chính xác hơn (VD: Nội bộ có thể check thêm qua LDAP/AD công ty, hoặc chỉ đơn giản là chặn đăng nhập sai phân vùng).

---

## 3. Luồng Quản lý Khóa học và Học tập (Course Management)

Hệ thống quản lý nội dung học tập theo mô hình phân cấp, từ lớn xuống nhỏ:
- **Khóa học (Course)** ➔ chứa nhiều ➔ **Phần/Chương (Section/Part)** ➔ chứa nhiều ➔ **Bài giảng (Lesson/Video)**.

### 3.1. Quản trị Nội dung
- **INSTRUCTOR (Giảng viên)** là người trực tiếp tạo ra Khóa học.
- Họ sẽ thiết lập thông tin cơ bản (Tên, Mô tả, Số lượng tối đa).
- Sau đó, họ tiếp tục tạo các Chương (Section) và upload các video hoặc đính kèm link video lưu trữ ngoài (Youtube/Vimeo/S3) vào từng Bài giảng (Lesson).

### 3.2. Đăng ký & Tham gia học (Enrollment)
- Một Học viên (`MEMBER`) có thể đăng ký (Enroll) vào nhiều Khóa học khác nhau.
- Ngược lại, một Khóa học có nhiều Học viên theo học.
- Dữ liệu này được lưu vết chặt chẽ để phục vụ cho việc: Theo dõi tiến độ học (Progress), Chấm điểm (Grade), và Báo cáo thống kê (Report) sau này.
