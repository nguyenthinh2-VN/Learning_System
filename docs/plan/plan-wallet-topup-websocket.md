# Kế hoạch: Wallet Top-up + WebSocket Realtime

**Ngày lập:** 2026-05-26  
**Phạm vi:** 2 chức năng nạp tiền (tự nạp + Admin thủ công) + WebSocket push realtime  
**Nguyên tắc thiết kế:** Open/Closed — sau này ghép VietQR chỉ thêm code mới, không sửa code cũ  
**Trạng thái:** Chờ implement

---

## Vấn đề cốt lõi cần giải quyết

VietQR callback về BE bằng URL (webhook), không có JWT token.  
→ BE cần biết "giao dịch ngân hàng này thuộc về user nào" mà không cần token.  
→ Giải pháp: **referenceCode** — mã duy nhất được tạo khi user khởi tạo nạp tiền,  
  user điền vào nội dung chuyển khoản, webhook parse ra → tìm đúng user trong DB.

---

## Kiến trúc tổng thể — Strategy Pattern

```
WalletController
    │
    ▼
InitTopUpUseCase  ──────────────────────────────────────────────────────┐
    │                                                                   │
    ▼                                                                   │
PaymentGateway (interface)          ← KHÔNG BAO GIỜ SỬA               │
    │                                                                   │
    ├── MockPaymentGateway           ← LÀM NGAY (tự approve)           │
    │       active khi: payment.provider=mock                          │
    │                                                                   │
    └── VietQrGateway                ← SAU NÀY THÊM (không đụng cũ)   │
            active khi: payment.provider=vietqr                        │
                                                                        │
WalletTransactionRepository ◄───────────────────────────────────────────┘
    │
    ▼
WalletNotificationService (WebSocket push)
```

**Khi thêm VietQR:** chỉ tạo class `VietQrGateway implements PaymentGateway` + `VietQrWebhookController` + đổi config `payment.provider=vietqr`. Không sửa `InitTopUpUseCase`, `WalletController`, hay bất kỳ code nào khác.

---

## 1. Database — Bảng mới

```sql
CREATE TABLE wallet_transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    reference_code VARCHAR(32) NOT NULL UNIQUE,   -- "NAP" + 9 ký tự random uppercase
    amount         DECIMAL(15,2) NOT NULL,
    status         ENUM('PENDING','COMPLETED','EXPIRED','FAILED') NOT NULL DEFAULT 'PENDING',
    source         ENUM('MOCK','VIETQR','ADMIN') NOT NULL,
    note           VARCHAR(255),                  -- admin ghi chú hoặc bank tx id
    created_at     TIMESTAMP NOT NULL,
    completed_at   TIMESTAMP,
    expired_at     TIMESTAMP NOT NULL,            -- PENDING hết hạn sau 15 phút
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Lý do thiết kế:**
- `reference_code` là cầu nối duy nhất giữa giao dịch ngân hàng và user
- `source=MOCK` cho giai đoạn dev, `source=VIETQR` khi ghép thật
- `expired_at` tránh pending transaction tồn tại mãi mãi
- `note` lưu bank transaction ID từ webhook hoặc lý do admin cộng

---

## 2. Interface PaymentGateway — Trái tim của thiết kế

```java
// application/port/PaymentGateway.java
// Interface này KHÔNG BAO GIỜ thay đổi
public interface PaymentGateway {

    /**
     * Khởi tạo yêu cầu thanh toán.
     * Trả về thông tin để FE hiển thị (QR, redirect URL, v.v.)
     */
    PaymentInitResult initPayment(String referenceCode, BigDecimal amount);

    /**
     * Tên provider — dùng để ghi vào wallet_transactions.source
     */
    String providerName();
}

// PaymentInitResult.java
public record PaymentInitResult(
    String referenceCode,
    BigDecimal amount,
    String displayData,   // QR URL (VietQR) hoặc message (Mock)
    String displayType,   // "QR_URL" | "MESSAGE"
    LocalDateTime expiredAt
) {}
```

---

## 3. Các PaymentGateway Implementation

### 3.1. MockPaymentGateway (làm ngay)

```java
// infrastructure/payment/MockPaymentGateway.java
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentInitResult initPayment(String referenceCode, BigDecimal amount) {
        // Không cần làm gì — trả về message hướng dẫn test
        return new PaymentInitResult(
            referenceCode,
            amount,
            "Dùng API POST /api/v1/webhook/mock?ref=" + referenceCode + " để giả lập thanh toán thành công",
            "MESSAGE",
            LocalDateTime.now().plusMinutes(15)
        );
    }

    @Override
    public String providerName() { return "MOCK"; }
}
```

### 3.2. VietQrGateway (sau này thêm — không đụng code cũ)

```java
// infrastructure/payment/VietQrGateway.java
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "vietqr")
public class VietQrGateway implements PaymentGateway {

    @Override
    public PaymentInitResult initPayment(String referenceCode, BigDecimal amount) {
        String qrUrl = "https://img.vietqr.io/image/" + bankId + "-" + accountNo
                     + "-compact2.png?amount=" + amount + "&addInfo=" + referenceCode;
        return new PaymentInitResult(referenceCode, amount, qrUrl, "QR_URL",
                                     LocalDateTime.now().plusMinutes(15));
    }

    @Override
    public String providerName() { return "VIETQR"; }
}
```

---

## 4. Backend API Endpoints

### 4.1. User khởi tạo nạp tiền

```
POST /api/v1/wallet/top-up/init
Authorization: Bearer <token>
Quyền: Đăng nhập (mọi role)
```

**Request Body:**
```json
{ "amount": 500000 }
```

**Response 200 (khi provider=mock):**
```json
{
  "status": 200,
  "message": "Tạo yêu cầu nạp tiền thành công",
  "data": {
    "referenceCode": "NAP4F8A2C1B3",
    "amount": 500000,
    "displayType": "MESSAGE",
    "displayData": "Dùng API POST /api/v1/webhook/mock?ref=NAP4F8A2C1B3 để giả lập thanh toán thành công",
    "expiredAt": "2026-05-26T10:15:00"
  }
}
```

**Response 200 (khi provider=vietqr — sau này):**
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
  }
}
```

**FE đọc `displayType` để biết render gì:**
- `"QR_URL"` → hiển thị ảnh QR
- `"MESSAGE"` → hiển thị text hướng dẫn (môi trường dev)

---

### 4.2. Mock Webhook — Giả lập thanh toán thành công (chỉ môi trường dev)

```
POST /api/v1/webhook/mock?ref={referenceCode}
Không cần Authorization
Chỉ active khi: payment.provider=mock
```

**Response 200:**
```json
{ "received": true, "message": "Đã cộng tiền thành công (mock)" }
```

Endpoint này dùng để test toàn bộ flow (pending → completed → WebSocket push) mà không cần ngân hàng thật.

---

### 4.3. VietQR Webhook Callback (sau này thêm — không đụng code cũ)

```
POST /api/v1/webhook/vietqr
Header: x-webhook-secret: <secret>    ← KHÔNG dùng JWT
```

**Body (do VietQR/ngân hàng gửi):**
```json
{
  "transactionId": "BANK_TX_20260526_001",
  "amount": 500000,
  "description": "NAP4F8A2C1B3 chuyen khoan",
  "transferType": "in"
}
```

**Xử lý BE:**
1. Verify `x-webhook-secret` header
2. Kiểm tra `transferType == "in"`
3. Scan `description` tìm token bắt đầu bằng `"NAP"` → `referenceCode`
4. Gọi `CompleteTopUpUseCase.execute(referenceCode, amount, transactionId)`
5. Trả về `{ "received": true }` — luôn 200 để tránh VietQR retry

**Lưu ý:** `CompleteTopUpUseCase` là use case **dùng chung** cho cả Mock và VietQR. Webhook controller chỉ parse request rồi gọi use case — không chứa business logic.

---

### 4.4. Admin cộng tiền thủ công

```
POST /api/v1/admin/users/{userId}/top-up
Authorization: Bearer <admin-token>
Quyền: SUPER_ADMIN
```

**Request Body:**
```json
{
  "amount": 200000,
  "note": "Bù lỗi giao dịch #123"
}
```

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
    "note": "Bù lỗi giao dịch #123"
  }
}
```

Admin cộng tiền trực tiếp — không qua pending, không qua PaymentGateway. Tạo `wallet_transactions` với `status=COMPLETED`, `source=ADMIN` ngay lập tức.

---

## 5. Use Cases — Phân tách rõ ràng

| Use Case | Ai gọi | Làm gì |
|---|---|---|
| `InitTopUpUseCase` | `WalletController` | Tạo pending tx + gọi `PaymentGateway.initPayment()` |
| `CompleteTopUpUseCase` | Mock webhook + VietQR webhook | Verify tx → cộng tiền → push WS |
| `AdminTopUpUseCase` | `AdminUserController` | Cộng tiền trực tiếp → push WS |

`CompleteTopUpUseCase` là **use case dùng chung** — cả Mock lẫn VietQR đều gọi cùng một use case này. Đây là lý do khi thêm VietQR không cần sửa business logic.

---

## 6. WebSocket — Thiết kế chi tiết

### BE: STOMP over WebSocket (Spring)

```
Handshake endpoint:  ws://localhost:8080/ws        (SockJS fallback)
STOMP broker:        /topic, /queue
User destination:    /user/queue/wallet            (Spring tự route theo principal)
```

**Message payload khi cộng tiền thành công:**
```json
{
  "event": "WALLET_UPDATED",
  "userId": 5,
  "newBalance": 700000.00,
  "addedAmount": 200000.00,
  "source": "ADMIN",
  "note": "Bù lỗi giao dịch #123",
  "timestamp": "2026-05-26T10:05:00"
}
```

`source` có thể là `"MOCK"`, `"VIETQR"`, hoặc `"ADMIN"`.

**Cách BE biết push tới user nào:**  
JWT trong WebSocket handshake header → Spring Security parse → set `Principal` → `SimpMessagingTemplate.convertAndSendToUser(username, "/queue/wallet", payload)`

---

### FE: Hướng dẫn kết nối WebSocket

**Thư viện cần cài:**
```bash
npm install @stomp/stompjs sockjs-client
```

**Code mẫu (React):**
```js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Gọi sau khi login thành công
function connectWallet(accessToken, onBalanceUpdate) {
  const client = new Client({
    // SockJS làm fallback nếu browser không hỗ trợ native WS
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),

    // Gửi JWT khi handshake — BE dùng để xác định user
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`
    },

    onConnect: () => {
      // Subscribe vào queue cá nhân — Spring tự route đúng user
      client.subscribe('/user/queue/wallet', (message) => {
        const data = JSON.parse(message.body);

        if (data.event === 'WALLET_UPDATED') {
          onBalanceUpdate(data);
          // data.newBalance   → cập nhật số dư trên UI
          // data.addedAmount  → hiển thị trong toast
          // data.source       → "MOCK" | "VIETQR" | "ADMIN"
        }
      });
    },

    onDisconnect: () => console.log('WebSocket disconnected'),
    reconnectDelay: 5000,  // tự reconnect sau 5s nếu mất kết nối
  });

  client.activate();
  return client; // lưu lại để deactivate khi logout
}

// Khi logout
function disconnectWallet(client) {
  client?.deactivate();
}
```

**Phân biệt source để hiển thị thông báo:**
```js
const messages = {
  MOCK:   `[Dev] Nạp tiền thành công: +${formatMoney(data.addedAmount)}đ`,
  VIETQR: `Nạp tiền thành công: +${formatMoney(data.addedAmount)}đ`,
  ADMIN:  `Tài khoản được cộng tiền: +${formatMoney(data.addedAmount)}đ`,
};
showToast(messages[data.source]);
```

**Lưu ý quan trọng cho FE:**

| Tình huống | Xử lý |
|---|---|
| Sau khi login | Gọi `connectWallet()` ngay |
| Khi logout | Gọi `disconnectWallet()` |
| Token hết hạn | Ngắt kết nối, refresh token, reconnect |
| Nhận `WALLET_UPDATED` | Cập nhật số dư + hiện toast |
| Mất mạng | `reconnectDelay: 5000` tự reconnect |

---

## 7. Cấu hình application.properties

```properties
# Payment provider: "mock" (dev) hoặc "vietqr" (production)
payment.provider=mock

# Chỉ dùng khi payment.provider=vietqr
vietqr.bank-id=MB
vietqr.account-no=1234567890
vietqr.account-name=CONG TY ABC
vietqr.webhook-secret=your-secret-key-here

# Wallet
wallet.topup.min-amount=10000
wallet.topup.pending-ttl-minutes=15
```

**Để chuyển sang VietQR thật:** chỉ đổi `payment.provider=vietqr` và điền thông tin ngân hàng. Không sửa bất kỳ dòng code nào.

---

## 8. Các file cần tạo/sửa (BE)

### Tạo mới

| File | Mô tả |
|---|---|
| `WalletTransaction` domain model | Enum `TxStatus` (PENDING/COMPLETED/EXPIRED/FAILED), `TxSource` (MOCK/VIETQR/ADMIN) |
| `WalletTransactionJpaEntity` | JPA entity theo pattern `UserJpaEntity` |
| `JpaWalletTransactionRepository` | Spring Data JPA interface |
| `WalletTransactionRepository` (port) | Interface trong application layer |
| `WalletTransactionRepositoryImpl` | Adapter |
| `PaymentGateway` (interface) | Port trong application layer |
| `PaymentInitResult` (record) | DTO kết quả từ gateway |
| `MockPaymentGateway` | `@ConditionalOnProperty(name="payment.provider", havingValue="mock")` |
| `InitTopUpUseCase` | Tạo pending tx + gọi `PaymentGateway.initPayment()` |
| `CompleteTopUpUseCase` | Verify tx → cộng tiền → push WS (dùng chung cho Mock + VietQR) |
| `AdminTopUpUseCase` | Cộng tiền trực tiếp → push WS |
| `WalletController` | `POST /api/v1/wallet/top-up/init` |
| `MockWebhookController` | `POST /api/v1/webhook/mock` — `@ConditionalOnProperty(...)` |
| `WebSocketConfig` | STOMP config |
| `WalletNotificationService` | Wrap `SimpMessagingTemplate` |

### Sửa

| File | Thay đổi |
|---|---|
| `AdminUserController` | Thêm `POST /{userId}/top-up` |
| `SecurityConfig` | Permit `/ws/**`, `/api/v1/webhook/**` |
| `application.properties` | Thêm payment config |

### Sau này thêm (không sửa file cũ)

| File | Mô tả |
|---|---|
| `VietQrGateway` | `implements PaymentGateway`, `@ConditionalOnProperty(havingValue="vietqr")` |
| `VietQrWebhookController` | `POST /api/v1/webhook/vietqr`, parse body → gọi `CompleteTopUpUseCase` |

---

## 9. Thứ tự implement

```
Bước 1: DB + Domain + Repository
  → SQL migration wallet_transactions
  → WalletTransaction domain model + enums
  → WalletTransactionJpaEntity
  → Repository interface + impl

Bước 2: WebSocket infrastructure
  → WebSocketConfig
  → WalletNotificationService
  → SecurityConfig cập nhật

Bước 3: PaymentGateway interface + MockPaymentGateway
  → PaymentGateway interface
  → PaymentInitResult record
  → MockPaymentGateway

Bước 4: Use Cases
  → CompleteTopUpUseCase (dùng chung)
  → InitTopUpUseCase
  → AdminTopUpUseCase

Bước 5: Controllers
  → WalletController (init top-up)
  → MockWebhookController (test flow)
  → AdminUserController thêm endpoint

Bước 6: Test end-to-end
  → POST /wallet/top-up/init → nhận referenceCode
  → POST /webhook/mock?ref=... → cộng tiền
  → FE nhận WebSocket event WALLET_UPDATED

Bước 7 (sau này): Ghép VietQR
  → Tạo VietQrGateway (không sửa code cũ)
  → Tạo VietQrWebhookController (không sửa code cũ)
  → Đổi application.properties: payment.provider=vietqr
```

---

## 10. Bảo mật

| Điểm | Cơ chế |
|---|---|
| Webhook VietQR | `x-webhook-secret` header, không dùng JWT |
| Mock webhook | Chỉ active khi `payment.provider=mock` (`@ConditionalOnProperty`) |
| WebSocket handshake | JWT trong `connectHeaders.Authorization` |
| Admin top-up | `@PreAuthorize("hasRole('SUPER_ADMIN')")` |
| Cộng tiền concurrent | Pessimistic lock (`SELECT ... FOR UPDATE`) |
| Webhook idempotency | Check `status != PENDING` trước khi xử lý, tránh cộng 2 lần |
| referenceCode | Random 9 ký tự uppercase, đủ ngẫu nhiên để không đoán được |
