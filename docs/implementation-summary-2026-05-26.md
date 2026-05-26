# Implementation Summary — 2026-05-26

Tổng kết tất cả thay đổi đã thực hiện cho feature **Wallet Top-up + WebSocket Realtime**.  
Compile: ✅ 0 errors. Build: ✅ SUCCESS.

Kế hoạch gốc: `docs/plan/plan-wallet-topup-websocket.md`

---

## Tổng quan những gì đã làm

Implement đầy đủ 2 chức năng nạp tiền + WebSocket push realtime theo **Open/Closed Principle**:
- User tự nạp tiền qua payment gateway (hiện tại: Mock, sau này: VietQR)
- Admin cộng tiền thủ công cho user bất kỳ
- Cả 2 đều push WebSocket event `WALLET_UPDATED` về FE realtime

Khi ghép VietQR thật: **chỉ thêm 2 file mới + đổi 1 dòng config**, không sửa bất kỳ code nào đã viết.

---

## Các file đã tạo mới

### Domain Layer

| File | Mô tả |
|------|-------|
| `domain/model/Wallet/TxStatus.java` | Enum: `PENDING`, `COMPLETED`, `EXPIRED`, `FAILED` |
| `domain/model/Wallet/TxSource.java` | Enum: `MOCK`, `VIETQR`, `ADMIN` |
| `domain/model/Wallet/WalletTransaction.java` | Domain model thuần — factory methods `createPending()`, `createCompleted()`, `reconstitute()`, method `complete()` với guard hết hạn |

### Application Layer

| File | Mô tả |
|------|-------|
| `application/port/PaymentGateway.java` | Interface port — **không bao giờ sửa** khi thêm provider mới |
| `application/port/PaymentInitResult.java` | Record kết quả từ gateway (`displayType`: `"QR_URL"` hoặc `"MESSAGE"`) |
| `application/repository/Wallet/WalletTransactionRepository.java` | Repository interface với `findPendingByRefForUpdate()` (pessimistic lock) |
| `application/dto/Wallet/InitTopUpOutput.java` | DTO output cho init top-up |
| `application/dto/Wallet/AdminTopUpOutput.java` | DTO output cho admin top-up |
| `application/usecase/Wallet/InitTopUpUseCase.java` | Tạo pending tx + gọi `PaymentGateway.initPayment()` |
| `application/usecase/Wallet/CompleteTopUpUseCase.java` | **Dùng chung** cho Mock + VietQR: verify pending → cộng tiền → trả `Result` để push WS |
| `application/usecase/Wallet/AdminTopUpUseCase.java` | Cộng tiền trực tiếp (COMPLETED ngay) + tạo audit record |

### Adapter Layer

| File | Mô tả |
|------|-------|
| `adapter/repository/jpa/WalletEntity/WalletTransactionJpaEntity.java` | JPA entity theo pattern `UserJpaEntity` — `toDomain()` + `fromDomain()` |
| `adapter/repository/JpaWalletTransactionRepository.java` | Spring Data JPA interface với `findPendingByRefForUpdate()` (`@Lock PESSIMISTIC_WRITE`) |
| `adapter/repository/WalletTransactionRepositoryImpl.java` | Adapter implement `WalletTransactionRepository` |
| `adapter/dto/request/Wallet/InitTopUpRequest.java` | Request record với `@NotNull`, `@DecimalMin(10000)` |
| `adapter/dto/request/Wallet/AdminTopUpRequest.java` | Request record với `amount` + `note` |
| `adapter/controller/WalletController.java` | `POST /api/v1/wallet/top-up/init` |
| `adapter/controller/MockWebhookController.java` | `POST /api/v1/webhook/mock?ref=...` — `@ConditionalOnProperty(payment.provider=mock)` |

### Infrastructure Layer

| File | Mô tả |
|------|-------|
| `infrastructure/payment/MockPaymentGateway.java` | `implements PaymentGateway`, `@ConditionalOnProperty(payment.provider=mock)` — trả `displayType=MESSAGE` |
| `infrastructure/config/WebSocketConfig.java` | STOMP over SockJS, JWT interceptor trên CONNECT frame, broker `/queue` + `/topic`, user prefix `/user` |
| `infrastructure/service/WalletNotificationService.java` | Wrap `SimpMessagingTemplate.convertAndSendToUser()` — push `WALLET_UPDATED` event, best-effort (không throw nếu WS fail) |

### Documentation & SQL

| File | Mô tả |
|------|-------|
| `docs/sql/wallet_transactions.sql` | SQL migration tạo bảng `wallet_transactions` — **chạy thủ công trước khi start app** |
| `docs/plan/plan-wallet-topup-websocket.md` | Kế hoạch đã cập nhật với thiết kế Open/Closed |
| `docs/api-docs.md` | Thêm Section 14 (Wallet Top-up & WebSocket) |
| `docs/project-overview.md` | Cập nhật section 2.4, 4, 5, 6 |

---

## Các file đã sửa

| File | Thay đổi |
|------|---------|
| `adapter/controller/AdminUserController.java` | Thêm `POST /{userId}/top-up` với `@PreAuthorize("hasRole('SUPER_ADMIN')")` + inject `AdminTopUpUseCase` + `WalletNotificationService` |
| `infrastructure/config/SecurityConfig.java` | Thêm `permitAll()` cho `/ws/**` và `/api/v1/webhook/**` |
| `src/main/resources/application-dev.yaml` | Thêm config `payment.provider=mock`, `vietqr.*`, `wallet.topup.*` |
| `pom.xml` | Thêm `spring-boot-starter-websocket` dependency |

---

## Hướng dẫn test

### Bước 0 — Chạy SQL migration

```sql
-- Chạy file: docs/sql/wallet_transactions.sql
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    reference_code VARCHAR(32) NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    status         ENUM('PENDING','COMPLETED','EXPIRED','FAILED') NOT NULL DEFAULT 'PENDING',
    source         ENUM('MOCK','VIETQR','ADMIN') NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMP NOT NULL,
    completed_at   TIMESTAMP NULL,
    expired_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_tx_ref UNIQUE (reference_code),
    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### Test Flow 1 — User tự nạp tiền (Mock)

**1. Login lấy token:**
```http
POST /api/v1/auth/login
{ "identifier": "user@example.com", "password": "123456" }
```

**2. Khởi tạo nạp tiền:**
```http
POST /api/v1/wallet/top-up/init
Authorization: Bearer <token>
{ "amount": 500000 }
```
→ Nhận `referenceCode` (ví dụ: `NAP4F8A2C1B3`) và `displayData` hướng dẫn gọi mock webhook.

**3. Kết nối WebSocket (trước bước 4):**
```js
// Dùng Postman WebSocket hoặc code FE mẫu trong api-docs.md Section 14.4
// Connect tới: ws://localhost:8080/ws
// Header: Authorization: Bearer <token>
// Subscribe: /user/queue/wallet
```

**4. Giả lập thanh toán thành công:**
```http
POST /api/v1/webhook/mock?ref=NAP4F8A2C1B3
```
→ Response: `{ "received": true, "message": "Đã cộng tiền thành công (mock)", "newBalance": ... }`  
→ WebSocket nhận event `WALLET_UPDATED` với `source: "MOCK"`

**Kiểm tra DB:**
```sql
SELECT * FROM wallet_transactions WHERE reference_code = 'NAP4F8A2C1B3';
-- status phải là COMPLETED, completed_at không null

SELECT balance FROM users WHERE id = <userId>;
-- balance phải tăng thêm 500000
```

---

### Test Flow 2 — Admin cộng tiền thủ công

**1. Login bằng SUPER_ADMIN:**
```http
POST /api/v1/auth/login
{ "identifier": "superadmin@example.com", "password": "..." }
```

**2. Cộng tiền cho user:**
```http
POST /api/v1/admin/users/{userId}/top-up
Authorization: Bearer <super-admin-token>
{ "amount": 200000, "note": "Bù lỗi giao dịch #123" }
```
→ Response: `{ "userId": ..., "username": ..., "addedAmount": 200000, "newBalance": ..., "note": "..." }`  
→ WebSocket của user đó nhận event `WALLET_UPDATED` với `source: "ADMIN"`

**Kiểm tra DB:**
```sql
SELECT * FROM wallet_transactions WHERE source = 'ADMIN' ORDER BY id DESC LIMIT 1;
-- status phải là COMPLETED ngay
```

---

### Test Flow 3 — Edge cases

| Tình huống | Cách test | Kết quả mong đợi |
|-----------|-----------|-----------------|
| Nạp tiền < 10,000đ | `amount: 5000` | 400 `VALIDATION_ERROR` |
| Gọi mock webhook 2 lần cùng ref | Gọi lại `POST /webhook/mock?ref=...` | 200 nhưng message lỗi "Không tìm thấy giao dịch PENDING" (idempotent) |
| Admin cộng tiền 0đ | `amount: 0` | 400 `VALIDATION_ERROR` |
| User không tồn tại (admin top-up) | `userId: 99999` | 404 `USER_NOT_FOUND` |
| Không có JWT khi init top-up | Bỏ header Authorization | 401 |
| Role không phải SUPER_ADMIN gọi admin top-up | Dùng token MEMBER | 403 |

---

## Thiết kế Open/Closed — Cách ghép VietQR sau này

Khi cần ghép VietQR thật, **chỉ làm 3 việc**:

**1. Tạo file mới** `infrastructure/payment/VietQrGateway.java`:
```java
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
    @Override public String providerName() { return "VIETQR"; }
}
```

**2. Tạo file mới** `adapter/controller/VietQrWebhookController.java`:
```java
@RestController
@ConditionalOnProperty(name = "payment.provider", havingValue = "vietqr")
public class VietQrWebhookController {
    // Parse body VietQR → tìm referenceCode trong description
    // Gọi completeTopUpUseCase.execute(referenceCode, transactionId)
    // Gọi walletNotificationService.pushWalletUpdated(...)
}
```

**3. Đổi config** `application-dev.yaml`:
```yaml
payment:
  provider: vietqr   # đổi từ mock → vietqr
```

**Không sửa bất kỳ file nào khác.**

---

## Trạng thái theo plan

| Bước trong plan | Trạng thái | Ghi chú |
|----------------|-----------|---------|
| DB migration SQL | ✅ Done | `docs/sql/wallet_transactions.sql` — chạy thủ công |
| `WalletTransaction` domain model | ✅ Done | Enums + factory methods + `complete()` |
| `WalletTransactionJpaEntity` | ✅ Done | Pattern `toDomain()` / `fromDomain()` |
| Repository interface + impl | ✅ Done | Pessimistic lock trên `findPendingByRefForUpdate` |
| `PaymentGateway` interface | ✅ Done | Port — không bao giờ sửa |
| `PaymentInitResult` record | ✅ Done | `displayType` + `displayData` |
| `MockPaymentGateway` | ✅ Done | `@ConditionalOnProperty(mock)` |
| `WebSocketConfig` | ✅ Done | STOMP + SockJS + JWT interceptor |
| `WalletNotificationService` | ✅ Done | Best-effort push, không throw |
| `CompleteTopUpUseCase` (dùng chung) | ✅ Done | Mock + VietQR đều gọi use case này |
| `InitTopUpUseCase` | ✅ Done | Inject `PaymentGateway` interface |
| `AdminTopUpUseCase` | ✅ Done | COMPLETED ngay, không qua pending |
| `WalletController` | ✅ Done | `POST /api/v1/wallet/top-up/init` |
| `MockWebhookController` | ✅ Done | `@ConditionalOnProperty(mock)` |
| `AdminUserController` thêm endpoint | ✅ Done | `POST /{userId}/top-up` — SUPER_ADMIN only |
| `SecurityConfig` cập nhật | ✅ Done | Permit `/ws/**`, `/api/v1/webhook/**` |
| `application-dev.yaml` | ✅ Done | `payment.provider=mock` + wallet config |
| `pom.xml` | ✅ Done | `spring-boot-starter-websocket` |
| `VietQrGateway` | ⏳ Chờ ghép | Tạo mới khi cần — không sửa code cũ |
| `VietQrWebhookController` | ⏳ Chờ ghép | Tạo mới khi cần — không sửa code cũ |
| Docs cập nhật | ✅ Done | `api-docs.md` Section 14, `project-overview.md` |
