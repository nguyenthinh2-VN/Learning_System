# Kế Hoạch Chức Năng Cấp Chứng Chỉ (Certificate Generation)

Chào bạn, tôi đã phân tích thành công codebase. Dưới đây là kế hoạch chi tiết để xây dựng chức năng tạo chứng chỉ cho học viên khi hoàn thành khóa học.

## Giải pháp Thiết kế Chứng chỉ (Đã Chốt)

Dựa trên yêu cầu của bạn (dùng ảnh form cố định rồi xuất ra PDF), tôi đã thiết kế luồng xử lý tối ưu nhất đảm bảo **đẹp 100% y như bản thiết kế** và **chuyên nghiệp dưới dạng PDF**:

1. **Đầu vào (Template):** Bạn sẽ cung cấp một file ảnh chứng chỉ mẫu (PNG/JPG) làm phôi nền.
2. **Xử lý Ảnh (Backend):** Sử dụng thư viện `java.awt.Graphics2D` của Java để "vẽ" chữ đè lên ảnh. Để **không bị lỗi font tiếng Việt**, hệ thống sẽ load trực tiếp một file font `.ttf` (ví dụ: Roboto, Arial, hoặc font thiết kế riêng hỗ trợ tiếng Việt) vào cấu hình của `Graphics2D`. Nhờ đó, chữ in ra sẽ hiển thị hoàn hảo dấu tiếng Việt.
3. **Chuyển đổi sang PDF:** Sử dụng thư viện `Apache PDFBox` hoặc `iText` để tạo một file PDF mới, sau đó chèn bức ảnh vừa sinh ra ở bước 2 vào làm nội dung cho trang PDF (tràn viền).
4. **Kết quả:** Trả về cho User một file PDF chuẩn, không bị lỗi font hay vỡ khung CSS.

---

## Luồng Nghiệp Vụ & Cơ Sở Dữ Liệu

1. **Cơ sở dữ liệu (Database):**
   Tạo bảng `user_certificates` để lưu trữ chứng chỉ vĩnh viễn:
   - `id` (PK)
   - `user_id` (FK)
   - `course_id` (FK)
   - `certificate_url` hoặc `file_path`: Đường dẫn lưu file PDF.
   - `issued_at`: Ngày cấp.
   *(UNIQUE constraint trên `user_id` và `course_id` để 1 user chỉ có 1 chứng chỉ cho 1 khóa).*

2. **Backend API:**
   - Khi học viên hoàn thành khóa học, gọi API cấp chứng chỉ.
   - Kiểm tra điều kiện: `is_completed = true` cho mọi bài giảng.
   - Nếu đủ điều kiện: Thực hiện sinh PDF (Ảnh + Chữ -> PDF).
   - Lưu thông tin vào DB.
   - Trả về file PDF cho Frontend.
   - API `GET /api/v1/users/me/certificates`: Để lấy danh sách chứng chỉ đã nhận ở trang "Chứng chỉ của tôi".

3. **Frontend:**
   - Nút **"Tải Chứng Chỉ"** sáng lên khi tiến độ = 100%.
   - Có thêm mục menu "Chứng chỉ của tôi" để xem lại các chứng chỉ lưu trong DB.

---

## > [!IMPORTANT]
## Trạng Thái: Đang Lên Kế Hoạch (Đã Chốt Luồng)

Kế hoạch này đã được cập nhật với giải pháp **Ảnh -> PDF** và **Lưu DB vĩnh viễn** như bạn yêu cầu.
Ngay khi bạn có file ảnh template (phôi chứng chỉ trống), hãy tải nó vào thư mục dự án (ví dụ: `src/main/resources/templates/certificate_template.png`) và báo cho tôi, tôi sẽ bắt tay vào code!
