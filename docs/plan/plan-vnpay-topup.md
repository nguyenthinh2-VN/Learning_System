# Kế hoạch: Tích hợp nạp tiền qua VNPay (sandbox)

> Trạng thái: **DRAFT v2 — rà soát lần 2 xong, chờ duyệt để code**
> Liên quan: `application/port/PaymentGateway`, `infrastructure/payment/MockPaymentGateway`,
> `InitTopUpUseCase`, `CompleteTopUpUseCase`, `WalletNotificationService`,
> `fe/src/pages/WalletPage.jsx`, `application-dev.yaml`, `docs/API/wallet.md`.

## 0. Giải đáp: VietQR vs VNPay (vì sao đổi)

- **VietQR** (qua `img.vietqr.io`) chỉ là **ảnh QR tĩnh** chứa số tài khoản + nội dung CK.
  Nó KHÔNG phải cổng thanh toán — bản thân VietQR **không báo cho mình biết khi tiền về**.
  Muốn "tự động cộng tiền khi nhận" phải gắn thêm dịch vụ đọc biến động số dư ngân hàng
  (Casso/Sepay) → cần kết nối tài khoản ngân hàng thật. Đó là lý do câu 4.1 phải hỏi "nguồn
  nhận thanh toán".
- **VNPay** là **cổng thanh toán đầy đủ** (như Stripe/PayPal của VN): có **sandbox miễn phí**,
  tự xử lý trang thanh toán (thẻ ATM/QR/Visa), và **chủ động gọi về server mình** để xác nhận
  kết quả (IPN). → Không cần tài khoản ngân hàng thật, không cần Casso/Sepay. Hợp để demo +
  hợp UX "mở tab mới → thanh toán → quay lại báo thành công".

→ **Kết luận:** dùng VNPay sandbox. Bỏ hướng VietQR.

## 1. Luồng VNPay (redirect + xác nhận) — khác hẳn QR tĩnh

```
1. User nhập số tiền → FE gọi POST /wallet/top-up/init
2. BE tạo PENDING tx (referenceCode = vnp_TxnRef) + build URL thanh toán VNPay đã KÝ
   (HMAC-SHA512 bằng vnp_HashSecret) → trả về displayType="REDIRECT_URL", displayData=URL
3. FE MỞ TAB MỚI tới URL đó → hiển thị trang thanh toán VNPay (VNPay tự render QR/thẻ)
4. User thanh toán xong, VNPay trả kết quả qua 2 kênh:
   (a) [Production] IPN server→server: VNPay GỌI GET /api/v1/webhook/vnpay-ipn?vnp_...
       (chỉ hoạt động khi vnpay.complete-mode=IPN — cần URL public/ngrok).
   (b) Return browser: VNPay REDIRECT tab về vnp_ReturnUrl = /wallet/vnpay-return?vnp_...
5. Hoàn tất giao dịch (cộng tiền) tùy complete-mode:
   - complete-mode=RETURN (DEV mặc định): trang return gọi POST /wallet/top-up/vnpay-return
     → BE verify chữ ký của return params → completeTopUpUseCase → cộng tiền → push WebSocket.
   - complete-mode=IPN (PRODUCTION): IPN endpoint verify + complete; return chỉ hiển thị.
6. Tab ví gốc nhận WebSocket WALLET_UPDATED → số dư cập nhật + toast (đã có sẵn).
```

> **Nguyên tắc bảo mật cốt lõi:** Dù ở kênh nào, BE LUÔN **verify chữ ký HMAC-SHA512** trước
> khi cộng tiền, và **chỉ cộng đúng `tx.getAmount()` nội bộ** (không tin amount từ query).
> `complete-mode` quyết định **kênh nào được phép** gọi complete (chống cộng tiền 2 lần + tránh
> phụ thuộc trình duyệt ở production).

## 2. Quyết định đã chốt (theo trao đổi)

| # | Nội dung | Chốt |
|---|----------|------|
| Provider | Cổng thanh toán | **VNPay sandbox** |
| Xác thực callback | Cơ chế verify | **Chữ ký HMAC-SHA512 bằng `vnp_HashSecret`** (tương đương ý "dùng webhook-secret") |
| Phạm vi | (a) gateway + IPN/return có verify, chạy sandbox; (b) full provider | **(a)** — dùng sandbox VNPay end-to-end luôn (sandbox đã đủ thật) |
| Hiển thị thanh toán | QR ảnh vs redirect | **Redirect URL mở tab mới** (VNPay tự hiển thị QR/thẻ). KHÁC với img.vietqr — vì VNPay là cổng redirect |
| Đối soát số tiền | Chặn nếu lệch | **Có** — `vnp_Amount` (đã /100) phải khớp `tx.amount`, lệch → không complete, log `amount_mismatch` |
| Hoàn tất giao dịch (dev) | IPN (ngrok) vs complete-tại-return | **Complete-tại-return** làm chính cho dev (không cần ngrok, "cái nào dễ thì làm"). Vẫn verify chữ ký server-side. Endpoint IPN vẫn được tạo cho production. Chọn chế độ bằng config `vnpay.complete-mode`. |
| Credential sandbox | TmnCode + HashSecret | **Chưa có** — để placeholder trong config, bạn điền sau. Code build/test được bằng secret giả. |
| Trang return | Layout | **Trong `AppLayout` (có sidebar)** — đồng bộ giao diện, dễ quay lại Ví. |
| Mock gateway | Giữ cho dev | **Giữ** — đổi provider qua config. |

> **Giải thích "complete-mode" (vì sao chọn cách này cho dev):**
> - VNPay xác nhận thanh toán qua **2 kênh**: **IPN** (VNPay → server bạn, cần URL public)
>   và **Return URL** (VNPay → trình duyệt user → trang FE).
> - Production chuẩn: cộng tiền ở **IPN** (đáng tin, không phụ thuộc trình duyệt). Nhưng IPN
>   cần expose server ra Internet (ngrok/cloudflared) → vướng khi demo localhost.
> - Dev: cho phép trang **Return** gọi BE để **verify chữ ký rồi complete** — chạy hoàn toàn
>   local, không cần ngrok. Kém an toàn hơn IPN (phụ thuộc user quay lại) nên **chỉ bật ở dev**.
> - Một biến config `vnpay.complete-mode = RETURN | IPN` quyết định kênh nào được phép cộng
>   tiền. Mặc định dev = `RETURN`; production khuyến nghị `IPN`.

## 3. UX cần làm (theo yêu cầu của bạn)

- **Trang ví (`WalletPage`)**: nút "Tiếp tục" → gọi init → nhận `REDIRECT_URL` → `window.open(url, '_blank')`
  mở tab VNPay. Trạng thái nút chuyển **loading** ("Đang chờ thanh toán…").
- **Trang return mới (`/wallet/vnpay-return`)**: tab VNPay sau khi thanh toán sẽ redirect về đây.
  - Đọc query `vnp_ResponseCode`, `vnp_TxnRef`, và toàn bộ `vnp_*` + `vnp_SecureHash`.
  - Hiện **spinner loading**.
  - (complete-mode=RETURN) gọi `POST /wallet/top-up/vnpay-return` gửi nguyên query để BE verify
    chữ ký + complete. Nhận kết quả → đổi sang **icon xanh + "Nạp tiền thành công"**; thất bại → đỏ.
  - (complete-mode=IPN) chỉ poll `GET /wallet/top-up/status?ref=...` tới khi `COMPLETED` (IPN đã
    cộng) → xanh.
  - Có nút "Quay lại ví".
- **Tab ví gốc**: nhận WebSocket `WALLET_UPDATED` → số dư nhảy + **toast thành công đã có sẵn**.
- Tóm lại UI cần thêm: (1) loading khi chờ ở trang ví, (2) trang return loading→xanh/đỏ. Toast
  nạp thành công đã có, không dựng lại.

## 4. Thiết kế BE (KHÔNG sửa use case/domain cũ — đúng Open/Closed)

### 4A. Mở rộng nhỏ (an toàn, additive)
- `domain/model/Wallet/TxSource`: thêm `VNPAY` vào enum (cần cho
  `TxSource.valueOf(providerName())` trong `InitTopUpUseCase`).
- `PaymentInitResult.displayType`: thêm giá trị quy ước `"REDIRECT_URL"` (chỉ là string, không
  đổi record). FE đọc để biết "mở tab" thay vì "render ảnh QR".

### 4B. `VnPayGateway implements PaymentGateway` (infrastructure/payment)
```
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "vnpay")
class VnPayGateway implements PaymentGateway {
  // @Value: vnpay.tmn-code, vnpay.hash-secret, vnpay.pay-url, vnpay.return-url
  initPayment(referenceCode, amount):
     params = { vnp_Version=2.1.0, vnp_Command=pay, vnp_TmnCode, vnp_Amount=amount*100,
                vnp_CurrCode=VND, vnp_TxnRef=referenceCode, vnp_OrderInfo="Nap vi "+referenceCode,
                vnp_OrderType=other, vnp_Locale=vn, vnp_ReturnUrl, vnp_IpAddr, vnp_CreateDate,
                vnp_ExpireDate }
     sort params → build query → vnp_SecureHash = HMAC_SHA512(hashSecret, query)
     url = payUrl + "?" + query + "&vnp_SecureHash=" + hash
     return PaymentInitResult(referenceCode, amount, url, "REDIRECT_URL", now+ttl)
  providerName(): "VNPAY"
}
```
- `MockPaymentGateway` và `VnPayGateway` loại trừ nhau qua `@ConditionalOnProperty` → tại một
  thời điểm chỉ 1 bean `PaymentGateway`. `MockWebhookController` cũng tự tắt khi provider=vnpay.

### 4C. Hoàn tất giao dịch — 2 kênh, cùng dùng `CompleteTopUpUseCase`

**Kênh RETURN (dev mặc định)** — `WalletController` thêm endpoint (yêu cầu đăng nhập):
```
POST /api/v1/wallet/top-up/vnpay-return   body = toàn bộ vnp_* params (Map)
  (chỉ hoạt động khi vnpay.complete-mode=RETURN)
  1. VnPaySignature.verify(params, hashSecret) sai → 400 (không complete).
  2. ref = vnp_TxnRef; tìm tx; tx.userId phải == requester (JWT) → tránh complete hộ người khác.
  3. (đối soát) vnp_Amount/100 != tx.amount → lỗi amount_mismatch, không complete.
  4. vnp_ResponseCode=="00" → completeTopUpUseCase.execute(ref, vnp_TransactionNo)
       + walletNotificationService.pushWalletUpdated(... source=VNPAY ...)
     else → trả trạng thái thất bại (không cộng).
  5. tx đã COMPLETED → trả luôn success (idempotent), không cộng lại.
  → trả { referenceCode, status, amount } cho FE render xanh/đỏ.
```

**Kênh IPN (production)** — `VnPayWebhookController` (adapter/controller):
```
@RestController @RequestMapping("/api/v1/webhook")
@ConditionalOnProperty(name="payment.provider", havingValue="vnpay")
GET "/vnpay-ipn" (VNPay gọi server→server; chỉ complete khi complete-mode=IPN):
  1. Verify vnp_SecureHash. Sai → {"RspCode":"97","Message":"Invalid Checksum"}.
  2. ref=vnp_TxnRef; không thấy tx → {"RspCode":"01",...}.
  3. amount lệch → {"RspCode":"04",...} + log.
  4. ResponseCode=="00" → completeTopUpUseCase + push WS → {"RspCode":"00","Message":"Confirm Success"}.
  5. tx đã COMPLETED → {"RspCode":"02","Message":"Order already confirmed"}.
  (Nếu complete-mode=RETURN: IPN chỉ ghi nhận/trả "00" mà KHÔNG cộng, để tránh cộng 2 lần.)
```
- Webhook endpoint là **public** (`SecurityConfig` đã `permitAll("/api/v1/webhook/**")`).
- `MockWebhookController` & `MockPaymentGateway` tự tắt khi `payment.provider=vnpay`.
- Idempotent: `CompleteTopUpUseCase` chỉ complete tx PENDING (pessimistic lock) → 2 kênh gọi
  trùng cũng không cộng 2 lần.

### 4D. Endpoint đọc trạng thái (cho FE poll khi cần)
- `GET /api/v1/wallet/top-up/status?ref={referenceCode}` (đăng nhập; chỉ trả tx của chính
  user) → `{ referenceCode, status, amount }`. Không cộng tiền — chỉ đọc.
- Use case `GetTopUpStatusUseCase` (dùng `WalletTransactionRepository.findByReferenceCode`,
  chặn nếu `tx.userId != requester`).

### 4E. Config (`application-dev.yaml`)
- Thay khối `vietqr:` bằng:
```
payment:
  provider: mock          # đổi 'vnpay' để bật VNPay
vnpay:
  tmn-code: CHANGE_ME      # điền khi đăng ký sandbox vnpayment.vn
  hash-secret: CHANGE_ME
  pay-url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
  return-url: http://localhost:5173/wallet/vnpay-return
  ipn-url: http://localhost:8080/api/v1/webhook/vnpay-ipn   # production: URL public/ngrok
  complete-mode: RETURN    # DEV=RETURN (không cần ngrok); PRODUCTION=IPN
  ttl-minutes: 15
```
- KHÔNG commit secret thật — giữ placeholder, set qua biến môi trường khi deploy.

## 5. Files đụng tới

**BE (mới):**
- `infrastructure/payment/VnPayGateway.java`
- `infrastructure/payment/VnPaySignature.java` (util HMAC-SHA512 + build/sort query, verify)
- `adapter/controller/VnPayWebhookController.java` (IPN — cho production)
- `application/usecase/Wallet/CompleteVnPayReturnUseCase.java` (verify chữ ký return + complete,
  dùng cho complete-mode=RETURN) — hoặc gộp logic verify vào controller + tái dùng
  `CompleteTopUpUseCase` đã có.
- `application/usecase/Wallet/GetTopUpStatusUseCase.java` + DTO output.
- DTO: `VnPayReturnRequest` (Map vnp_*), `TopUpStatusResponse`, `VnPayIpnResponse` (RspCode/Message).

**BE (sửa nhỏ, additive — KHÔNG đụng logic cũ):**
- `domain/model/Wallet/TxSource.java` (+ `VNPAY`).
- `adapter/controller/WalletController.java` (+ `POST /top-up/vnpay-return`, + `GET /top-up/status`).
- `application-dev.yaml` (khối `vnpay`).
- (Không sửa `InitTopUpUseCase`, `CompleteTopUpUseCase`, `PaymentGateway`, `MockPaymentGateway`,
  `MockWebhookController`.)

**FE (mới/sửa):**
- `api/wallet.js`: `getTopUpStatusApi(ref)`, `completeVnPayReturnApi(params)`.
- `pages/WalletPage.jsx`: khi `displayType==='REDIRECT_URL'` → `window.open` + trạng thái
  "đang chờ thanh toán" (loading); vẫn giữ nhánh `QR_URL`/`MESSAGE` cũ cho mock.
- `pages/WalletVnpayReturnPage.jsx` (mới) + route `/wallet/vnpay-return` trong `App.jsx`
  (đặt trong `AppLayout` — có sidebar): loading → complete/poll → xanh/đỏ + nút "Quay lại ví".
- (Giữ nguyên) WebSocket handler + toast hiện có.

## 6. Hai chế độ hoàn tất (complete-mode) — chi tiết

| | RETURN (DEV mặc định) | IPN (PRODUCTION) |
|---|---|---|
| Ai gọi BE để complete | Trang FE return (`POST /wallet/top-up/vnpay-return`) | VNPay server (`GET /webhook/vnpay-ipn`) |
| Cần URL public/ngrok | **Không** | **Có** |
| Verify chữ ký | Có (server-side) | Có (server-side) |
| Đáng tin | Phụ thuộc user quay lại trang return | Cao (server→server) |
| Khi nào dùng | Demo/dev localhost | Thật |

- Chuyển chế độ bằng `vnpay.complete-mode`. Code 2 kênh dùng chung `CompleteTopUpUseCase`.
- An toàn 2 chiều: kênh KHÔNG được chọn sẽ **không cộng tiền** (chỉ ghi nhận) để tránh cộng
  trùng. Idempotent của `CompleteTopUpUseCase` là lớp bảo vệ cuối.
- Khi nào có nhu cầu production thật: đổi `complete-mode=IPN` + đặt `ipn-url` = URL public.

## 7. Bảo mật & edge cases

| Tình huống | Xử lý |
|-----------|-------|
| Sai chữ ký | IPN trả RspCode `97`; không cộng tiền |
| Không thấy tx theo `vnp_TxnRef` | RspCode `01` |
| Số tiền lệch | RspCode `04` + log `amount_mismatch`, không complete |
| IPN gọi trùng (tx đã COMPLETED) | RspCode `02`, không cộng lại (idempotent sẵn có) |
| `vnp_ResponseCode != "00"` (user huỷ/thất bại) | Không cộng; tx giữ PENDING (hết hạn theo TTL) hoặc set FAILED; trả RspCode `00` để ngừng retry |
| Return URL bị mở lại/giả mạo | Mọi complete đều verify chữ ký HMAC server-side trước; tx đã COMPLETED → idempotent không cộng lại; chỉ chủ tx (JWT) mới complete được ở kênh RETURN |
| Cộng tiền | Luôn = `tx.getAmount()` nội bộ, không tin amount từ query |

## 8. Trạng thái quyết định (đã chốt)

1. **Hoàn tất giao dịch dev**: dùng **complete-mode=RETURN** (không cần ngrok). Endpoint IPN
   vẫn được tạo để chuyển sang production sau bằng config. ✅
2. **Credential sandbox**: chưa có → để placeholder `CHANGE_ME` trong config; bạn điền sau khi
   đăng ký. Unit test ký/verify dùng secret giả nên vẫn build/test được ngay. ✅
3. **Trang return** `/wallet/vnpay-return` đặt trong `AppLayout` (có sidebar). ✅
4. **Giữ `MockPaymentGateway`** cho dev, đổi provider qua config. ✅

> Khi bạn có TmnCode + HashSecret sandbox: chỉ cần điền vào `application-dev.yaml`, đổi
> `payment.provider=vnpay`, chạy thử. Không phải sửa code.

## 9. Thứ tự triển khai

1. `TxSource.VNPAY` + `VnPaySignature` util + unit test ký/verify (secret giả).
2. `VnPayGateway` (build URL đã ký) + unit test (`displayType=REDIRECT_URL`, query có chữ ký).
3. Kênh RETURN: `POST /wallet/top-up/vnpay-return` (verify + đối soát amount + complete) +
   `GET /wallet/top-up/status` + use case + unit test (chữ ký sai, amount lệch, success, trùng).
4. Kênh IPN: `VnPayWebhookController` (verify + RspCode) + unit test — cho production.
5. Config `application-dev.yaml` (khối `vnpay`, `complete-mode=RETURN`).
6. FE: WalletPage mở tab + trạng thái chờ; `WalletVnpayReturnPage` loading→xanh/đỏ; route trong
   `AppLayout`; `api/wallet.js`.
7. Cập nhật `docs/API/wallet.md` (mục 14.6 → VNPay: init/redirect/return/IPN + complete-mode +
   RspCode) và `docs/project-overview.md`.
8. Test end-to-end với sandbox khi bạn có credential (đổi provider=vnpay).
