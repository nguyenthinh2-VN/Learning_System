# Wallet, Purchase & Top-up (WebSocket)

[← Quay lại mục lục](./README.md)

Base URL: `http://localhost:8080/api/v1`

---

## 7. Wallet & Monetization

### 7.1. Nạp tiền vào ví (Top-up — legacy)

**Endpoint:** `POST /api/v1/users/me/top-up`

**Yêu cầu quyền:** Phải đăng nhập (MEMBER, STAFF, INSTRUCTOR...)

**Request Body:**
```json
{
  "amount": 500000.00
}
```

**Response 200 (Success):**
```json
{
  "status": 200,
  "message": "Nạp tiền thành công",
  "data": {
    "newBalance": 1500000.00
  },
  "timestamp": "2026-05-18T15:30:00"
}
```

**Response 400 (Bad Request - Số tiền <= 0):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Nạp tiền phải lớn hơn 0",
  "timestamp": "2026-05-18T15:30:00"
}
```

---

### 7.2. Mua Khóa Học

**Endpoint:** `POST /api/v1/courses/{id}/purchase`

**Yêu cầu quyền:** Phải đăng nhập. Hệ thống tự động trừ tiền trong ví. Nếu là nội bộ (`isInternal = true`), khóa học được tính giá 0đ.

> **Voucher tích hợp:** Body có thể chứa `voucherCode` (tùy chọn) để áp dụng giảm giá. Server LUÔN tính lại giá ở thời điểm checkout, không tin giá quote trước đó. Xem chi tiết tại [voucher.md](./voucher.md).

> **Anti-tampering:** DTO request CHỈ khai báo `voucherCode`. Mọi field giá khác (`price`, `originalPrice`, `discount`, `finalPrice`, `paidPrice`) nếu có trong body sẽ bị bỏ qua hoàn toàn.

**Path Variables:**
| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID của khóa học muốn mua |

**Request Body (tùy chọn):**
```json
{
  "voucherCode": "WELCOME50"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| voucherCode | String | No | 0–32 ký tự, regex `^[A-Za-z0-9_-]*$` |

**Response 200 (Success — không voucher):**
```json
{
  "status": 200,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": 1,
    "originalPrice": 500000.00,
    "discountAmount": 0.00,
    "finalPrice": 500000.00,
    "paidPrice": 500000.00,
    "voucherApplied": false,
    "voucherCode": null
  },
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 200 (Success — có voucher):**
```json
{
  "status": 200,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": 1,
    "originalPrice": 500000.00,
    "discountAmount": 50000.00,
    "finalPrice": 450000.00,
    "paidPrice": 450000.00,
    "voucherApplied": true,
    "voucherCode": "WELCOME50"
  },
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Course chưa publish:**
```json
{
  "code": "COURSE_NOT_PUBLISHED",
  "message": "Khóa học với ID 5 chưa được duyệt và công khai.",
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Đã enrolled:**
```json
{
  "code": "ALREADY_ENROLLED",
  "message": "Bạn đã đăng ký khóa học này rồi.",
  "timestamp": "2026-05-18T15:35:00"
}
```

**Response 400 — Số dư không đủ:**
```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Số dư không đủ để thanh toán khóa học.",
  "timestamp": "2026-05-18T15:35:00"
}
```

*Ghi chú:*
- Khóa học đã đầy → HTTP 400 với mã `BAD_REQUEST`.
- Internal Member (`isInternal = true`) luôn `paidPrice = 0`, voucher bị bỏ qua, không tạo Voucher_Usage.
- INSTRUCTOR / STAFF / ADMIN_USER gửi `voucherCode` → HTTP 403 (`VOUCHER_USE_DENIED`).
- Mọi lỗi voucher (`VOUCHER_NOT_FOUND`, `VOUCHER_EXPIRED`, `VOUCHER_USAGE_LIMIT_REACHED`...) xem [voucher.md](./voucher.md).

---

## 14. Wallet Top-up & WebSocket

### Kiến trúc tổng quan

Hệ thống nạp tiền được thiết kế theo **Strategy Pattern (Open/Closed Principle)**:
- Hiện tại dùng `MockPaymentGateway` (dev mode).
- Khi ghép VietQR: chỉ thêm `VietQrGateway` + `VietQrWebhookController`, **không sửa code cũ**.
- Chuyển đổi bằng config: `payment.provider=vietqr` trong `application-dev.yaml`.

### 14.1. Khởi tạo nạp tiền (User tự nạp)

**Endpoint:** `POST /api/v1/wallet/top-up/init`

**Yêu cầu quyền:** Đăng nhập (mọi role)

**Request Body:**
```json
{ "amount": 500000 }
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| amount | BigDecimal | Yes | >= 10,000đ |

**Response 200 (khi `payment.provider=mock`):**
```json
{
  "status": 200,
  "message": "Tạo yêu cầu nạp tiền thành công",
  "data": {
    "referenceCode": "NAP4F8A2C1B3",
    "amount": 500000,
    "displayType": "MESSAGE",
    "displayData": "Gọi POST /api/v1/webhook/mock?ref=NAP4F8A2C1B3 để giả lập thanh toán thành công",
    "expiredAt": "2026-05-26T10:15:00"
  },
  "timestamp": "2026-05-26T10:00:00"
}
```

**Response 200 (khi `payment.provider=vietqr` — sau này):**
```json
{
  "status": 200,
  "message": "Tạo yêu cầu nạp tiền thành công",
  "data": {
    "referenceCode": "NAP4F8A2C1B3",
    "amount": 500000,
    "displayType": "QR_URL",
    "displayData": "https://img.vietqr.io/image/MB-1234567890-compact2.png?amount=500000&addInfo=NAP4F8A2C1B3",
    "expiredAt": "2026-05-26T10:15:00"
  },
  "timestamp": "2026-05-26T10:00:00"
}
```

**FE đọc `displayType` để biết render gì:**
- `"QR_URL"` → hiển thị `<img src={displayData} />`
- `"MESSAGE"` → hiển thị text hướng dẫn (môi trường dev)

---

### 14.2. Mock Webhook — Giả lập thanh toán (chỉ dev)

**Endpoint:** `POST /api/v1/webhook/mock?ref={referenceCode}`

**Yêu cầu quyền:** Không cần (chỉ active khi `payment.provider=mock`)

**Response 200:**
```json
{
  "received": true,
  "message": "Đã cộng tiền thành công (mock)",
  "newBalance": 1500000.00
}
```

Sau khi gọi endpoint này, BE sẽ:
1. Tìm pending transaction theo `referenceCode`
2. Cộng tiền vào ví user
3. Push WebSocket event `WALLET_UPDATED` tới FE của user

---

### 14.3. Admin cộng tiền thủ công

**Endpoint:** `POST /api/v1/admin/users/{userId}/top-up`

**Yêu cầu quyền:** `SUPER_ADMIN`

**Request Body:**
```json
{
  "amount": 200000,
  "note": "Bù lỗi giao dịch #123"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| amount | BigDecimal | Yes | > 0 |
| note | String | No | Tối đa 255 ký tự |

**Response 200:**
```json
{
  "status": 200,
  "message": "Cộng tiền thành công",
  "data": {
    "userId": 5,
    "username": "MEM2B4A1D",
    "addedAmount": 200000,
    "newBalance": 700000,
    "note": "Bù lỗi giao dịch #123",
    "referenceCode": "NAPADMIN_XYZ"
  },
  "timestamp": "2026-05-26T10:05:00"
}
```

Sau khi cộng tiền, BE push WebSocket event `WALLET_UPDATED` tới FE của user được cộng tiền.

---

### 14.4. WebSocket — Nhận thông báo realtime

**Kết nối:**
```
ws://localhost:8080/ws  (SockJS endpoint)
```

**FE cần cài:**
```bash
npm install @stomp/stompjs sockjs-client
```

**Code kết nối (React):**
```js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

function connectWallet(accessToken, onBalanceUpdate) {
  const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`  // JWT để BE biết user nào
    },
    onConnect: () => {
      client.subscribe('/user/queue/wallet', (message) => {
        const data = JSON.parse(message.body);
        if (data.event === 'WALLET_UPDATED') {
          onBalanceUpdate(data);
        }
      });
    },
    reconnectDelay: 5000,
  });
  client.activate();
  return client;
}

// Khi logout
function disconnectWallet(client) {
  client?.deactivate();
}
```

**Payload nhận được khi ví được cập nhật:**
```json
{
  "event": "WALLET_UPDATED",
  "userId": 5,
  "newBalance": 700000.00,
  "addedAmount": 200000.00,
  "source": "ADMIN",
  "referenceCode": "NAPADMIN_XYZ",
  "note": "Bù lỗi giao dịch #123",
  "timestamp": "2026-05-26T10:05:00"
}
```

| Field | Giá trị có thể | Ý nghĩa |
|-------|---------------|---------|
| source | `MOCK` | Nạp qua mock (dev) |
| source | `VIETQR` | Nạp qua VietQR (production) |
| source | `ADMIN` | Admin cộng thủ công |

**Hiển thị toast theo source:**
```js
const messages = {
  MOCK:   `[Dev] Nạp tiền thành công: +${formatMoney(data.addedAmount)}đ`,
  VIETQR: `Nạp tiền thành công: +${formatMoney(data.addedAmount)}đ`,
  ADMIN:  `Tài khoản được cộng tiền: +${formatMoney(data.addedAmount)}đ`,
};
showToast(messages[data.source]);
```

---

### 14.5. Flow test end-to-end (môi trường dev)

```
1. Login → lấy JWT token
2. Kết nối WebSocket với token
3. POST /api/v1/wallet/top-up/init → nhận referenceCode (ví dụ: NAP4F8A2C1B3)
4. POST /api/v1/webhook/mock?ref=NAP4F8A2C1B3 → cộng tiền
5. FE nhận WebSocket event WALLET_UPDATED → cập nhật số dư trên UI
```

---

### 14.6. Hướng dẫn ghép VietQR (sau này)

Khi cần ghép VietQR thật, chỉ cần:

1. Tạo `VietQrGateway implements PaymentGateway` trong `infrastructure/payment/`
2. Tạo `VietQrWebhookController` trong `adapter/controller/`
   - Parse body từ VietQR
   - Tìm `referenceCode` trong `description`
   - Gọi `CompleteTopUpUseCase.execute(referenceCode, transactionId)`
   - Push WebSocket qua `WalletNotificationService`
3. Đổi config: `payment.provider=vietqr`

**Không sửa bất kỳ file nào khác.**

---

### Mã lỗi (Wallet & Purchase)

| HTTP Status | Error Code | Mô tả |
|-------------|-----------|-------|
| 400 | `BAD_REQUEST` | Số tiền không hợp lệ / khóa học đã đầy / `page` `size` ngoài khoảng |
| 400 | `COURSE_NOT_PUBLISHED` | Khóa học chưa được duyệt và công khai |
| 400 | `ALREADY_ENROLLED` | Đã đăng ký khóa học này rồi |
| 400 | `INSUFFICIENT_BALANCE` | Số dư không đủ để thanh toán |

---

## 14.7. Lịch sử giao dịch ví

**Endpoint:** `GET /api/v1/users/me/transactions`

**Yêu cầu quyền:** Đăng nhập (mọi role). Chỉ trả về giao dịch của chính người gọi (xác định qua `userId` trong JWT).

Trả về **cả tiền vào và tiền ra**:
- **Tiền vào (`direction = CREDIT`)**: nạp tiền — `source ∈ {MOCK, VIETQR, ADMIN}`.
- **Tiền ra (`direction = DEBIT`)**: mua khóa học — `source = PURCHASE`.

> Giao dịch mua khóa học chỉ được ghi khi `paidPrice > 0`. Khóa học 0đ (internal member / khóa miễn phí) không tạo bản ghi giao dịch.

**Query Parameters:**
| Field | Type | Default | Validation |
|-------|------|---------|------------|
| page | Integer | 0 | >= 0 |
| size | Integer | 20 | [1, 100] |

Sắp xếp theo `createdAt DESC` (mới nhất trước).

**Response 200:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 2,
    "totalPages": 1,
    "page": 0,
    "size": 20,
    "items": [
      {
        "id": 12,
        "referenceCode": "BUY1A2B3C4D5",
        "amount": 300000.00,
        "direction": "DEBIT",
        "status": "COMPLETED",
        "source": "PURCHASE",
        "note": "Mua khóa học #5",
        "createdAt": "2026-05-29T10:05:00",
        "completedAt": "2026-05-29T10:05:00"
      },
      {
        "id": 8,
        "referenceCode": "NAP4F8A2C1B3",
        "amount": 500000.00,
        "direction": "CREDIT",
        "status": "COMPLETED",
        "source": "MOCK",
        "note": null,
        "createdAt": "2026-05-28T09:00:00",
        "completedAt": "2026-05-28T09:01:00"
      }
    ]
  },
  "timestamp": "2026-05-29T12:00:00"
}
```

Trả về trang rỗng (`items: []`, không phải 403/404) nếu chưa có giao dịch nào.

| Field | Type | Mô tả |
|-------|------|-------|
| id | Long | ID giao dịch |
| referenceCode | String | Mã tham chiếu (`NAP...` nạp tiền, `BUY...` mua khóa học) |
| amount | BigDecimal | Số tiền, **luôn dương** (hướng tiền biểu diễn bằng `direction`) |
| direction | String | `CREDIT` (tiền vào) hoặc `DEBIT` (tiền ra) |
| status | String | `PENDING` / `COMPLETED` / `EXPIRED` / `FAILED` |
| source | String | `MOCK` / `VIETQR` / `ADMIN` / `PURCHASE` |
| note | String | Ghi chú (nullable) |
| createdAt | LocalDateTime | Thời điểm tạo giao dịch |
| completedAt | LocalDateTime | Thời điểm hoàn thành (nullable) |

**Response 400 (page/size không hợp lệ):**
```json
{
  "code": "BAD_REQUEST",
  "message": "size phải trong khoảng [1, 100]",
  "timestamp": "2026-05-29T12:00:00"
}
```
