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

---

## 5. Chi Tiết Triển Khai (Dự Kiến)

Dựa trên luồng `ApplyVoucherCheckoutUseCase` hiện tại, việc triển khai Bulk Checkout cần tuân thủ các quy tắc chống **Deadlock** và Clean Architecture.

### 5.1. Chiến Lược Chống Deadlock (Quan Trọng)
Trong `ApplyVoucherCheckoutUseCase`, lock được lấy theo thứ tự: User → Course → Voucher. 
Với Bulk Checkout có nhiều khóa học, việc lock đồng thời nhiều Course có thể dẫn đến **Deadlock** ở Database (nếu 2 user checkout cùng lúc 2 giỏ hàng chứa khóa A và B nhưng khác thứ tự).
**Giải pháp:** Bắt buộc phải **Sort danh sách `courseIds` theo thứ tự tăng dần** trước khi gọi `findByIdForUpdate` trong Database.

### 5.2. Thay Đổi Ở Domain Layer
- **Entity**: Thêm `CartItem`.
- **Repository**: Thêm `CartItemRepository`. Thêm method `findByIdInOrderByIdForUpdate(List<Long> courseIds)` vào `CourseRepository`.

### 5.3. Thay Đổi Ở Application Layer
- **Use Cases mới**:
  - `AddCourseToCartUseCase`
  - `RemoveCourseFromCartUseCase`
  - `GetCartItemsUseCase`
  - `BulkCheckoutUseCase` (Lock theo thứ tự User -> Courses đã sort -> Voucher, trừ tiền tổng, lưu Enrollment hàng loạt, xóa Cart).

---

## 6. Câu Hỏi Mở (Open Questions)

Xin vui lòng trả lời các câu hỏi sau trước khi chốt kế hoạch và bắt đầu code:

1. **Áp dụng Voucher**: Trong Bulk Checkout, nếu user nhập mã Voucher, Voucher đó sẽ được tính giảm giá trên **TỔNG ĐƠN HÀNG** hay chỉ áp dụng cho một khóa học duy nhất trong giỏ? (Hệ thống `PricingEngine` hiện tại đang thiết kế cho 1 `course_id`).
2. **Xử lý khóa học đã đăng ký (Already Enrolled)**: Nếu giỏ hàng có 3 khóa, trong đó 1 khóa user đã mua trước đó rồi, hệ thống nên `throw Exception` (từ chối toàn bộ giao dịch) hay tự động bỏ qua khóa đã mua và chỉ tính tiền/enroll các khóa còn lại?
3. **API Giỏ hàng**: API `GET /api/v1/cart` trả về danh sách khóa. Backend có cần tính luôn **Tổng tiền** của giỏ để Frontend hiển thị cho nhanh không?
4. **Script Migration**: File SQL tạo bảng `cart_items` sẽ được tạo rời trong `src/main/resources/sql/cart_items.sql`. Dự án của bạn chạy migration SQL tự động hay bạn sẽ chạy file này thủ công trên DB?
