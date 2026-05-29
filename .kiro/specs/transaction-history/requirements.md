# Requirements Document

## Introduction

Tính năng **Transaction History (Lịch sử giao dịch)** cho phép user đã đăng nhập xem lại các giao dịch ví của chính mình trên trang Ví tiền (`WalletPage`). Hiện tại trang Ví đã hiển thị số dư và cho phép nạp tiền, nhưng phần "Lịch sử giao dịch" mới chỉ là placeholder tĩnh ("Lịch sử giao dịch sẽ hiển thị ở đây.") vì backend chưa có endpoint liệt kê giao dịch.

Hệ thống hiện đã có bảng `wallet_transactions` (model `WalletTransaction`) lưu **các giao dịch nạp tiền**:
- Nạp qua payment gateway (`source = MOCK` ở dev, `source = VIETQR` ở production) — tạo bản ghi `PENDING` rồi chuyển `COMPLETED` khi webhook xác nhận.
- Admin cộng tiền thủ công (`source = ADMIN`) — tạo bản ghi `COMPLETED` ngay.

Tuy nhiên, **giao dịch mua khóa học hiện KHÔNG được ghi vào `wallet_transactions`** — luồng mua chỉ gọi `user.deductBalance()` và ghi audit log dạng JSONL (`logs/purchase_ledger.jsonl`). Do đó nếu chỉ liệt kê `wallet_transactions`, user sẽ thấy tiền vào nhưng không thấy tiền ra, gây hiểu nhầm.

Tính năng này gồm:

1. **API liệt kê giao dịch của chính mình** (`GET /api/v1/users/me/transactions`) — phân trang, sắp xếp mới nhất trước, trả về cả giao dịch tiền vào (nạp) và tiền ra (mua khóa học).
2. **Ghi nhận giao dịch mua khóa học** vào `wallet_transactions` để lịch sử phản ánh đúng dòng tiền hai chiều.
3. **UI hiển thị lịch sử giao dịch** trên `WalletPage` thay cho placeholder, với phân trang / tải thêm, phân biệt tiền vào (+) và tiền ra (−), badge trạng thái và nguồn.

Yêu cầu trọng tâm: lịch sử **chỉ thuộc về chính người gọi** (không lộ giao dịch của user khác), dữ liệu tiền **chính xác và nhất quán** với số dư, và việc ghi nhận giao dịch mua hàng **không được làm hỏng hay chặn** luồng mua hiện có.

---

## Glossary

- **Wallet_Transaction**: Bản ghi một giao dịch ví, lưu ở bảng `wallet_transactions`. Gồm `id`, `userId`, `referenceCode`, `amount`, `status`, `source`, `note`, `createdAt`, `completedAt`, `expiredAt`.
- **Transaction_Direction**: Hướng dòng tiền của một giao dịch. `CREDIT` (tiền vào ví — nạp tiền) hoặc `DEBIT` (tiền ra khỏi ví — mua khóa học). Đây là khái niệm hiển thị, được suy ra từ `source`/`type`, không nhất thiết là cột DB rời.
- **Tx_Status**: Trạng thái giao dịch, enum đã có: `PENDING`, `COMPLETED`, `EXPIRED`, `FAILED`.
- **Tx_Source**: Nguồn giao dịch, enum hiện có: `MOCK`, `VIETQR`, `ADMIN`. Tính năng này bổ sung thêm `PURCHASE` (mua khóa học) để biểu diễn giao dịch tiền ra.
- **Transaction_History_Endpoint**: Endpoint `GET /api/v1/users/me/transactions` trả về danh sách Wallet_Transaction của chính người gọi, có phân trang.
- **Get_My_Transactions_UseCase**: UseCase ở `application/usecase/Wallet/` (hoặc `User/`) đọc danh sách giao dịch của người gọi theo `userId`, có phân trang, sắp xếp `createdAt DESC`.
- **Wallet_Transaction_Repository**: Repository interface `application/repository/Wallet/WalletTransactionRepository` (đã tồn tại) — bổ sung phương thức truy vấn theo `userId` có phân trang.
- **Transaction_Item_Output / Response**: DTO mô tả một dòng giao dịch trả về cho client.
- **Page_Result**: Record `application/dto/PageResult<T>` đã có (`totalElements`, `totalPages`, `page`, `size`, `items`).
- **Api_Response**: Envelope `adapter/dto/response/ApiResponse<T>` đã có (`status`, `message`, `data`, `timestamp`).
- **Purchase_Transaction_Recording**: Việc ghi một bản ghi Wallet_Transaction với `source = PURCHASE`, `status = COMPLETED`, `amount` = số tiền đã trả, mỗi khi user mua khóa học thành công (kể cả qua voucher checkout).
- **Requester**: User đang đăng nhập, xác định qua claim `userId` trong JWT (nhất quán với các endpoint `/me/*` hiện có).

---

## Requirements

### Requirement 1: API liệt kê giao dịch của chính mình

**User Story:** Là user đã đăng nhập, tôi muốn xem danh sách các giao dịch ví của chính mình theo trang, để theo dõi lịch sử nạp tiền và chi tiêu.

#### Acceptance Criteria

1. WHEN một request `GET /api/v1/users/me/transactions` được gửi bởi user đã đăng nhập, THE Transaction_History_Endpoint SHALL gọi Get_My_Transactions_UseCase với `userId` lấy từ claim JWT và trả về danh sách giao dịch của chính người gọi kèm HTTP 200.
2. THE Transaction_History_Endpoint SHALL chấp nhận tham số phân trang `page` (mặc định `0`, phải `>= 0`) và `size` (mặc định `20`, phải trong khoảng `[1, 100]`).
3. IF `page < 0` HOẶC `size` nằm ngoài `[1, 100]`, THEN THE Get_My_Transactions_UseCase SHALL ném `IllegalArgumentException` để Global_Exception_Handler trả về HTTP 400 với mã `BAD_REQUEST`.
4. THE Get_My_Transactions_UseCase SHALL sắp xếp kết quả theo `createdAt` giảm dần (mới nhất trước).
5. THE Transaction_History_Endpoint SHALL bọc kết quả trong `ApiResponse<PageResult<TransactionItemResponse>>` đúng convention của các endpoint `/me/*` hiện có.
6. WHEN người gọi chưa có giao dịch nào, THE Transaction_History_Endpoint SHALL trả về một trang rỗng (`items = []`, `totalElements = 0`) với HTTP 200, KHÔNG trả 404 hay 403.
7. THE Get_My_Transactions_UseCase SHALL chỉ trả về các giao dịch có `userId` bằng `userId` của người gọi. THE UseCase SHALL KHÔNG bao giờ trả về giao dịch của user khác, kể cả khi client cố truyền tham số `userId` trong query/body (mọi tham số định danh user do client gửi đều bị bỏ qua).
8. THE Get_My_Transactions_UseCase SHALL được đánh dấu `@Transactional(readOnly = true)`.

---

### Requirement 2: Dữ liệu trả về cho mỗi giao dịch

**User Story:** Là user, tôi muốn mỗi dòng giao dịch hiển thị đủ thông tin (số tiền, hướng tiền, trạng thái, nguồn, thời gian, mã tham chiếu), để hiểu rõ giao dịch đó là gì.

#### Acceptance Criteria

1. THE Transaction_Item_Output SHALL chứa các trường: `id`, `referenceCode`, `amount` (BigDecimal, luôn dương), `direction` (`CREDIT` hoặc `DEBIT`), `status` (Tx_Status), `source` (Tx_Source), `note` (nullable), `createdAt`, `completedAt` (nullable).
2. THE Get_My_Transactions_UseCase SHALL suy ra `direction = CREDIT` khi `source ∈ {MOCK, VIETQR, ADMIN}` (tiền vào ví) và `direction = DEBIT` khi `source = PURCHASE` (tiền ra khỏi ví).
3. THE Transaction_Item_Response SHALL được map từ Transaction_Item_Output ở tầng adapter và KHÔNG để lộ các trường nội bộ không cần thiết (ví dụ `expiredAt` không bắt buộc trả về cho client).
4. THE `amount` trong response SHALL luôn là số dương; hướng tiền được biểu diễn bằng `direction`, KHÔNG bằng dấu của `amount`.
5. WHERE giao dịch có `source = PURCHASE`, THE Transaction_Item_Output SHALL đặt `note` mô tả khóa học đã mua (ví dụ chứa `courseId` hoặc tiêu đề khóa học) để client hiển thị ngữ cảnh, trong giới hạn độ dài cột `note` (255 ký tự).
6. THE Transaction_Item_Response SHALL dùng kiểu `BigDecimal` cho `amount`, KHÔNG dùng `double`/`float`.

---

### Requirement 3: Ghi nhận giao dịch mua khóa học vào lịch sử

**User Story:** Là user, tôi muốn thấy cả các khoản chi khi mua khóa học trong lịch sử giao dịch, để lịch sử phản ánh đúng dòng tiền vào và ra của ví.

> Ghi chú phạm vi: Đây là yêu cầu mở rộng để lịch sử có ý nghĩa hai chiều. Nếu muốn thu hẹp phạm vi (chỉ hiển thị giao dịch nạp tiền), có thể bỏ Requirement 3 này; khi đó Requirement 2.2 chỉ còn nhánh `CREDIT`.

#### Acceptance Criteria

1. THE Tx_Source enum SHALL được bổ sung giá trị `PURCHASE` để biểu diễn giao dịch tiền ra khi mua khóa học.
2. WHEN một user mua khóa học thành công qua luồng checkout (bao gồm cả luồng có voucher) với `paidPrice > 0`, THE Purchase_Transaction_Recording SHALL tạo một bản ghi Wallet_Transaction với `userId` của người mua, `amount = paidPrice`, `source = PURCHASE`, `status = COMPLETED`, `createdAt = completedAt = now()`, và `note` mô tả khóa học.
3. WHEN `paidPrice = 0` (ví dụ Internal_Member mua giá 0đ hoặc khóa học miễn phí), THE Purchase_Transaction_Recording SHALL KHÔNG tạo bản ghi Wallet_Transaction (không có dòng tiền thực tế ra khỏi ví).
4. THE Purchase_Transaction_Recording SHALL chạy trong cùng transaction với việc trừ tiền ví và tạo Enrollment, để bảo đảm tính nhất quán: nếu mua thất bại và rollback thì bản ghi giao dịch cũng không tồn tại.
5. THE Purchase_Transaction_Recording SHALL sinh `referenceCode` duy nhất cho bản ghi mua hàng theo cùng cơ chế hiện có của Wallet_Transaction (đảm bảo ràng buộc UNIQUE trên `reference_code`).
6. THE Purchase_Transaction_Recording SHALL KHÔNG làm thay đổi giá trị `paidPrice`, số dư ví, hay kết quả trả về của luồng mua hàng hiện tại. Việc ghi nhận chỉ là thêm bản ghi lịch sử.
7. IF việc ghi bản ghi Wallet_Transaction thất bại do lỗi kỹ thuật, THEN THE Purchase flow SHALL coi đây là lỗi của transaction mua hàng và rollback toàn bộ (không để trạng thái "đã trừ tiền nhưng không có bản ghi"), nhất quán với Requirement 3.4.

---

### Requirement 4: Phân quyền và bảo mật

**User Story:** Là chủ hệ thống, tôi muốn bảo đảm mỗi user chỉ xem được giao dịch của chính mình, để không lộ dữ liệu tài chính của người khác.

#### Acceptance Criteria

1. THE Transaction_History_Endpoint SHALL yêu cầu xác thực JWT. IF request không có token hợp lệ, THEN THE Security_Layer SHALL trả về HTTP 401 (nhất quán với các endpoint `/me/*` hiện có).
2. THE Transaction_History_Endpoint SHALL cho phép mọi role đã đăng nhập (MEMBER, INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN) gọi, vì endpoint chỉ trả về giao dịch của chính người gọi.
3. THE Get_My_Transactions_UseCase SHALL xác định `userId` DUY NHẤT từ claim JWT do `JwtService` giải mã, KHÔNG từ tham số do client cung cấp.
4. THE Transaction_History_Endpoint SHALL KHÔNG cung cấp khả năng cho một user thường truy vấn giao dịch của `userId` khác. (Việc admin xem giao dịch của user khác, nếu cần, là một endpoint admin riêng nằm ngoài phạm vi spec này.)

---

### Requirement 5: Repository và truy vấn

**User Story:** Là kiến trúc sư hệ thống, tôi muốn truy vấn giao dịch theo user được thực hiện ở repository đúng tầng và có index, để hiệu năng tốt và tuân thủ Clean Architecture.

#### Acceptance Criteria

1. THE Wallet_Transaction_Repository (interface ở `application/repository/Wallet/`) SHALL bổ sung phương thức `PageResult<WalletTransaction> findByUserId(Long userId, int page, int size)` trả về kết quả phân trang, sắp xếp `createdAt DESC`.
2. THE Wallet_Transaction_Repository interface SHALL KHÔNG để lộ chi tiết JPA (`Page`, `Pageable`); việc chuyển đổi Spring `Page` sang `PageResult` SHALL nằm trong implementation ở tầng adapter.
3. THE adapter implementation SHALL dùng index `idx_wallet_tx_user` (đã có trên cột `user_id`) cho truy vấn này.
4. THE JPA repository nội bộ SHALL cung cấp `Page<WalletTransactionJpaEntity> findByUserId(Long userId, Pageable pageable)` với `Pageable` chứa sort `createdAt DESC`.
5. THE adapter implementation SHALL map mỗi `WalletTransactionJpaEntity` sang domain `WalletTransaction` rồi để UseCase map tiếp sang Output DTO, KHÔNG trả JPA entity ra ngoài tầng adapter.

---

### Requirement 6: Giao diện Lịch sử giao dịch trên WalletPage

**User Story:** Là user, tôi muốn thấy danh sách giao dịch của mình ngay trên trang Ví tiền, để không phải đi đâu khác để tra cứu.

#### Acceptance Criteria

1. WHEN user mở `WalletPage` và đã đăng nhập, THE WalletPage SHALL gọi Transaction_History_Endpoint và hiển thị danh sách giao dịch trong một **shadcn DataTable** (`@tanstack/react-table`) thay cho placeholder tĩnh hiện tại.
2. WHILE đang tải danh sách giao dịch, THE WalletPage SHALL hiển thị trạng thái loading (skeleton hoặc spinner), KHÔNG hiển thị placeholder "sẽ hiển thị ở đây".
3. WHEN danh sách giao dịch rỗng, THE DataTable SHALL hiển thị một dòng trạng thái rỗng thân thiện (ví dụ "Chưa có giao dịch nào.").
4. WHEN tải danh sách thất bại, THE WalletPage SHALL hiển thị thông báo lỗi và một nút thử lại.
5. THE DataTable SHALL có các cột: **Thời gian** (`createdAt` định dạng `vi-VN`), **Loại giao dịch** (icon hướng + nhãn nguồn, kèm `note` nếu có), **Trạng thái** (Badge), **Số tiền** (căn phải, `+`/màu xanh cho `CREDIT`, `−`/màu đỏ cho `DEBIT`).
6. THE WalletPage SHALL hỗ trợ phân trang server-side bằng nút **Trước / Sau** đặt dưới bảng, chỉ hiển thị khi `totalPages > 1`, kèm chỉ báo "Trang X/Y · N giao dịch". Nút Trước bị disable ở trang đầu, nút Sau bị disable ở trang cuối.
7. WHEN ví được nạp tiền thành công và FE nhận sự kiện WebSocket `WALLET_UPDATED` (cơ chế cập nhật số dư hiện có làm `balance` trong `AuthContext` thay đổi), THE WalletPage SHALL tải lại trang đầu của danh sách giao dịch để giao dịch mới xuất hiện mà không cần F5.
8. THE WalletPage SHALL định dạng số tiền bằng `Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })`, nhất quán với cách hiển thị số dư hiện tại.
9. THE FE API client SHALL bổ sung hàm gọi `GET /api/v1/users/me/transactions` (ví dụ `getTransactionsApi(page, size)`) trong `fe/src/api/wallet.js`, nhất quán với các hàm API ví hiện có.
10. THE FE SHALL bổ sung dependency `@tanstack/react-table` và primitive `fe/src/components/ui/table.jsx` (bộ wrapper bảng của shadcn, thuần Tailwind, không phụ thuộc Radix) làm nền cho DataTable. DataTable dùng SHALL là bản rút gọn (không có toolbar ẩn/hiện cột bằng dropdown), dùng `manualPagination` vì phân trang do server xử lý.

---

### Requirement 7: Tài liệu

**User Story:** Là thành viên nhóm, tôi muốn tài liệu API phản ánh endpoint mới, để FE/QA dùng đúng hợp đồng API.

#### Acceptance Criteria

1. THE API_Documentation SHALL bổ sung mô tả endpoint `GET /api/v1/users/me/transactions` vào `docs/API/wallet.md` (hoặc `docs/API/user.md` cho nhóm `/me/*`), gồm: tham số phân trang, ví dụ response thành công, response rỗng, và bảng mô tả field của mỗi giao dịch.
2. THE API_Documentation SHALL cập nhật bảng tóm tắt các endpoint `/api/v1/users/me/*` để thêm `GET /me/transactions`.
3. THE API_Documentation SHALL ghi rõ `direction`, `source` (bao gồm giá trị mới `PURCHASE`), và `status` có thể nhận giá trị nào.
4. THE API_Documentation trong `docs/API/endpoints-summary.md` SHALL liệt kê endpoint mới trong nhóm Wallet hoặc User Profile.

---

### Requirement 8: Kiểm thử

**User Story:** Là kỹ sư, tôi muốn có kiểm thử cho luồng liệt kê giao dịch và ghi nhận giao dịch mua hàng, để bảo đảm tính đúng đắn và tránh hồi quy.

#### Acceptance Criteria

1. THE Test_Suite SHALL có unit test cho Get_My_Transactions_UseCase: kết quả rỗng; một trang; nhiều trang với thứ tự `createdAt DESC` được xác minh; từ chối `size` không hợp lệ (`0`, `101`); từ chối `page` âm (`-1`).
2. THE Test_Suite SHALL có unit test xác minh Get_My_Transactions_UseCase chỉ truy vấn theo `userId` của người gọi (verify đối số truyền vào repository là `userId` từ JWT).
3. THE Test_Suite SHALL có test xác minh việc suy ra `direction` đúng cho từng `source` (`MOCK/VIETQR/ADMIN → CREDIT`, `PURCHASE → DEBIT`).
4. WHERE Requirement 3 (ghi nhận giao dịch mua hàng) được triển khai, THE Test_Suite SHALL có test xác minh: mua thành công với `paidPrice > 0` tạo đúng một bản ghi Wallet_Transaction `source = PURCHASE` `status = COMPLETED`; mua với `paidPrice = 0` KHÔNG tạo bản ghi; rollback transaction khi mua thất bại không để lại bản ghi giao dịch.
5. THE Test_Suite SHALL chạy được bằng `mvnw test` và xanh trước khi tính năng được coi là hoàn thành.
