# Requirements Document

## Introduction

Tính năng **Voucher Management** bổ sung cơ chế mã giảm giá (voucher) vào luồng mua khóa học hiện có của Learning System. Tính năng gồm hai phần:

1. **Quản trị voucher (CRUD)**: STAFF và SUPER_ADMIN tạo / sửa / xóa / xem voucher với các thuộc tính: loại giảm (phần trăm hoặc cố định), giá trị giảm tối đa, thời hạn, số lượng tổng, số lượt mỗi user, phạm vi áp dụng (theo course cụ thể hoặc toàn hệ thống), và giá đơn hàng tối thiểu.
2. **API định giá có voucher**: client gửi `courseId` cùng `voucherCode` lên server; server tự tra giá khóa học từ DB, kiểm tra voucher, tính giá cuối cùng và trả về cho client. Có hai endpoint:
   - `POST /api/v1/courses/{courseId}/price-preview` — chỉ tính và trả giá hiển thị, không tiêu thụ voucher.
   - `POST /api/v1/courses/{courseId}/purchase` (mở rộng từ luồng có sẵn) — tính lại giá lần nữa tại thời điểm thanh toán, tiêu thụ một lượt voucher, trừ tiền ví, tạo enrollment.

Yêu cầu trọng tâm của tính năng này là **chống gian lận giá (price tampering)**. Client KHÔNG bao giờ được phép gửi giá tiền lên server. Server LUÔN tự tra giá khóa học từ DB tại thời điểm xử lý request, và LUÔN tính lại giá tại thời điểm purchase (không tin kết quả của price-preview gọi trước đó).

Một quy tắc nghiệp vụ bổ sung: **MEMBER nội bộ (`isInternal = true`) đã được mua khóa học giá 0đ**, do đó voucher không có ý nghĩa với họ. Bốn role còn lại (INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN) cũng không phải đối tượng mua khóa học, nên không sử dụng voucher. Chỉ MEMBER bên ngoài (`isInternal = false`) mới được áp dụng voucher.

---

## Glossary

- **Voucher**: Bản ghi quy định một chương trình giảm giá, có mã định danh duy nhất, có loại giảm, giá trị giảm, thời hạn và quota.
- **Voucher_Code**: Chuỗi định danh voucher do người tạo nhập (ví dụ `WELCOME50`). Độ dài 4–32 ký tự. Cho phép `A-Z`, `0-9`, `_`, `-`. So khớp không phân biệt hoa thường: server lưu dạng uppercase chuẩn hóa và mọi tra cứu đều chuẩn hóa trước.
- **Voucher_Type**: Một trong hai giá trị `PERCENT` (giảm theo phần trăm giá khóa học) hoặc `FIXED` (giảm số tiền cố định).
- **Voucher_Status**: Cờ persisted trong DB, nhận một trong hai giá trị `ACTIVE` (đang phát hành) hoặc `INACTIVE` (đã tắt thủ công bởi admin hoặc đã bị soft-delete).
- **Voucher_Effective_Status**: Trạng thái tính toán động tại thời điểm đánh giá voucher, nhận một trong các giá trị: `ACTIVE`, `INACTIVE`, `NOT_YET_ACTIVE` (chưa tới `validFrom`), `EXPIRED` (quá `validTo`), `EXHAUSTED` (đã đạt `usageLimit` toàn cục).
- **Voucher_Scope**: Phạm vi áp dụng. Một trong hai giá trị: `ALL_COURSES` (áp dụng cho mọi khóa học) hoặc `SPECIFIC_COURSES` (chỉ áp dụng cho danh sách khóa học cụ thể).
- **Voucher_Usage_Limit**: Tổng số lượt voucher có thể được tiêu thụ thành công trên toàn hệ thống. `0` được hiểu là không giới hạn.
- **Voucher_Usage_Per_User**: Số lượt tối đa mà MỘT user được phép tiêu thụ cùng một voucher. `0` được hiểu là không giới hạn.
- **Voucher_Min_Order_Amount**: Giá khóa học gốc (`originalPrice`) tối thiểu để voucher có thể áp dụng. Nếu giá gốc nhỏ hơn ngưỡng này, voucher không hợp lệ.
- **Voucher_Max_Discount**: Mức giảm tối đa tính theo đơn vị tiền. Áp dụng cho voucher loại `PERCENT` để chặn trường hợp khóa học rất đắt bị giảm quá nhiều. Với voucher `FIXED`, trường này được bỏ qua.
- **Voucher_Valid_From** / **Voucher_Valid_To**: Khoảng thời gian có hiệu lực, kiểu `LocalDateTime`. Voucher chỉ tiêu thụ được khi `validFrom ≤ now ≤ validTo`.
- **Voucher_Usage**: Bản ghi xác nhận một lượt voucher đã được tiêu thụ thành công trong luồng purchase. Mỗi bản ghi gắn với `voucherId`, `userId`, `courseId`, `enrollmentId`, `originalPrice`, `discountAmount`, `finalPrice`, `appliedAt`.
- **Original_Price**: Giá khóa học hiện hành lưu trong cột `courses.price`, do server tự tra từ DB.
- **Discount_Amount**: Số tiền giảm thực tế, do Pricing_Engine tính ra. Luôn `0 ≤ discountAmount ≤ originalPrice`.
- **Final_Price**: Giá cuối cùng client phải trả, bằng `originalPrice − discountAmount`. Luôn `≥ 0`.
- **Pricing_Engine**: Domain service thuần (không phụ thuộc Spring/JPA) chịu trách nhiệm tính `(originalPrice, voucher) → (originalPrice, discountAmount, finalPrice)`. Pure function.
- **Voucher_Validator**: Domain service thuần kiểm tra một voucher có hợp lệ trong ngữ cảnh `(user, course, originalPrice, now)` hay không. Trả về thành công hoặc ném domain exception cụ thể.
- **Price_Preview_UseCase**: UseCase tính giá hiển thị. Read-only, không ghi DB, không tiêu thụ voucher.
- **Purchase_With_Voucher_UseCase**: UseCase mua khóa học có (hoặc không có) voucher. Tính lại giá ở server, lock voucher, trừ tiền ví, ghi enrollment, ghi `Voucher_Usage`, ghi audit log.
- **Voucher_Repository**: Repository interface ở `application/repository/Voucher/` để load và lưu Voucher.
- **Voucher_Usage_Repository**: Repository interface ở `application/repository/Voucher/` để load và lưu bản ghi Voucher_Usage.
- **Internal_Member**: User có `role = MEMBER` và `isInternal = true`. Luôn mua khóa học với giá 0đ, không sử dụng voucher.
- **External_Member**: User có `role = MEMBER` và `isInternal = false`. Đối tượng duy nhất sử dụng voucher.

---

## Requirements

### Requirement 1: Mô hình dữ liệu Voucher

**User Story:** Là STAFF hoặc SUPER_ADMIN, tôi muốn có một mô hình dữ liệu voucher đầy đủ, để có thể cấu hình các chương trình khuyến mãi linh hoạt mà vẫn nhất quán.

#### Acceptance Criteria

1. THE Voucher_Repository SHALL lưu trữ voucher với các trường bắt buộc: `id` (Long, PK), `code` (String, UNIQUE, lưu uppercase), `type` (`PERCENT` hoặc `FIXED`), `value` (BigDecimal, > 0), `status` (`ACTIVE` hoặc `INACTIVE`), `scope` (`ALL_COURSES` hoặc `SPECIFIC_COURSES`), `validFrom` (LocalDateTime), `validTo` (LocalDateTime), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime).
2. THE Voucher_Repository SHALL lưu trữ các trường tùy chọn: `minOrderAmount` (BigDecimal, ≥ 0, mặc định 0), `maxDiscount` (BigDecimal, ≥ 0, có ý nghĩa khi `type = PERCENT`), `usageLimit` (Long, ≥ 0, `0` nghĩa là không giới hạn, mặc định 0), `usagePerUser` (Integer, ≥ 0, `0` nghĩa là không giới hạn, mặc định 0).
3. WHERE `scope = SPECIFIC_COURSES`, THE Voucher_Repository SHALL lưu thêm danh sách `applicableCourseIds` (Set<Long>), không rỗng.
4. WHEN tạo voucher loại `PERCENT`, THE Voucher_Validator SHALL yêu cầu `0 < value ≤ 100`.
5. WHEN tạo voucher loại `FIXED`, THE Voucher_Validator SHALL yêu cầu `value > 0`.
6. THE Voucher_Repository SHALL áp dụng ràng buộc `validFrom ≤ validTo` ở tầng domain trước khi save.
7. THE Voucher_Repository SHALL chuẩn hóa `code` về uppercase trước khi save, để bảo đảm tính duy nhất không phân biệt hoa thường.
8. WHERE `scope = SPECIFIC_COURSES`, THE Voucher_Validator SHALL yêu cầu mọi `courseId` trong `applicableCourseIds` tồn tại trong `Course_Repository` tại thời điểm tạo / cập nhật voucher.

---

### Requirement 2: Quản lý Voucher (CRUD cho STAFF / SUPER_ADMIN)

**User Story:** Là STAFF hoặc SUPER_ADMIN, tôi muốn tạo, sửa, xóa và xem voucher, để quản lý chương trình khuyến mãi của hệ thống.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/admin/vouchers` được gửi bởi user có role STAFF hoặc SUPER_ADMIN với body hợp lệ, THE Admin_Voucher_Controller SHALL gọi Create_Voucher_UseCase và trả về voucher vừa tạo cùng HTTP 201.
2. IF một request tới `/api/v1/admin/vouchers/**` được gửi bởi user có role MEMBER, INSTRUCTOR, hoặc ADMIN_USER, THEN THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
3. WHEN một request `PUT /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL gọi Update_Voucher_UseCase và trả về voucher đã cập nhật cùng HTTP 200.
4. IF Update_Voucher_UseCase được gọi với `id` không tồn tại, THEN THE Update_Voucher_UseCase SHALL ném `VoucherNotFoundException` để Global_Exception_Handler trả về HTTP 404 với mã `VOUCHER_NOT_FOUND`.
5. WHEN một request `DELETE /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL gọi Delete_Voucher_UseCase, đặt `status = INACTIVE` (soft-delete) và trả về HTTP 200.
6. IF Delete_Voucher_UseCase được gọi với một voucher đã có ít nhất một bản ghi Voucher_Usage, THEN THE Delete_Voucher_UseCase SHALL chỉ thực hiện soft-delete (set `status = INACTIVE`) để bảo toàn lịch sử tiêu thụ, KHÔNG xóa cứng.
7. WHEN một request `GET /api/v1/admin/vouchers` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL trả về danh sách voucher có phân trang (`page`, `size`, `totalElements`, `totalPages`).
8. WHEN một request `GET /api/v1/admin/vouchers/{id}` được gửi bởi STAFF hoặc SUPER_ADMIN, THE Admin_Voucher_Controller SHALL trả về chi tiết voucher kèm hai trường tính toán: `usedCount` (đếm số bản ghi Voucher_Usage tương ứng) và `effectiveStatus` (Voucher_Effective_Status tại thời điểm gọi).
9. WHILE voucher đã có ít nhất một bản ghi Voucher_Usage, THE Update_Voucher_UseCase SHALL từ chối thay đổi các trường `code`, `type`, và `value`. Việc này tránh thay đổi ngữ nghĩa của các lượt tiêu thụ đã ghi nhận.
10. IF Update_Voucher_UseCase nhận `usageLimit` mới nhỏ hơn `usedCount` hiện tại VÀ `usageLimit` mới khác 0, THEN THE Update_Voucher_UseCase SHALL ném `VoucherUsageLimitTooLowException` để trả về HTTP 400 với mã `VOUCHER_USAGE_LIMIT_TOO_LOW`.

---

### Requirement 3: API tính giá xem trước (Price Preview)

**User Story:** Là MEMBER bên ngoài, tôi muốn nhập mã voucher và biết trước giá cuối cùng của khóa học, để quyết định có thanh toán hay không.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/courses/{courseId}/price-preview` với body `{ "voucherCode": "<code>" }` được gửi bởi user đã đăng nhập, THE Price_Preview_Controller SHALL gọi Price_Preview_UseCase và trả về kết quả định giá kèm HTTP 200.
2. THE Price_Preview_Request DTO SHALL chỉ khai báo trường `voucherCode` (String, tùy chọn, có thể null hoặc rỗng). DTO SHALL KHÔNG khai báo bất kỳ trường nào liên quan đến giá tiền (`price`, `originalPrice`, `discount`, `discountAmount`, `finalPrice`, `paidPrice`).
3. THE Price_Preview_UseCase SHALL đọc `originalPrice` của khóa học bằng `Course_Repository.findById(courseId)` tại thời điểm xử lý request và SHALL KHÔNG nhận giá tiền từ bất kỳ nguồn nào do client cung cấp.
4. WHEN `voucherCode` trong request là null hoặc chuỗi rỗng sau khi `trim()`, THE Price_Preview_UseCase SHALL trả về `{ originalPrice, discountAmount: 0, finalPrice: originalPrice, voucherApplied: false }`.
5. WHEN voucher hợp lệ và áp dụng được, THE Price_Preview_UseCase SHALL trả về `{ originalPrice, discountAmount, finalPrice, voucherApplied: true, voucherCode (uppercase), voucherType }`.
6. IF `courseId` không tồn tại, THEN THE Price_Preview_UseCase SHALL ném `CourseNotFoundException` để trả về HTTP 404 với mã `COURSE_NOT_FOUND`.
7. IF user gọi Price_Preview_UseCase là Internal_Member, THEN THE Price_Preview_UseCase SHALL trả về `{ originalPrice, discountAmount: originalPrice, finalPrice: 0, voucherApplied: false, internalDiscount: true }` và SHALL bỏ qua hoàn toàn `voucherCode`.
8. IF user gọi Price_Preview_UseCase có role thuộc tập {INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN} (tức không phải MEMBER), THEN THE Price_Preview_UseCase SHALL ném `VoucherUseDeniedException` để trả về HTTP 403 với mã `VOUCHER_USE_DENIED`.
9. THE Price_Preview_UseCase SHALL KHÔNG ghi vào Voucher_Repository, Voucher_Usage_Repository, hay Wallet. Mọi cuộc gọi đều read-only.
10. WHEN `originalPrice` của khóa học bằng 0, THE Price_Preview_UseCase SHALL trả về `{ originalPrice: 0, discountAmount: 0, finalPrice: 0, voucherApplied: false }` ngay cả khi `voucherCode` được cung cấp và voucher hợp lệ. Voucher không có ý nghĩa với khóa học miễn phí.

---

### Requirement 4: Validation voucher tại thời điểm tính giá

**User Story:** Là người dùng API, tôi muốn nhận thông báo lỗi rõ ràng và nhất quán khi voucher không áp dụng được, để biết lý do và xử lý phù hợp.

#### Acceptance Criteria

1. THE Voucher_Validator SHALL chuẩn hóa `voucherCode` đầu vào bằng `trim()` và chuyển sang uppercase trước khi tra cứu Voucher_Repository.
2. IF voucher với `code` đã chuẩn hóa không tồn tại trong Voucher_Repository, THEN THE Voucher_Validator SHALL ném `VoucherNotFoundException` để trả về HTTP 404 với mã `VOUCHER_NOT_FOUND`.
3. IF voucher có `status = INACTIVE`, THEN THE Voucher_Validator SHALL ném `VoucherInactiveException` để trả về HTTP 400 với mã `VOUCHER_INACTIVE`.
4. IF thời điểm hiện tại nhỏ hơn `validFrom`, THEN THE Voucher_Validator SHALL ném `VoucherNotYetActiveException` để trả về HTTP 400 với mã `VOUCHER_NOT_YET_ACTIVE`.
5. IF thời điểm hiện tại lớn hơn `validTo`, THEN THE Voucher_Validator SHALL ném `VoucherExpiredException` để trả về HTTP 400 với mã `VOUCHER_EXPIRED`.
6. IF voucher có `scope = SPECIFIC_COURSES` VÀ `courseId` không nằm trong `applicableCourseIds`, THEN THE Voucher_Validator SHALL ném `VoucherNotApplicableException` để trả về HTTP 400 với mã `VOUCHER_NOT_APPLICABLE`.
7. IF `originalPrice` nhỏ hơn `minOrderAmount`, THEN THE Voucher_Validator SHALL ném `VoucherMinOrderNotMetException` để trả về HTTP 400 với mã `VOUCHER_MIN_ORDER_NOT_MET`.
8. IF voucher có `usageLimit > 0` VÀ tổng số bản ghi Voucher_Usage cho voucher này đã đạt `usageLimit`, THEN THE Voucher_Validator SHALL ném `VoucherUsageLimitReachedException` để trả về HTTP 409 với mã `VOUCHER_USAGE_LIMIT_REACHED`.
9. IF voucher có `usagePerUser > 0` VÀ user hiện tại đã có số bản ghi Voucher_Usage cho voucher này đạt `usagePerUser`, THEN THE Voucher_Validator SHALL ném `VoucherUsagePerUserExceededException` để trả về HTTP 409 với mã `VOUCHER_USAGE_PER_USER_EXCEEDED`.
10. THE Voucher_Validator SHALL kiểm tra các điều kiện theo thứ tự cố định: tồn tại → status → thời gian (validFrom rồi validTo) → scope → minOrder → usageLimit toàn cục → usagePerUser. Cùng đầu vào cho cùng kết quả lỗi giữa các lần gọi.

---

### Requirement 5: Pricing Engine — Domain Logic Thuần

**User Story:** Là kiến trúc sư hệ thống, tôi muốn logic tính giá nằm ở Domain Service thuần, để dễ test bằng property-based testing và tái sử dụng nhất quán giữa luồng preview và purchase.

#### Acceptance Criteria

1. THE Pricing_Engine SHALL được đặt tại `domain/service/PricingEngine.java` và SHALL là class thuần Java, KHÔNG phụ thuộc Spring, JPA, hay framework nào.
2. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher = null`, THE Pricing_Engine SHALL trả về `PriceQuote{ originalPrice, discountAmount = 0, finalPrice = originalPrice }`.
3. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher.type = PERCENT`, THE Pricing_Engine SHALL tính `rawDiscount = originalPrice × voucher.value / 100` (làm tròn HALF_UP đến 2 chữ số thập phân) và `discountAmount = min(rawDiscount, voucher.maxDiscount)` khi `maxDiscount > 0`, ngược lại `discountAmount = rawDiscount`.
4. WHEN Pricing_Engine.compute(`originalPrice`, `voucher`) được gọi với `voucher.type = FIXED`, THE Pricing_Engine SHALL tính `discountAmount = min(voucher.value, originalPrice)`.
5. THE Pricing_Engine SHALL bảo đảm `0 ≤ discountAmount ≤ originalPrice` cho mọi đầu vào hợp lệ.
6. THE Pricing_Engine SHALL bảo đảm `finalPrice = originalPrice − discountAmount` và `0 ≤ finalPrice ≤ originalPrice` cho mọi đầu vào hợp lệ.
7. THE Pricing_Engine SHALL sử dụng kiểu `BigDecimal` cho mọi phép tính tiền và SHALL KHÔNG sử dụng `double` hoặc `float`.
8. IF `originalPrice` âm, THEN THE Pricing_Engine SHALL ném `IllegalArgumentException`. Đây là lỗi lập trình, không phải lỗi nghiệp vụ.
9. THE Pricing_Engine SHALL là pure function: cùng cặp đầu vào (`originalPrice`, voucher snapshot) trả về cùng kết quả, KHÔNG phụ thuộc vào thời gian, random, hay state ngoài.

---

### Requirement 6: API Purchase có dùng voucher (chống tampering & race condition)

**User Story:** Là MEMBER bên ngoài, tôi muốn mua khóa học với mã voucher để được giảm giá, và tin tưởng rằng giá tôi trả là đúng giá khuyến mãi tại thời điểm thanh toán.

#### Acceptance Criteria

1. WHEN một request `POST /api/v1/courses/{courseId}/purchase` với body `{ "voucherCode": "<code>" }` được gửi bởi user đã đăng nhập, THE Purchase_Controller SHALL gọi Purchase_With_Voucher_UseCase và trả về kết quả mua hàng kèm HTTP 200.
2. THE Purchase_Request DTO SHALL chỉ khai báo trường `voucherCode` (String, tùy chọn). DTO SHALL KHÔNG khai báo bất kỳ trường nào liên quan đến giá tiền (`price`, `originalPrice`, `discount`, `discountAmount`, `finalPrice`, `paidPrice`).
3. THE Purchase_With_Voucher_UseCase SHALL đọc `originalPrice` của khóa học bằng `Course_Repository.findById(courseId)` tại thời điểm xử lý request và SHALL KHÔNG nhận giá tiền từ bất kỳ nguồn nào do client cung cấp.
4. THE Purchase_With_Voucher_UseCase SHALL gọi Pricing_Engine để tính lại `discountAmount` và `finalPrice` từ đầu trong mỗi cuộc gọi purchase, KHÔNG sử dụng bất kỳ kết quả nào của Price_Preview_UseCase trước đó.
5. THE Purchase_With_Voucher_UseCase SHALL chạy toàn bộ logic trong một transaction `@Transactional` duy nhất bao gồm: load course với pessimistic lock, load user với pessimistic lock, load voucher với pessimistic lock (nếu có), validate voucher, tính giá, trừ tiền ví, tạo Enrollment, tạo Voucher_Usage (nếu có voucher), ghi audit log.
6. WHEN voucherCode không null hoặc rỗng, THE Purchase_With_Voucher_UseCase SHALL load voucher với `@Lock(LockModeType.PESSIMISTIC_WRITE)` để hai request đồng thời với cùng voucher được tuần tự hóa.
7. WHILE giữ pessimistic lock trên voucher, THE Purchase_With_Voucher_UseCase SHALL đếm lại tổng `usedCount` (toàn cục) và `usedCount theo user` từ Voucher_Usage_Repository, để giá trị đếm phản ánh đúng tình trạng tại thời điểm khóa.
8. IF user là Internal_Member, THEN THE Purchase_With_Voucher_UseCase SHALL bỏ qua `voucherCode` (kể cả khi client gửi lên), đặt `paidPrice = 0`, KHÔNG tạo Voucher_Usage, KHÔNG ném lỗi.
9. IF user có role thuộc tập {INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN} VÀ `voucherCode` không null hoặc rỗng, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherUseDeniedException` để trả về HTTP 403 với mã `VOUCHER_USE_DENIED`.
10. IF user đã có Enrollment cho khóa học này từ trước, THEN THE Purchase_With_Voucher_UseCase SHALL ném `AlreadyEnrolledException` để trả về HTTP 400 với mã `ALREADY_ENROLLED`, và SHALL KHÔNG tạo Voucher_Usage, KHÔNG trừ tiền.
11. IF số dư ví của user nhỏ hơn `finalPrice` đã tính ở server, THEN THE Purchase_With_Voucher_UseCase SHALL ném `InsufficientBalanceException` để trả về HTTP 400 với mã `INSUFFICIENT_BALANCE`, và SHALL KHÔNG tạo Voucher_Usage, KHÔNG trừ tiền.
12. WHEN purchase thành công có voucher, THE Purchase_With_Voucher_UseCase SHALL tạo đúng MỘT bản ghi Voucher_Usage chứa `(voucherId, userId, courseId, enrollmentId, originalPrice, discountAmount, finalPrice, appliedAt)` với `appliedAt = now()` của server.
13. WHEN purchase thành công, THE Purchase_Controller SHALL trả về body `{ enrollmentId, originalPrice, discountAmount, finalPrice, paidPrice, voucherApplied, voucherCode (nếu có) }` với `paidPrice = finalPrice`.
14. THE Purchase_With_Voucher_UseCase SHALL ghi audit log dạng JSONL vào `logs/purchase_ledger.jsonl` (mở rộng `PurchaseLedgerService` đã có) bao gồm `event`, `userId`, `courseId`, `voucherCode` (uppercase nếu có), `originalPrice`, `discountAmount`, `finalPrice`, `enrollmentId`, `appliedAt`.

---

### Requirement 7: Chống tampering courseId và price (Mối lo trọng tâm của user)

**User Story:** Là chủ hệ thống, tôi muốn bảo đảm client không thể đổi `courseId` hoặc gửi giá thấp hơn để lấy giá rẻ hơn cho khóa học đắt hơn.

#### Acceptance Criteria

1. THE Price_Preview_UseCase SHALL tính giá DUY NHẤT dựa trên `courseId` ở path và giá hiện hành của chính `courseId` đó trong DB. Không tồn tại tham số nào trong API cho phép client gợi ý giá.
2. THE Purchase_With_Voucher_UseCase SHALL tính giá DUY NHẤT dựa trên `courseId` ở path và giá hiện hành của chính `courseId` đó trong DB tại thời điểm purchase. KHÔNG có nhánh code nào "tin" kết quả của Price_Preview_UseCase trước đó.
3. THE Purchase_With_Voucher_UseCase SHALL KHÔNG bao giờ chấp nhận hay sử dụng các tham số như `quotedPrice`, `quotedDiscount`, `previewId`, `priceToken` từ client. Mỗi lần purchase là một lần tính lại giá độc lập.
4. WHEN Pricing_Engine và Voucher_Validator được gọi từ Purchase_With_Voucher_UseCase, THE Purchase_With_Voucher_UseCase SHALL truyền `originalPrice` đọc trực tiếp từ Course_Repository tại thời điểm purchase, KHÔNG truyền giá nào do client cung cấp.
5. THE Price_Preview_Controller và Purchase_Controller SHALL trả về cùng `originalPrice`, `discountAmount`, `finalPrice` khi `courseId`, `voucherCode`, `userId`, và state DB không đổi giữa hai lần gọi (metamorphic property: `pricePreview(c, v).priceFields == purchase(c, v).priceFields`).
6. IF client gửi body chứa các trường `price`, `originalPrice`, `discount`, `discountAmount`, `finalPrice`, hoặc `paidPrice` cho cả endpoint price-preview và purchase, THEN THE Spring_MVC_Layer SHALL KHÔNG bind các trường này vào DTO (vì DTO không khai báo chúng) và SHALL KHÔNG gây lỗi (giữ tương thích với client gửi field thừa). Server SHALL hoàn toàn bỏ qua giá tiền do client gửi.
7. THE Course_Repository SHALL được gọi với `findById(courseId)` cứng trong cả hai luồng pricing. KHÔNG có nhánh code nào tra course bằng tên, slug, hay alias.
8. WHEN ghi audit log, THE Purchase_With_Voucher_UseCase SHALL ghi rõ `courseId`, `originalPriceAtPurchase`, `voucherCode`, `discountAmount`, `finalPrice` để có thể đối soát ngược nếu phát hiện bất thường.

---

### Requirement 8: Validity của voucher tại thời điểm purchase (không tin trạng thái preview)

**User Story:** Là chủ hệ thống, tôi muốn voucher được kiểm tra lại tại thời điểm purchase, để xử lý trường hợp voucher hết hạn / hết quota giữa lần preview và lần purchase.

#### Acceptance Criteria

1. THE Purchase_With_Voucher_UseCase SHALL gọi lại Voucher_Validator tại thời điểm purchase, KHÔNG dựa trên kết quả validation của bất kỳ Price_Preview_UseCase nào trước đó.
2. WHILE giữ pessimistic lock trên voucher, THE Purchase_With_Voucher_UseCase SHALL kiểm tra `validFrom`, `validTo`, `status`, `scope`, `minOrderAmount`, `usageLimit`, `usagePerUser` lần nữa với dữ liệu mới nhất trong DB.
3. IF voucher đã hết hạn (`now > validTo`) tại thời điểm purchase trong khi preview trước đó đã pass, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherExpiredException` (HTTP 400, mã `VOUCHER_EXPIRED`) và rollback transaction.
4. IF voucher đã đạt `usageLimit` tại thời điểm purchase trong khi preview trước đó đã pass, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherUsageLimitReachedException` (HTTP 409, mã `VOUCHER_USAGE_LIMIT_REACHED`) và rollback transaction.
5. IF voucher đã bị admin set `status = INACTIVE` tại thời điểm purchase trong khi preview trước đó đã pass, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherInactiveException` (HTTP 400, mã `VOUCHER_INACTIVE`) và rollback transaction.
6. IF `originalPrice` của khóa học đã bị admin sửa giảm xuống dưới `minOrderAmount` của voucher tại thời điểm purchase, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherMinOrderNotMetException` (HTTP 400, mã `VOUCHER_MIN_ORDER_NOT_MET`) và rollback transaction.
7. THE Price_Preview_UseCase SHALL KHÔNG ghi bản ghi Voucher_Usage và SHALL KHÔNG cập nhật bất kỳ counter nào của voucher. Việc preview KHÔNG bao giờ "khóa" hoặc "giữ chỗ" lượt voucher cho user.

---

### Requirement 9: Concurrency safety trên quota voucher

**User Story:** Là chủ hệ thống, tôi muốn bảo đảm khi voucher chỉ còn 1 lượt dùng, chỉ MỘT trong số nhiều request đồng thời được thành công, không có chuyện vượt quá giới hạn.

#### Acceptance Criteria

1. WHEN nhiều request `POST /api/v1/courses/{courseId}/purchase` đồng thời cùng tham chiếu một `voucherId`, THE Purchase_With_Voucher_UseCase SHALL tuần tự hóa các request thông qua pessimistic write lock trên hàng voucher tương ứng.
2. WHILE giữ pessimistic lock trên voucher, THE Purchase_With_Voucher_UseCase SHALL đếm lại tổng `usedCount` để bảo đảm chưa vượt `usageLimit`. Việc đếm SHALL được thực hiện bên trong transaction và sau khi đã giữ lock.
3. IF tại thời điểm sau khi giữ lock, `usedCount ≥ usageLimit` (với `usageLimit > 0`), THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherUsageLimitReachedException` (HTTP 409, mã `VOUCHER_USAGE_LIMIT_REACHED`) và rollback toàn bộ transaction (KHÔNG tạo Enrollment, KHÔNG trừ tiền).
4. THE Voucher_Usage_Repository SHALL áp dụng ràng buộc UNIQUE trên `(voucherId, enrollmentId)` ở tầng DB để bảo đảm một enrollment tiêu thụ tối đa một lượt voucher, kể cả khi có race ở mức ứng dụng.
5. WHEN client gửi cùng một request `POST /api/v1/courses/{courseId}/purchase` hai lần liên tiếp (ví dụ do retry mạng) cho cùng `(userId, courseId)`, THE Purchase_With_Voucher_UseCase SHALL phát hiện qua `AlreadyEnrolledException` và lần thứ hai SHALL trả về HTTP 400 với mã `ALREADY_ENROLLED`, KHÔNG tạo Voucher_Usage thứ hai.
6. THE Voucher_Usage_Repository SHALL có index trên `(voucher_id)` và `(voucher_id, user_id)` để truy vấn `usedCount` toàn cục và `usedCount theo user` không trở thành nút thắt.
7. WHEN thứ tự load các bảng có lock trong cùng transaction, THE Purchase_With_Voucher_UseCase SHALL giữ thứ tự cố định: `Course → User → Voucher` để tránh deadlock giữa các transaction đồng thời.

---

### Requirement 10: Ràng buộc role-based — Internal Member và non-Member không dùng voucher

**User Story:** Là chủ hệ thống, tôi muốn voucher chỉ áp dụng cho External_Member, vì Internal_Member đã được mua khóa học giá 0đ và các role nội bộ khác không phải đối tượng mua khóa học.

#### Acceptance Criteria

1. WHEN user gọi endpoint `POST /api/v1/courses/{courseId}/price-preview` HOẶC `POST /api/v1/courses/{courseId}/purchase` với `voucherCode` không null hoặc rỗng, THE Purchase_With_Voucher_UseCase và Price_Preview_UseCase SHALL kiểm tra role và `isInternal` của user.
2. IF user có role MEMBER và `isInternal = true` (Internal_Member), THEN THE Price_Preview_UseCase SHALL bỏ qua `voucherCode` và trả về kết quả `{ finalPrice = 0, voucherApplied = false, internalDiscount = true }`.
3. IF user có role MEMBER và `isInternal = true` (Internal_Member), THEN THE Purchase_With_Voucher_UseCase SHALL bỏ qua `voucherCode` và đặt `paidPrice = 0`, KHÔNG tạo Voucher_Usage, KHÔNG ném lỗi.
4. IF user có role thuộc tập {INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN}, THEN THE Price_Preview_UseCase SHALL ném `VoucherUseDeniedException` để trả về HTTP 403 với mã `VOUCHER_USE_DENIED` ngay cả khi `voucherCode` là null.
5. IF user có role thuộc tập {INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN}, THEN THE Purchase_With_Voucher_UseCase SHALL ném `VoucherUseDeniedException` (HTTP 403, mã `VOUCHER_USE_DENIED`) khi `voucherCode` không null hoặc rỗng. Khi `voucherCode` là null, hành vi purchase được giữ nguyên như flow purchase hiện tại (không thay đổi).
6. THE Price_Preview_Controller SHALL được mount riêng cho voucher preview. Nếu user không phải MEMBER, controller SHALL trả lỗi 403 thay vì trả giá gốc (vì các role này không có nhu cầu xem giá voucher).

---

### Requirement 11: Phân quyền Voucher (RBAC permission)

**User Story:** Là chủ hệ thống, tôi muốn phân quyền rõ ràng cho voucher trong RBAC để tránh lạm dụng.

#### Acceptance Criteria

1. THE Permission_Seed SHALL thêm bốn permission mới vào bảng `permissions`: `CREATE_VOUCHER`, `EDIT_VOUCHER`, `DELETE_VOUCHER`, `VIEW_VOUCHER`.
2. THE Permission_Seed SHALL gán cả bốn permission `CREATE_VOUCHER`, `EDIT_VOUCHER`, `DELETE_VOUCHER`, `VIEW_VOUCHER` cho role STAFF.
3. THE Permission_Seed SHALL gán cả bốn permission cho role SUPER_ADMIN.
4. THE Permission_Seed SHALL KHÔNG gán bất kỳ permission voucher nào cho các role MEMBER, INSTRUCTOR, ADMIN_USER (vì voucher quản trị nằm ngoài trách nhiệm của họ).
5. WHEN một user gọi tới `POST /api/v1/admin/vouchers` mà không có `CREATE_VOUCHER`, THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
6. WHEN một user gọi tới `PUT /api/v1/admin/vouchers/{id}` mà không có `EDIT_VOUCHER`, THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
7. WHEN một user gọi tới `DELETE /api/v1/admin/vouchers/{id}` mà không có `DELETE_VOUCHER`, THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
8. WHEN một user gọi tới `GET /api/v1/admin/vouchers/**` mà không có `VIEW_VOUCHER`, THE Security_Layer SHALL trả về HTTP 403 với mã `VOUCHER_ACCESS_DENIED`.
9. THE Permission_Matrix_Documentation SHALL được cập nhật trong `docs/permission-matrix.md` để phản ánh bốn permission mới.

---

### Requirement 12: Audit trail cho voucher

**User Story:** Là quản trị viên, tôi muốn có audit trail đầy đủ về voucher để điều tra khi nghi ngờ lạm dụng hoặc cần đối soát giá.

#### Acceptance Criteria

1. WHEN một bản ghi Voucher_Usage được tạo thành công, THE Purchase_With_Voucher_UseCase SHALL ghi một dòng JSONL vào `logs/purchase_ledger.jsonl` với các trường: `event = "VOUCHER_APPLIED"`, `userId`, `courseId`, `voucherId`, `voucherCode` (uppercase), `originalPrice`, `discountAmount`, `finalPrice`, `enrollmentId`, `appliedAt`.
2. WHEN voucher validation thất bại trong luồng purchase (sau khi đã giữ lock), THE Purchase_With_Voucher_UseCase SHALL ghi một dòng JSONL với `event = "VOUCHER_REJECTED"`, `userId`, `courseId`, `voucherCode` (uppercase), `reason` (mã lỗi như `VOUCHER_EXPIRED`, `VOUCHER_USAGE_LIMIT_REACHED`...).
3. THE Voucher_Usage_Repository SHALL hỗ trợ truy vấn `findByUserId(userId, page, size)` và `findByVoucherId(voucherId, page, size)` để phục vụ admin xem lịch sử.
4. THE Admin_Voucher_Controller SHALL cung cấp endpoint `GET /api/v1/admin/vouchers/{id}/usages` (yêu cầu `VIEW_VOUCHER`) trả về danh sách Voucher_Usage có phân trang.
5. THE Voucher_Usage_Repository SHALL KHÔNG cho phép xóa hoặc cập nhật bản ghi Voucher_Usage. Bảng `voucher_usages` là append-only ở mức ứng dụng (chỉ insert, không update / delete).

---

### Requirement 13: Validation đầu vào ở tầng adapter

**User Story:** Là người dùng API, tôi muốn nhận lỗi rõ ràng khi gửi request sai định dạng để biết cách sửa nhanh chóng.

#### Acceptance Criteria

1. THE Price_Preview_Request DTO SHALL khai báo `voucherCode` là `String` tùy chọn, độ dài 0–32 ký tự.
2. WHEN `voucherCode` chứa ký tự ngoài tập `A-Z`, `a-z`, `0-9`, `_`, `-`, THE Bean_Validation SHALL trả về HTTP 400 với mã `VALIDATION_ERROR` và message chỉ rõ ký tự không hợp lệ.
3. THE Create_Voucher_Request DTO SHALL khai báo các trường bắt buộc: `code` (4–32 ký tự, regex `^[A-Za-z0-9_-]+$`), `type` (`PERCENT` hoặc `FIXED`), `value` (BigDecimal, > 0), `validFrom` (ISO-8601 LocalDateTime), `validTo` (ISO-8601 LocalDateTime), `scope` (`ALL_COURSES` hoặc `SPECIFIC_COURSES`).
4. THE Create_Voucher_Request DTO SHALL khai báo các trường tùy chọn: `minOrderAmount` (BigDecimal, ≥ 0, mặc định 0), `maxDiscount` (BigDecimal, ≥ 0, mặc định 0), `usageLimit` (Long, ≥ 0, mặc định 0), `usagePerUser` (Integer, ≥ 0, mặc định 0), `applicableCourseIds` (Set<Long>, không rỗng khi `scope = SPECIFIC_COURSES`).
5. WHEN `validFrom > validTo` trong Create_Voucher_Request, THE Create_Voucher_UseCase SHALL ném `VoucherDateRangeInvalidException` để trả về HTTP 400 với mã `VOUCHER_DATE_RANGE_INVALID`.
6. WHEN `type = PERCENT` và `value > 100` trong Create_Voucher_Request, THE Create_Voucher_UseCase SHALL ném `VoucherPercentOutOfRangeException` để trả về HTTP 400 với mã `VOUCHER_PERCENT_OUT_OF_RANGE`.
7. WHEN `scope = SPECIFIC_COURSES` nhưng `applicableCourseIds` rỗng hoặc null, THE Create_Voucher_UseCase SHALL ném `VoucherScopeMismatchException` để trả về HTTP 400 với mã `VOUCHER_SCOPE_MISMATCH`.
8. WHEN `scope = ALL_COURSES` và `applicableCourseIds` không rỗng, THE Create_Voucher_UseCase SHALL ném `VoucherScopeMismatchException` để trả về HTTP 400 với mã `VOUCHER_SCOPE_MISMATCH`.
9. WHEN tạo voucher với `code` đã tồn tại (sau khi chuẩn hóa uppercase), THE Create_Voucher_UseCase SHALL ném `VoucherCodeAlreadyExistsException` để trả về HTTP 409 với mã `VOUCHER_CODE_ALREADY_EXISTS`.
