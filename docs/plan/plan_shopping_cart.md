# Kế Hoạch Chức Năng: Giỏ Hàng & Thanh Toán Hàng Loạt (Shopping Cart & Bulk Checkout)

## 1. Mục tiêu
Cho phép người dùng gom nhiều khóa học (mua lẻ, từ lộ trình Admin tạo, hoặc từ AI tư vấn) vào một **Giỏ hàng (Cart)**. Sau đó tiến hành **Thanh toán hàng loạt (Bulk Purchase)** trong một lần giao dịch duy nhất thay vì phải bấm mua từng khóa.

---

## 2. Thiết kế Database

Tạo bảng `cart_items` để lưu trạng thái giỏ hàng (lưu DB giúp user không bị mất giỏ hàng khi đổi thiết bị):
- `id` (PK)
- `user_id` (FK)
- `course_id` (FK)
- `added_at` (TIMESTAMP)

*(Ràng buộc: `UNIQUE(user_id, course_id)` để một khóa học không bị add 2 lần vào giỏ).*

---

## 3. Kiến trúc Luồng Thanh toán (Bulk Checkout)

Hệ thống thanh toán hiện tại (`PurchaseCourseUseCase`) chỉ hỗ trợ mua 1 khóa học. Ta sẽ tạo thêm `BulkCheckoutUseCase`:
1. Nhận danh sách `courseIds` từ Frontend (các item user tick chọn trong giỏ).
2. Lặp qua danh sách để tính **Tổng tiền (Total Price)**. (Nếu có Voucher thì apply Voucher cho từng khóa học hoặc Voucher cho tổng đơn tùy logic hiện tại).
3. Kiểm tra số dư ví (`balance >= Total Price`).
4. **Trừ tiền một lần duy nhất** trong ví (Tạo 1 record `wallet_transactions` ghi chú là mua danh sách khóa học).
5. **Tạo hàng loạt record** trong bảng `enrollments` cho các khóa học đó.
6. Xóa các khóa học đã mua thành công khỏi bảng `cart_items`.

---

## 4. API Endpoints Dự Kiến

### Quản lý Giỏ hàng
- `GET /api/v1/cart`: Lấy danh sách khóa học trong giỏ hàng (kèm thông tin giá, thumbnail).
- `POST /api/v1/cart/items`: Thêm một hoặc nhiều khóa học vào giỏ.
  - Body: `{ "course_ids": [10, 15, 20] }`
- `DELETE /api/v1/cart/items/{courseId}`: Xóa 1 khóa học khỏi giỏ.
- `DELETE /api/v1/cart/items`: Xóa nhiều khóa (hoặc làm trống giỏ).

### Thanh toán
- `POST /api/v1/checkout/bulk`: Thực hiện thanh toán hàng loạt.
  - Body: `{ "course_ids": [10, 15], "voucher_code": "OPTIONAL" }`
  - Response: Trả về kết quả giao dịch và số dư ví còn lại.

---

## > [!IMPORTANT]
## Trạng Thái: Đang Lên Kế Hoạch (Chưa Code)

Kế hoạch này sinh ra để làm cầu nối cho **Lộ trình học tập** và **AI Chatbot**. User sẽ bỏ các khóa học được gợi ý vào giỏ và thanh toán 1 lần tại đây.
Bạn đồng ý với luồng xử lý Bulk Checkout trừ tiền 1 lần này chứ?
