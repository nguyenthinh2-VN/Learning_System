# API Giỏ Hàng & Thanh Toán Hàng Loạt (Cart & Bulk Checkout)

Tài liệu mô tả các API liên quan đến quản lý giỏ hàng và thanh toán hàng loạt (gom nhiều khóa học thanh toán cùng lúc).

**Base URL**: `/api/v1`
**Authorization**: Bearer Token (yêu cầu đăng nhập với tất cả endpoint trừ khi có ghi chú khác).

---

## 1. Quản Lý Giỏ Hàng (Cart)

### 1.1. Lấy thông tin giỏ hàng
- **Endpoint:** `GET /cart`
- **Mô tả:** Lấy danh sách các khóa học đang nằm trong giỏ hàng của user hiện tại, kèm theo tổng số tiền. Tự động bỏ qua các khóa học đã bị gỡ (unpublish) khỏi hệ thống.
- **Headers:** 
  - `Authorization: Bearer <token>`
- **Response: 200 OK**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "items": [
      {
        "courseId": 10,
        "title": "Spring Boot Căn Bản",
        "price": 500000,
        "thumbnail": "https://example.com/thumb1.jpg",
        "addedAt": "2026-06-09T09:00:00"
      },
      {
        "courseId": 15,
        "title": "ReactJS Nâng Cao",
        "price": 600000,
        "thumbnail": "https://example.com/thumb2.jpg",
        "addedAt": "2026-06-09T09:05:00"
      }
    ],
    "totalPrice": 1100000
  },
  "timestamp": "2026-06-09T09:25:00"
}
```

### 1.2. Thêm khóa học vào giỏ hàng
- **Endpoint:** `POST /cart/items`
- **Mô tả:** Thêm một hoặc nhiều khóa học vào giỏ. Nếu khóa học đã có trong giỏ hoặc user đã sở hữu khóa học này, hệ thống sẽ bỏ qua hoặc báo lỗi phù hợp.
- **Headers:** 
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "course_ids": [10, 15]
}
```
- **Response: 200 OK**
```json
{
  "status": 200,
  "message": "Đã thêm vào giỏ hàng",
  "timestamp": "2026-06-09T09:25:00"
}
```

### 1.3. Xóa một khóa học khỏi giỏ hàng
- **Endpoint:** `DELETE /cart/items/{courseId}`
- **Mô tả:** Xóa một khóa học cụ thể khỏi giỏ hàng.
- **Headers:** 
  - `Authorization: Bearer <token>`
- **Response: 200 OK**
```json
{
  "status": 200,
  "message": "Đã xóa khỏi giỏ hàng",
  "timestamp": "2026-06-09T09:25:00"
}
```

### 1.4. Xóa toàn bộ giỏ hàng
- **Endpoint:** `DELETE /cart/items`
- **Mô tả:** Làm rỗng giỏ hàng của user hiện tại.
- **Headers:** 
  - `Authorization: Bearer <token>`
- **Response: 200 OK**
```json
{
  "status": 200,
  "message": "Đã làm trống giỏ hàng",
  "timestamp": "2026-06-09T09:25:00"
}
```

---

## 2. Thanh Toán Hàng Loạt (Bulk Checkout)

### 2.1. Thanh toán giỏ hàng
- **Endpoint:** `POST /checkout/bulk`
- **Mô tả:** Thanh toán cùng lúc nhiều khóa học. Hệ thống sẽ trừ tiền 1 lần duy nhất trong ví, đăng ký (enroll) tất cả khóa học hợp lệ và làm sạch giỏ hàng.
- **Đặc điểm:**
  - Tự động bỏ qua các khóa học đã đăng ký hoặc đang bị ẩn.
  - Áp dụng Voucher (nếu có) trên **TỔNG ĐƠN HÀNG** (tổng của các khóa chưa đăng ký).
- **Headers:** 
  - `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "course_ids": [10, 15],
  "voucher_code": "SUMMER_SALE" 
}
```
*(Lưu ý: `voucher_code` là optional)*

- **Response: 200 OK**
```json
{
  "status": 200,
  "message": "Thanh toán thành công",
  "data": {
    "enrolledCourseIds": [10, 15],
    "skippedCourseIds": [],
    "originalTotalPrice": 1100000,
    "discountAmount": 100000,
    "finalTotalPrice": 1000000,
    "paidPrice": 1000000,
    "voucherApplied": true,
    "voucherCode": "SUMMER_SALE"
  },
  "timestamp": "2026-06-09T09:25:00"
}
```

- **Lỗi thường gặp:**
  - `400 Bad Request`: Thiếu `course_ids` hoặc danh sách rỗng.
  - `400 Bad Request` (InsufficientBalanceException): Số dư trong ví không đủ.
  - `404 Not Found` (VoucherNotFoundException): Mã voucher không hợp lệ.
  - `400 Bad Request` (VoucherNotApplicableException): Voucher không áp dụng được cho bất kỳ khóa nào trong giỏ.
