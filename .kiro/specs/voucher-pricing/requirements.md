# Requirements Document

## Introduction

Tính năng **Voucher Pricing** bổ sung cơ chế mã giảm giá (voucher) cho luồng mua khóa học của Learning System. Hệ thống cho phép STAFF / SUPER_ADMIN tạo và quản lý voucher; cho phép MEMBER (Học viên) áp dụng voucher khi mua khóa học để được giảm giá theo phần trăm hoặc số tiền cố định.

Tính năng cung cấp một API nghiệp vụ trọng tâm: client gửi `courseId` và `voucherCode` lên server, server tự tính toán giá cuối cùng và trả về cho client. Đây là API "preview / quote" — chỉ tính giá hiển thị, không khóa voucher, không trừ tiền.

Một API thứ hai — checkout — được mở rộng từ luồng mua khóa học hiện có (`POST /api/v1/courses/{id}/purchase`) để chấp nhận `voucherCode` tùy chọn. Trong luồng checkout, server **luôn tính lại giá từ đầu** (server-side price recomputation) thay vì tin giá do client gửi lên, đồng thời thực hiện khóa số lần dùng voucher một cách an toàn (concurrency-safe).

Trọng tâm thiết kế là **chống giả mạo (anti-tampering)** ở 4 mặt:

1. **Tampering courseId**: Client KHÔNG được xem giá của khóa học A rồi áp dụng giá đó cho khóa học B.
2. **Tampering price**: Client KHÔNG được phép gửi `price` / `discount` / `finalPrice` lên server. Mọi giá trị tiền đều được tính lại ở server tại thời điểm thanh toán.
3. **Replay**: Client KHÔNG được phép tái sử dụng response giá cũ để thanh toán với điều kiện đã thay đổi (voucher hết hạn, đã đạt usage limit, đã đổi giá khóa học, v.v.).
4. **Race condition**: Hai request thanh toán đồng thời với cùng một voucher (đặc biệt voucher có usage limit còn 1) phải bị tuần tự hóa, không cho phép vượt giới hạn.

---

## Glossary

- **Voucher**: Mã giảm giá có thời hạn, có giới hạn lượt dùng, có loại giảm giá (phần trăm hoặc cố định). Mỗi voucher có một `code` duy nhất là chuỗi không phân biệt hoa thường.
- **Voucher_Code**: Chuỗi định danh voucher do người tạo nhập (ví dụ `WELCOME50`). Độ dài 4–32 ký tự, chỉ chứa `A-Z`, `0-9`, `_`, `-`. So khớp không phân biệt hoa thường (case-insensitive).
- **Voucher_Type**: Một trong hai giá trị: `PERCENT` (giảm theo phần trăm) hoặc `FIXED` (giảm số tiền cố định).
- **Voucher_Status**: Một trong các giá trị: `ACTIVE` (đang phát hành), `INACTIVE` (đã tắt), `EXPIRED` (hết hạn theo thời gian), `EXHAUSTED` (đã đạt giới hạn lượt dùng tổng). `EXPIRED` và `EXHAUSTED` là trạng thái tính toán động dựa trên dữ liệu thực tế, không bắt buộc lưu thẳng vào DB.
- **Voucher_Scope**: Phạm vi áp dụng của voucher. Một trong các giá trị: `ALL_COURSES` (áp dụng cho mọi khóa học) hoặc `SPECIFIC_COURSES` (chỉ áp dụng cho danh sách khóa học cụ thể).
- **Voucher_Usage_Limit**: Tổng số lượt voucher có thể được sử dụng trên toàn hệ thống. `NULL` hoặc `0` được hiểu là không giới hạn.
- **Voucher_Usage_Per_User**: Số lượt tối đa mà MỘT user được phép dùng cùng một voucher. `NULL` hoặc `0` được hiểu là không giới hạn.
- **Voucher_Min_Order_Amount**: Giá khóa học gốc (`originalPrice`) tối thiểu để voucher có thể áp dụng. Nếu giá gốc nhỏ hơn ngưỡng này, voucher không hợp lệ.
- **Voucher_Max_Discount**: Mức giảm tối đa (đơn vị tiền). Áp dụng cho voucher loại `PERCENT` để giới hạn giá trị giảm. Với voucher `FIXED`, trường này được bỏ qua.
- **Voucher_Valid_From** / **Voucher_Valid_To**: Khoảng thời gian có hiệu lực, kiểu `LocalDateTime` UTC. `valid_from <= now <= valid_to` thì voucher mới được dùng.
- **Voucher_Usage**: Bản ghi xác nhận một lượt voucher đã được tiêu thụ thành công (gắn với `userId`, `courseId`, `enrollmentId`, `voucherId`, `paidPrice`, `discountAmount`, `appliedAt`).
- **Pricing_Engine**: Domain service thuần, không phụ thuộc Spring/JPA, chịu trách nhiệm tính `finalPrice = computePrice(originalPrice, voucher)` và trả về kết quả gồm `originalPrice`, `discountAmount`, `finalPrice`.
- **Voucher_Validator**: Domain service thuần kiểm tra một voucher có hợp lệ trong ngữ cảnh `(user, course, now)` hay không. Trả về kết quả thành công hoặc lỗi nghiệp vụ cụ thể.
- **Quote_Pricing_UseCase**: UseCase tính giá xem trước (preview). KHÔNG ghi DB, KHÔNG khóa voucher, KHÔNG trừ tiền.
- **Apply_Voucher_Checkout_UseCase**: UseCase mua khóa học có dùng voucher. Tính lại giá ở server, khóa voucher (pessimistic lock), trừ tiền ví, ghi `Voucher_Usage`, ghi `Enrollment`, ghi audit log.
- **Server_Computed_Price**: Giá cuối cùng được Pricing_Engine tính lại tại thời điểm checkout, dựa trên dữ liệu hiện hành trong DB (giá khóa học, trạng thái voucher, lượt dùng còn lại).
- **Voucher_Repository**: Repository interface ở `application/repository/Voucher/` để lưu/đọc Voucher.
- **Voucher_Usage_Repository**: Repository interface ở `application/repository/Voucher/` để lưu/đọc bản ghi Voucher_Usage.

---

## Requirements

### Requirement 1: Mô hình dữ liệu Voucher

**User Story:** Là STAFF / SUPER_ADMIN, tôi muốn có một mô hình dữ liệu voucher đầy đủ để có thể cấu hình các chương trình khuyến mãi linh hoạt.

#### Acceptance Criteria

1. THE Voucher_Repository SHALL lưu trữ voucher với các trường bắt buộc: `id` (Long, PK), `code` (String, UNIQUE, không phân biệt hoa thường), `type` (`PERCENT` hoặc `FIXED`), `value` (BigDecimal, > 0), `status` (`ACTIVE` hoặc `INACTIVE`), `validFrom` (LocalDateTime), `validTo` (LocalDateTime), `scope` (`ALL_COURSES` hoặc `SPECIFIC_COURSES`), `createdAt`, `updatedAt`.
2. THE Voucher_Repository SHALL lưu trữ các trường tùy chọn của voucher: `minOrderAmount` (BigDecimal, ≥ 0, mặc định 0), `maxDiscount` (BigDecimal, ≥ 0, chỉ áp dụng khi `type = PERCENT`), `usageLimit` (Long, ≥ 0, `0` nghĩa là không giới hạn), `usagePerUser` (Integer, ≥ 0, `0` nghĩa là không giới hạn), `applicableCourseIds` (Set<Long>, chỉ tồn tại khi `scope = SPECIFIC_COURSES`).
3. WHEN tạo voucher loại `PERCENT`, THE Voucher_Validator SHALL yêu cầu `0 < value ≤ 100`.
4. WHEN tạo voucher loại `FIXED`, THE Voucher_Validator SHALL yêu cầu `value > 0` và `value` ≤ giá trị tối đa cho phép của hệ thống (cấu hình `voucher.fixed.max-value`, mặc định 100,000,000).
5. WHEN tạo voucher với `scope = SPECIFIC_COURSES`, THE Voucher_Validator SHALL yêu cầu `applicableCourseIds` không rỗng và mọi `courseId` trong danh sách phải tồn tại trong DB.
6. WHEN tạo voucher với `scope = ALL_COURSES`, THE Voucher_Validator SHALL yêu cầu `applicableCourseIds` rỗng hoặc null.
7. THE Voucher_Repository SHALL áp dụng ràng buộc `validFrom ≤ validTo` ở tầng domain trước khi save.
8. THE Voucher_Repository SHALL lưu `code` ở dạng chữ HOA chuẩn hóa (uppercase) để bảo đảm tính duy nhất không phân biệt hoa thường.

---

### Requirement 2: Quản lý Voucher (CRUD cho STAFF / SUPER_ADMIN)

**User Story:** Là STAFF hoặc SUPER_ADMIN, tôi muốn tạo, sửa, xóa, xem danh sách voucher để quản lý chương trình khuyến mãi.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/admin/vouchers` được gửi tới hệ thống bởi user có role STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL gọi Create_Voucher_UseCase và trả về voucher vừa tạo cùng HTTP 201.
2. IF một request `POST /api/v1/admin/vouchers` được gửi bởi user có role MEMBER, INSTRUCTOR, hoặc ADMIN_USER, THEN THE Admin_Voucher_Controller SHALL trả về HTTP 403 với mã lỗi `VOUCHER_ACCESS_DENIED`.
3. WHEN một request `PUT /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL gọi Update_Voucher_UseCase và trả về voucher đã cập nhật cùng HTTP 200.
4. IF Update_Voucher_UseCase được gọi với `id` không tồn tại, THEN THE Update_Voucher_UseCase SHALL ném `VoucherNotFoundException` để Global_Exception_Handler trả về HTTP 404 với mã `VOUCHER_NOT_FOUND`.
5. WHEN một request `DELETE /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL gọi Delete_Voucher_UseCase, đặt `status = INACTIVE` (soft-delete) và trả về HTTP 200.
6. IF Delete_Voucher_UseCase được gọi với một voucher đã có ít nhất một bản ghi Voucher_Usage, THEN THE Delete_Voucher_UseCase SHALL chỉ thực hiện soft-delete (set `status = INACTIVE`) và SHALL KHÔNG xóa cứng để bảo toàn lịch sử.
7. WHEN một request `GET /api/v1/admin/vouchers` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL trả về danh sách voucher có phân trang (`page`, `size`, `totalElements`, `totalPages`).
8. WHEN một request `GET /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL trả về chi tiết voucher kèm trường tính toán `usedCount` (đếm từ Voucher_Usage_Repository) và `effectiveStatus` (một trong `ACTIVE` / `INACTIVE` / `EXPIRED` / `EXHAUSTED`).
9. WHILE voucher có ít nhất một bản ghi Voucher_Usage, THE Update_Voucher_UseCase SHALL từ chối thay đổi các trường `code`, `type`, và `value` (chỉ cho phép sửa `validTo`, `status`, `usageLimit` để mở rộng, `applicableCourseIds`, `minOrderAmount`, `maxDiscount`).
10. THE Update_Voucher_UseCase SHALL từ chối giảm `usageLimit` xuống dưới `usedCount` hiện tại và ném `VoucherUsageLimitTooLowException`.

---

### Requirement 3: API tính giá xem trước (Quote / Preview)

**User Story:** Là MEMBER, tôi muốn nhập mã voucher và thấy giá cuối cùng của khóa học trước khi quyết định thanh toán, để biết mình tiết kiệm được bao nhiêu.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/courses/{courseId}/quote` với body `{ "voucherCode": "<code>" }` được gửi bởi user đã đăng nhập, THE Quote_Pricing_Controller SHALL gọi Quote_Pricing_UseCase và trả về kết quả định giá HTTP 200.
2. THE Quote_Pricing_Controller SHALL CHỈ chấp nhận `courseId` từ path và `voucherCode` từ body. Mọi trường `price`, `originalPrice`, `discount`, `finalPrice` xuất hiện trong body request SHALL bị bỏ qua hoàn toàn (không deserialize, không sử dụng).
3. THE Quote_Pricing_UseCase SHALL đọc `originalPrice` của khóa học từ Course_Repository tại thời điểm gọi và SHALL KHÔNG bao giờ tin giá tiền do client gửi lên.
4. WHEN `voucherCode` trong request là rỗng hoặc null, THE Quote_Pricing_UseCase SHALL trả về kết quả `{ originalPrice, discountAmount: 0, finalPrice: originalPrice, voucherApplied: false }`.
5. WHEN voucher hợp lệ và áp dụng được, THE Quote_Pricing_UseCase SHALL trả về `{ originalPrice, discountAmount, finalPrice, voucherApplied: true, voucherCode, voucherType, quotedAt: <timestamp> }`.
6. IF `courseId` không tồn tại, THEN THE Quote_Pricing_UseCase SHALL ném `CourseNotFoundException` để trả về HTTP 404 với mã `COURSE_NOT_FOUND`.
7. IF user là MEMBER có cờ `isInternal = true`, THEN THE Quote_Pricing_UseCase SHALL trả về `{ originalPrice: <giá_gốc>, discountAmount: <giá_gốc>, finalPrice: 0, voucherApplied: false, internalDiscount: true }` và SHALL bỏ qua mọi voucher (vì học viên nội bộ luôn có giá 0).
8. THE Quote_Pricing_UseCase SHALL KHÔNG ghi bất kỳ thay đổi nào vào Voucher_Repository, Voucher_Usage_Repository, hoặc Wallet, bảo đảm tính idempotent: gọi nhiều lần liên tiếp với cùng đầu vào trả về cùng đầu ra (nếu dữ liệu DB không đổi).
9. THE Quote_Pricing_Controller SHALL trả về `quotedAt` ở dạng ISO-8601 UTC để client biết thời điểm tính giá. Trường này CHỈ mang tính thông tin và SHALL KHÔNG được dùng làm chứng cứ ràng buộc giá ở luồng checkout.
10. WHEN `originalPrice` của khóa học bằng 0, THE Quote_Pricing_UseCase SHALL trả về `{ originalPrice: 0, discountAmount: 0, finalPrice: 0, voucherApplied: false }` ngay cả khi voucher được cung cấp.

---

### Requirement 4: Validation voucher tại thời điểm tính giá

**User Story:** Là người dùng, tôi muốn nhận thông báo lỗi rõ ràng khi voucher không áp dụng được, để biết lý do và xử lý phù hợp.

#### Acceptance Criteria

1. IF voucher với `code` đã chuẩn hóa (uppercase) không tồn tại trong DB, THEN THE Voucher_Validator SHALL ném `VoucherNotFoundException` để trả về HTTP 404 với mã `VOUCHER_NOT_FOUND`.
2. IF voucher có `status = INACTIVE`, THEN THE Voucher_Validator SHALL ném `VoucherInactiveException` để trả về HTTP 400 với mã `VOUCHER_INACTIVE`.
3. IF thời điểm hiện tại nhỏ hơn `validFrom`, THEN THE Voucher_Validator SHALL ném `VoucherNotYetActiveException` để trả về HTTP 400 với mã `VOUCHER_NOT_YET_ACTIVE`.
4. IF thời điểm hiện tại lớn hơn `validTo`, THEN THE Voucher_Validator SHALL ném `VoucherExpiredException` để trả về HTTP 400 với mã `VOUCHER_EXPIRED`.
5. IF voucher có `usageLimit > 0` VÀ tổng số bản ghi Voucher_Usage cho voucher này đã đạt `usageLimit`, THEN THE Voucher_Validator SHALL ném `VoucherUsageLimitReachedException` để trả về HTTP 400 với mã `VOUCHER_USAGE_LIMIT_REACHED`.
6. IF voucher có `usagePerUser > 0` VÀ user hiện tại đã có số bản ghi Voucher_Usage cho voucher này đạt `usagePerUser`, THEN THE Voucher_Validator SHALL ném `VoucherUsagePerUserExceededException` để trả về HTTP 400 với mã `VOUCHER_USAGE_PER_USER_EXCEEDED`.
7. IF `originalPrice` của khóa học nhỏ hơn `minOrderAmount`, THEN THE Voucher_Validator SHALL ném `VoucherMinOrderNotMetException` để trả về HTTP 400 với mã `VOUCHER_MIN_ORDER_NOT_MET`.
8. IF voucher có `scope = SPECIFIC_COURSES` VÀ `courseId` không nằm trong `applicableCourseIds`, THEN THE Voucher_Validator SHALL ném `VoucherNotApplicableException` để trả về HTTP 400 với mã `VOUCHER_NOT_APPLICABLE`.
9. THE Voucher_Validator SHALL kiểm tra các điều kiện theo thứ tự cố định: tồn tại → status → thời gian → scope → minOrder → usageLimit toàn cục → usagePerUser, để bảo đảm thông báo lỗi nhất quán giữa các lần gọi.
10. THE Voucher_Validator SHALL chuẩn hóa `voucherCode` bằng cách `trim()` và chuyển sang uppercase trước khi tra cứu DB.


---

### Requirement 5: Tính giá ở Pricing Engine (Domain Logic)

**User Story:** Là kiến trúc sư hệ thống, tôi muốn logic tính giá nằm ở Domain Service thuần, để có thể test bằng property-based testing và tái sử dụng giữa luồng quote và checkout.

#### Acceptance Criteria

1. THE Pricing_Engine SHALL được đặt tại `domain/service/PricingEngine.java`, là class thuần Java không phụ thuộc Spring, JPA, hay bất kỳ framework nào.
2. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher = null`, THE Pricing_Engine SHALL trả về `PriceQuote{ originalPrice, discountAmount = 0, finalPrice = originalPrice }`.
3. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher.type = PERCENT`, THE Pricing_Engine SHALL tính `rawDiscount = originalPrice × voucher.value / 100` (làm tròn HALF_UP đến 2 chữ số thập phân) và `discountAmount = min(rawDiscount, voucher.maxDiscount)` (nếu `maxDiscount > 0`, ngược lại bỏ qua giới hạn).
4. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher.type = FIXED`, THE Pricing_Engine SHALL tính `discountAmount = min(voucher.value, originalPrice)`.
5. THE Pricing_Engine SHALL bảo đảm `finalPrice = originalPrice - discountAmount` LUÔN ≥ 0 (không bao giờ trả về giá âm).
6. THE Pricing_Engine SHALL bảo đảm `discountAmount` LUÔN ≤ `originalPrice` (không bao giờ giảm quá giá gốc).
7. THE Pricing_Engine SHALL bảo đảm `finalPrice` LUÔN ≤ `originalPrice` (không bao giờ tăng giá).
8. THE Pricing_Engine SHALL sử dụng kiểu `BigDecimal` cho mọi phép tính tiền, không bao giờ dùng `double` hoặc `float`.
9. IF `originalPrice` âm, THEN THE Pricing_Engine SHALL ném `IllegalArgumentException` (đây là lỗi lập trình, không phải lỗi nghiệp vụ).
10. THE Pricing_Engine SHALL là pure function: cùng đầu vào (`originalPrice`, `voucher` snapshot) thì luôn trả về cùng đầu ra, không phụ thuộc thời gian, random, hay state ngoài.

---

### Requirement 6: API checkout có dùng voucher (chống tampering & race condition)

**User Story:** Là MEMBER, tôi muốn mua khóa học với mã voucher để được giảm giá, và tôi tin tưởng rằng giá tôi trả là đúng giá khuyến mãi tại thời điểm thanh toán.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/courses/{courseId}/purchase` với body `{ "voucherCode": "<code>" }` được gửi bởi user đã đăng nhập, THE Purchase_Controller SHALL gọi Apply_Voucher_Checkout_UseCase và trả về kết quả mua hàng HTTP 200.
2. THE Purchase_Controller SHALL CHỈ chấp nhận `courseId` từ path và `voucherCode` (tùy chọn) từ body. Mọi trường `price`, `originalPrice`, `discountAmount`, `finalPrice`, `paidPrice` xuất hiện trong body request SHALL bị bỏ qua hoàn toàn.
3. THE Apply_Voucher_Checkout_UseCase SHALL đọc `originalPrice` của khóa học từ Course_Repository tại thời điểm checkout và SHALL gọi Pricing_Engine để tính lại `finalPrice` từ đầu, KHÔNG sử dụng bất kỳ giá trị nào do client gửi lên hoặc cache từ luồng quote trước đó.
4. THE Apply_Voucher_Checkout_UseCase SHALL chạy toàn bộ logic trong một transaction `@Transactional` duy nhất bao gồm: load voucher với pessimistic write lock, validate voucher, tính giá, trừ tiền ví (sử dụng pessimistic lock đã có trên User), tạo Enrollment, tạo Voucher_Usage, ghi audit log.
5. WHEN load voucher trong checkout, THE Apply_Voucher_Checkout_UseCase SHALL sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên hàng voucher tương ứng để bảo đảm hai request đồng thời với cùng voucher sẽ được tuần tự hóa.
6. WHEN đếm `usedCount` của voucher trong checkout (để kiểm tra `usageLimit` và `usagePerUser`), THE Apply_Voucher_Checkout_UseCase SHALL đếm BÊN TRONG transaction sau khi đã giữ pessimistic lock, để giá trị đếm phản ánh đúng tình trạng tại thời điểm khóa.
7. IF user đã có Enrollment cho khóa học này từ trước, THEN THE Apply_Voucher_Checkout_UseCase SHALL ném `AlreadyEnrolledException` (HTTP 400, mã `ALREADY_ENROLLED`) và SHALL KHÔNG tạo Voucher_Usage.
8. IF số dư ví của user nhỏ hơn `finalPrice` đã tính ở server, THEN THE Apply_Voucher_Checkout_UseCase SHALL ném `InsufficientBalanceException` (HTTP 400, mã `INSUFFICIENT_BALANCE`) và SHALL KHÔNG tạo Voucher_Usage, KHÔNG trừ tiền.
9. WHEN checkout thành công, THE Apply_Voucher_Checkout_UseCase SHALL tạo đúng MỘT bản ghi Voucher_Usage chứa `(voucherId, userId, courseId, enrollmentId, originalPrice, discountAmount, finalPrice, appliedAt)` và `appliedAt` được set bằng thời gian server.
10. WHEN checkout thành công, THE Purchase_Controller SHALL trả về body chứa `{ enrollmentId, originalPrice, discountAmount, finalPrice, paidPrice, voucherApplied, voucherCode (nếu có) }` với `paidPrice = finalPrice`.
11. THE Apply_Voucher_Checkout_UseCase SHALL ghi audit log dạng JSONL vào `logs/purchase_ledger.jsonl` (mở rộng từ `PurchaseLedgerService` đã có) bao gồm cả `voucherCode`, `originalPrice`, `discountAmount`, `finalPrice` để bảo đảm tính bất biến của lịch sử.
12. IF user là MEMBER nội bộ (`isInternal = true`), THEN THE Apply_Voucher_Checkout_UseCase SHALL bỏ qua `voucherCode` (nếu có) và đặt `paidPrice = 0`, KHÔNG tạo Voucher_Usage. Học viên nội bộ luôn được miễn phí, voucher không có ý nghĩa với họ.

---

### Requirement 7: Chống tampering courseId (Mối lo trọng tâm)

**User Story:** Là chủ hệ thống, tôi muốn bảo đảm client không thể đổi `courseId` để lấy giá rẻ hơn áp dụng cho khóa học đắt hơn.

#### Acceptance Criteria

1. THE Quote_Pricing_UseCase SHALL tính giá DUY NHẤT dựa trên `courseId` mà client gửi và giá hiện hành của chính `courseId` đó trong DB. Không tồn tại bất kỳ tham số nào cho phép client gợi ý giá khác.
2. THE Apply_Voucher_Checkout_UseCase SHALL tính giá DUY NHẤT dựa trên `courseId` ở path và giá hiện hành của chính `courseId` đó trong DB tại thời điểm checkout. Không có bước nào "tin" kết quả của Quote_Pricing_UseCase.
3. THE Apply_Voucher_Checkout_UseCase SHALL KHÔNG bao giờ chấp nhận hay sử dụng các tham số như `quotedPrice`, `quotedDiscount`, `quoteId`, `quoteToken` từ client. Mỗi lần checkout là một lần tính lại giá độc lập.
4. WHEN log audit trail, THE Apply_Voucher_Checkout_UseCase SHALL ghi rõ `courseId`, `originalPriceAtCheckout`, `voucherCode`, `discountAmount`, `finalPrice` để có thể dò ngược nếu phát hiện bất thường.
5. THE Quote_Pricing_Controller và Purchase_Controller SHALL trả về cùng kết quả tính giá khi `courseId`, `voucherCode`, `userId`, và state DB không đổi giữa hai lần gọi (xem như property metamorphic: `quote(c, v) ≈ checkout(c, v).priceFields` về phần định giá).
6. IF client cố ý gửi body chứa các trường `price`, `originalPrice`, `discount`, `finalPrice`, `paidPrice` cho cả endpoint `/quote` và `/purchase`, THEN THE Controller SHALL bỏ qua các trường này (DTO của adapter chỉ khai báo `voucherCode`), không trả lỗi để giữ tương thích nhưng cũng không sử dụng giá trị từ client.
7. THE Course_Repository SHALL được gọi với `findById(courseId)` cứng. KHÔNG có nhánh code nào cho phép truy vấn course bằng tên, slug, alias, hay thông tin khác trong các luồng pricing.

---

### Requirement 8: Race condition và idempotency

**User Story:** Là chủ hệ thống, tôi muốn bảo đảm khi voucher chỉ còn 1 lượt dùng, chỉ MỘT trong số nhiều request đồng thời được thành công, không có chuyện vượt quá giới hạn.

#### Acceptance Criteria

1. WHEN nhiều request checkout đồng thời tham chiếu cùng một `voucherId`, THE Apply_Voucher_Checkout_UseCase SHALL tuần tự hóa các request thông qua pessimistic write lock trên hàng voucher (như đã định nghĩa ở Requirement 6.5).
2. WHILE giữ pessimistic lock trên voucher, THE Apply_Voucher_Checkout_UseCase SHALL kiểm tra lại tổng `usedCount` để bảo đảm chưa vượt `usageLimit`, kể cả khi luồng quote trước đó đã pass validation.
3. IF tại thời điểm khóa, `usedCount >= usageLimit`, THEN THE Apply_Voucher_Checkout_UseCase SHALL ném `VoucherUsageLimitReachedException` (HTTP 409, mã `VOUCHER_USAGE_LIMIT_REACHED`) và rollback transaction.
4. THE Voucher_Usage_Repository SHALL có ràng buộc UNIQUE trên `(voucherId, enrollmentId)` để bảo đảm một enrollment chỉ tiêu thụ tối đa một lượt voucher, kể cả khi có race ở mức ứng dụng.
5. WHEN client gửi cùng một request `POST /api/v1/courses/{courseId}/purchase` hai lần liên tiếp (ví dụ do retry của mạng), THE Apply_Voucher_Checkout_UseCase SHALL phát hiện qua kiểm tra `AlreadyEnrolledException` (Requirement 6.7) và lần thứ hai SHALL trả về lỗi 400 với mã `ALREADY_ENROLLED` thay vì tạo Voucher_Usage thứ hai.
6. THE Quote_Pricing_UseCase SHALL idempotent: hai lần gọi liên tiếp với cùng `(userId, courseId, voucherCode)` và state DB không đổi SHALL trả về cùng `originalPrice`, `discountAmount`, `finalPrice` (`quotedAt` được phép khác nhau).

---

### Requirement 9: Phân tách Preview flow và Checkout flow

**User Story:** Là chủ hệ thống, tôi muốn phân biệt rõ giữa "xem giá" và "thực sự dùng voucher", để tránh việc duyệt voucher tiêu hao lượt dùng vô ích.

#### Acceptance Criteria

1. THE Quote_Pricing_UseCase SHALL KHÔNG ghi bản ghi Voucher_Usage và SHALL KHÔNG cập nhật bất kỳ counter nào của voucher.
2. THE Quote_Pricing_UseCase SHALL có thể chạy với READ-ONLY transaction (`@Transactional(readOnly = true)`).
3. THE Apply_Voucher_Checkout_UseCase là điểm DUY NHẤT trong hệ thống tạo bản ghi Voucher_Usage. Không UseCase nào khác được phép insert vào bảng `voucher_usages`.
4. THE Quote_Pricing_Controller SHALL được mount ở endpoint riêng `POST /api/v1/courses/{courseId}/quote`, tách biệt với `POST /api/v1/courses/{courseId}/purchase`, để client có thể gọi /quote nhiều lần mà không tốn lượt dùng.
5. THE Quote_Pricing_UseCase và Apply_Voucher_Checkout_UseCase SHALL chia sẻ cùng một Pricing_Engine và cùng một Voucher_Validator, để bảo đảm kết quả định giá và rule kiểm tra hoàn toàn nhất quán giữa hai flow.

---

### Requirement 10: Phân quyền

**User Story:** Là chủ hệ thống, tôi muốn phân quyền rõ ràng cho voucher để tránh lạm dụng.

#### Acceptance Criteria

1. THE Permission_Seed SHALL thêm hai permission mới: `MANAGE_VOUCHER` (tạo / sửa / xóa / xem voucher trong panel admin) và `USE_VOUCHER` (áp dụng voucher khi mua khóa học).
2. THE Permission_Seed SHALL gán `MANAGE_VOUCHER` cho các role STAFF và SUPER_ADMIN.
3. THE Permission_Seed SHALL gán `USE_VOUCHER` cho role MEMBER (cả nội bộ và bên ngoài) và SUPER_ADMIN. Các role INSTRUCTOR và ADMIN_USER KHÔNG có permission này (vì họ không phải đối tượng mua khóa học).
4. WHEN một user không có `MANAGE_VOUCHER` gọi tới bất kỳ endpoint `/api/v1/admin/vouchers/**`, THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
5. WHEN một user không có `USE_VOUCHER` gọi tới `/api/v1/courses/{id}/quote` hoặc gửi `voucherCode` trong `/api/v1/courses/{id}/purchase`, THE Security_Layer hoặc Quote_Pricing_UseCase SHALL trả về HTTP 403 với mã `VOUCHER_USE_DENIED`.

---

### Requirement 11: Audit và quan sát hệ thống

**User Story:** Là quản trị viên, tôi muốn có audit trail đầy đủ về voucher để điều tra khi nghi ngờ lạm dụng.

#### Acceptance Criteria

1. WHEN một bản ghi Voucher_Usage được tạo, THE Apply_Voucher_Checkout_UseCase SHALL ghi một dòng JSONL vào `logs/purchase_ledger.jsonl` với các trường `event = "VOUCHER_APPLIED"`, `userId`, `courseId`, `voucherId`, `voucherCode`, `originalPrice`, `discountAmount`, `finalPrice`, `enrollmentId`, `appliedAt`.
2. WHEN voucher validation thất bại trong luồng checkout, THE Apply_Voucher_Checkout_UseCase SHALL ghi một dòng JSONL với `event = "VOUCHER_REJECTED"`, `userId`, `courseId`, `voucherCode` (không phân biệt hoa thường, đã chuẩn hóa), `reason` (mã lỗi như `VOUCHER_EXPIRED`, `VOUCHER_USAGE_LIMIT_REACHED`...).
3. THE Voucher_Usage_Repository SHALL hỗ trợ truy vấn `findByUserId(userId, page, size)` và `findByVoucherId(voucherId, page, size)` để phục vụ admin xem lịch sử.
4. THE Admin_Voucher_Controller SHALL cung cấp endpoint `GET /api/v1/admin/vouchers/{id}/usages` (chỉ STAFF/SUPER_ADMIN) trả về danh sách Voucher_Usage có phân trang.

---

### Requirement 12: Validation đầu vào ở tầng adapter

**User Story:** Là người dùng API, tôi muốn nhận lỗi rõ ràng khi gửi sai định dạng để biết cách sửa.

#### Acceptance Criteria

1. THE Quote_Pricing_Request DTO SHALL khai báo `voucherCode` là `String` tùy chọn (có thể null), độ dài 0–32 ký tự.
2. WHEN `voucherCode` chứa ký tự ngoài tập `A-Z`, `a-z`, `0-9`, `_`, `-`, THE Bean_Validation SHALL trả về HTTP 400 với mã `VALIDATION_ERROR` và message chỉ rõ ký tự không hợp lệ.
3. THE Create_Voucher_Request DTO SHALL khai báo `code` (bắt buộc, 4–32 ký tự, regex `^[A-Za-z0-9_-]+$`), `type` (bắt buộc, `PERCENT` hoặc `FIXED`), `value` (bắt buộc, > 0), `validFrom` (bắt buộc, ISO-8601), `validTo` (bắt buộc, ISO-8601), `scope` (bắt buộc, `ALL_COURSES` hoặc `SPECIFIC_COURSES`).
4. WHEN `validFrom > validTo` ở tầng request, THE Bean_Validation hoặc Create_Voucher_UseCase SHALL trả về HTTP 400 với mã `VOUCHER_DATE_RANGE_INVALID`.
5. WHEN `type = PERCENT` và `value > 100`, THE Create_Voucher_UseCase SHALL trả về HTTP 400 với mã `VOUCHER_PERCENT_OUT_OF_RANGE`.
6. WHEN `scope = SPECIFIC_COURSES` nhưng `applicableCourseIds` rỗng hoặc null, THE Create_Voucher_UseCase SHALL trả về HTTP 400 với mã `VOUCHER_SCOPE_MISMATCH`.

---

## Correctness Properties (Cho Property-Based Testing)

Phần này liệt kê các property bất biến mà tính năng PHẢI thỏa mãn, làm cơ sở cho property-based test trong giai đoạn design và implementation.

### Pricing Engine — Pure Domain (PBT phù hợp, chi phí thấp, đầu vào đa dạng)

1. **Non-negative final price** (Invariant): Với mọi `originalPrice ≥ 0` và mọi voucher hợp lệ, `compute(originalPrice, voucher).finalPrice ≥ 0`.
2. **Final price upper bound** (Invariant): Với mọi đầu vào, `compute(originalPrice, voucher).finalPrice ≤ originalPrice`.
3. **Discount upper bound** (Invariant): Với mọi đầu vào, `compute(originalPrice, voucher).discountAmount ≤ originalPrice`.
4. **MaxDiscount cap** (Metamorphic): Với voucher `PERCENT` có `maxDiscount > 0`, `discountAmount ≤ maxDiscount`.
5. **Idempotence của Pricing_Engine** (Idempotence): Gọi `compute(originalPrice, voucher)` nhiều lần luôn trả về cùng kết quả (pure function).
6. **Null voucher = no discount** (Invariant): `compute(originalPrice, null).finalPrice == originalPrice` và `discountAmount == 0`.
7. **Zero original price** (Edge case → example test): `compute(0, anyVoucher).finalPrice == 0`.
8. **Larger discount → smaller or equal final price** (Metamorphic): Với hai voucher `PERCENT` cùng cấu hình, voucher có `value` lớn hơn cho `finalPrice` không lớn hơn voucher có `value` nhỏ hơn.

### Voucher Validator (PBT phù hợp với input voucher state)

9. **Expired voucher always rejected** (Invariant): Với mọi voucher có `validTo < now`, validator luôn ném `VoucherExpiredException`, không phụ thuộc các điều kiện khác.
10. **Inactive voucher always rejected** (Invariant): Với mọi voucher `status = INACTIVE`, validator luôn ném `VoucherInactiveException`.
11. **Order of error checks deterministic** (Invariant): Với cùng voucher state và cùng `(user, course, now)`, validator luôn ném cùng exception (theo thứ tự cố định ở Requirement 4.9).

### Quote vs Checkout Consistency (Metamorphic, quan trọng cho anti-tampering)

12. **Quote-Checkout price parity** (Metamorphic): Khi state DB không đổi giữa hai lệnh gọi, `quote(courseId, voucherCode).finalPrice == checkout(courseId, voucherCode).finalPrice`. Đây là property cốt lõi để chống tampering.
13. **Quote idempotence** (Idempotence): Gọi `quote` nhiều lần liên tiếp với cùng đầu vào → cùng kết quả về phần giá.
14. **Quote không tiêu thụ voucher** (Invariant): Số bản ghi Voucher_Usage trước và sau khi gọi `quote` n lần luôn bằng nhau.

### Server-side Recomputation (Anti-tampering, dùng integration test 1–3 examples thay vì PBT)

15. **CourseId tampering ineffective** (Example test): Gọi `quote(course_A, voucher)` → ghi nhận giá `P_A`. Gọi `checkout(course_B, voucher)` (course B đắt hơn). Server tính lại giá theo course B, KHÔNG dùng `P_A`.
16. **Client-supplied price ignored** (Example test): Gửi body `{"voucherCode": "X", "price": 0, "finalPrice": 0}` → server trả về giá đúng theo DB, các trường client gửi bị bỏ qua.

### Race Condition (Integration test với mock concurrency, không phải PBT thuần)

17. **Voucher usage limit không bị vượt** (Integration test với 2–3 thread): Với voucher có `usageLimit = 1`, hai request checkout đồng thời chỉ đúng 1 thành công, request kia nhận `VOUCHER_USAGE_LIMIT_REACHED`.
18. **Tổng số Voucher_Usage ≤ usageLimit** (Invariant kiểm tra sau test concurrency): Sau N request đồng thời, `count(Voucher_Usage WHERE voucherId=X) ≤ X.usageLimit`.

### Round-trip & Audit (Cần đối với log JSONL, NOT cần PBT cho parser/printer phức tạp)

19. **Audit log round-trip** (Round-trip property cho JSON serialization): Mọi bản ghi `VoucherAuditEntry` được serialize thành JSON rồi parse lại phải bằng entry gốc. Đây là property tiêu chuẩn cho parser/serializer.

### Properties KHÔNG nên dùng PBT (theo decision guide)

- "Endpoint trả về HTTP 401 khi không có JWT" — behavior không vary với input → integration test 1 example.
- "Audit log được ghi vào file" — testing IO infrastructure → integration test.
- "Pessimistic lock hoạt động" — testing JPA/MySQL behavior → integration test với 2 thread.
- "Permission seeding chạy đúng" — config / setup → smoke test.

---

## Notes về thiết kế (chỉ là gợi ý cho phase Design, KHÔNG ràng buộc requirements)

Các điểm sau đây sẽ được làm rõ trong phase Design — ghi lại ở đây để phase tiếp theo không quên:

- Cấu trúc bảng `vouchers`, `voucher_courses` (mapping nếu `scope = SPECIFIC_COURSES`), `voucher_usages`.
- Index DB: UNIQUE `code` chuẩn hóa uppercase, INDEX `(voucherId)` trên `voucher_usages` để đếm `usedCount` hiệu quả.
- Tích hợp với `PurchaseLedgerService` đã có.
- Cấu trúc `domain/service/PricingEngine`, `domain/service/VoucherValidator`, `domain/exception/Voucher*Exception`.
- UseCase: `CreateVoucherUseCase`, `UpdateVoucherUseCase`, `DeleteVoucherUseCase`, `GetVouchersUseCase`, `GetVoucherDetailUseCase`, `QuotePricingUseCase`, `ApplyVoucherCheckoutUseCase` (có thể refactor `PurchaseCourseUseCase` hiện có).
- Tách biệt: `PurchaseCourseUseCase` cũ (không có voucher) và `ApplyVoucherCheckoutUseCase` mới (có voucher) — hoặc gộp thành một UseCase duy nhất với tham số `voucherCode` tùy chọn (cần quyết định ở Design).
