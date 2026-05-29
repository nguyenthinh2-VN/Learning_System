# Bugfix Requirements Document

## Introduction

Tài liệu này mô tả phân tích lỗi cho 4 vấn đề liên quan trong Learning System (backend Spring Boot Clean Architecture + frontend React/Vite tại `fe/`). Cả 4 lỗi đều nằm trong khu vực quản trị (Admin/Instructor portal) và luồng thanh toán mua khóa học:

- **BUG 1** — Form "Cộng tiền thủ công" bắt admin nhập User ID dạng số khó nhớ, thay vì username/email.
- **BUG 2** — Nút "Xem trước" trên trang "Khóa học chờ duyệt" mở route công khai `/courses/{id}`, chạy dưới ngữ cảnh người học (public) nên không hiển thị nội dung khóa học chưa xuất bản cho admin.
- **BUG 3** — Sidebar admin tô sáng (active) đồng thời HAI mục menu (ví dụ "Khóa học" và "Chờ duyệt") gây nhầm lẫn.
- **BUG 4** — Trang "Xác nhận đăng ký" hiển thị "Số dư ví" là "NaN đ"; việc mua vẫn ghi nhận phía server, chỉ phần hiển thị số dư bị sai.

Mỗi lỗi được mô tả như một điều kiện lỗi (bug condition) độc lập, kèm hành vi hiện tại, hành vi mong đợi và các hành vi cần được bảo toàn (chống hồi quy). Phần thiết kế chi tiết (endpoint mới, thay đổi component, redesign sidebar/layout) sẽ được xử lý ở `design.md` ở pha tiếp theo.

## Bug Analysis

### Current Behavior (Defect)

Hành vi sai hiện tại khi mỗi điều kiện lỗi xảy ra:

1.1 WHEN một admin (SUPER_ADMIN) cần cộng tiền cho người dùng THEN form "Cộng tiền thủ công" chỉ chấp nhận một ô nhập **User ID dạng số** (`type="number"`) và không có cách nào nhập username hoặc email; admin buộc phải tự tra cứu ID dạng số trước.

1.2 WHEN một admin bấm "Xem trước" trên một khóa học trong danh sách "Khóa học chờ duyệt" (chưa xuất bản) THEN hệ thống mở tab mới tới route công khai `/courses/{id}`, trang này chạy dưới ngữ cảnh người học (public token) và hành xử như trang công khai — nội dung khóa học chưa xuất bản không được hiển thị (báo lỗi/không thấy nội dung) và hiển thị các hành động dành cho người học (Đăng nhập/Đăng ký) thay vì chế độ xem trước cho admin.

1.3 WHEN admin điều hướng tới một route con như `/admin/courses/pending` THEN sidebar tô sáng đồng thời cả mục cha "Khóa học" (`/admin/courses`) và mục con "Chờ duyệt" (`/admin/courses/pending`) do logic so khớp active dùng so trùng tiền tố (prefix match).

1.4 WHEN trang "Xác nhận đăng ký" (`Xác nhận đăng ký`) hiển thị mục "Số dư ví" THEN hệ thống hiển thị "NaN đ" vì giá trị số dư được đọc từ nguồn không chứa số dư (giá trị `undefined`), khiến hàm định dạng tiền tệ trả về NaN.

### Expected Behavior (Correct)

Hành vi đúng tương ứng với từng điều kiện lỗi ở trên:

2.1 WHEN một admin (SUPER_ADMIN) cần cộng tiền cho người dùng THEN hệ thống SHALL cung cấp một ô nhập văn bản duy nhất cho phép nhập **username HOẶC email**, phân giải (resolve) ra đúng người dùng trước khi cộng tiền, và báo lỗi rõ ràng nếu không tìm thấy người dùng tương ứng.

2.2 WHEN một admin bấm "Xem trước" trên một khóa học chờ duyệt (chưa xuất bản) THEN hệ thống SHALL hiển thị nội dung khóa học chưa xuất bản bằng quyền của admin/instructor (không bị chặn như anonymous/public), ở chế độ xem trước dành cho admin chứ không phải trang đăng ký công khai.

2.3 WHEN admin điều hướng tới bất kỳ route nào THEN sidebar SHALL chỉ tô sáng đúng MỘT mục — mục khớp cụ thể nhất (most-specific/exact match) với đường dẫn hiện tại.

2.4 WHEN trang "Xác nhận đăng ký" hiển thị mục "Số dư ví" THEN hệ thống SHALL hiển thị số dư ví thực tế của người dùng, định dạng đúng theo VND (ví dụ "200.000 ₫"), đọc từ đúng nguồn dữ liệu số dư.

### Unchanged Behavior (Regression Prevention)

Các hành vi đang đúng cần được bảo toàn sau khi sửa:

3.1 WHEN admin cộng tiền bằng một định danh hợp lệ THEN hệ thống SHALL CONTINUE TO cộng tiền vào đúng ví, giữ ràng buộc quyền chỉ **SUPER_ADMIN** mới được cộng tiền, ghi audit log, và đẩy sự kiện WebSocket `WALLET_UPDATED` realtime tới người dùng được cộng tiền.

3.2 WHEN người học xem một khóa học **đã xuất bản** tại `/courses/{id}` THEN hệ thống SHALL CONTINUE TO hiển thị trang công khai bình thường với các hành động Đăng nhập/Đăng ký/Vào học như hiện tại.

3.3 WHEN admin đang ở một route cấp cao như `/admin` hoặc `/admin/courses` (không có route con cụ thể hơn đang active) THEN sidebar SHALL CONTINUE TO tô sáng đúng mục tương ứng.

3.4 WHEN trang "Xác nhận đăng ký" xử lý mua khóa học THEN hệ thống SHALL CONTINUE TO tính báo giá/voucher (quote), kiểm tra số dư có đủ hay không, và hoàn tất thanh toán/đăng ký thành công.

3.5 WHEN số dư ví được cập nhật qua WebSocket (`WALLET_UPDATED`) hoặc qua làm mới profile THEN hệ thống SHALL CONTINUE TO phản ánh số dư mới nhất một cách nhất quán giữa các trang.

---

## Bug Conditions & Properties

Phần này suy ra điều kiện lỗi `C(X)` và thuộc tính kiểm chứng cho từng lỗi. Quy ước: **F** là hàm/luồng trước khi sửa, **F'** là sau khi sửa.

### BUG 1 — Định danh cộng tiền (username/email thay vì User ID số)

```pascal
FUNCTION isBugCondition_1(X)
  INPUT: X = { identifier }   // định danh người dùng admin muốn cộng tiền
  OUTPUT: boolean

  // Lỗi xảy ra khi admin muốn xác định người dùng bằng username/email
  // nhưng giao diện chỉ cho phép nhập User ID dạng số.
  RETURN isUsernameOrEmail(X.identifier) AND NOT isNumericId(X.identifier)
END FUNCTION
```

```pascal
// Property: Fix Checking — phân giải username/email
FOR ALL X WHERE isBugCondition_1(X) DO
  result ← adminTopUpFlow'(X)
  ASSERT result.resolvedUser = lookupByUsernameOrEmail(X.identifier)
     AND result.creditedCorrectWallet = TRUE
END FOR
```

Bằng chứng tái hiện: `AdminTopUpPage.jsx` dùng ô `Input type="number"` cho `userId` và gọi `adminTopUpApi(form.userId, ...)` → `POST /api/v1/admin/users/{userId}/top-up` (yêu cầu `userId` số). Có sẵn `GET /api/v1/admin/users?keyword=` để tra cứu theo tên/email (quyền ADMIN_USER/SUPER_ADMIN) có thể tái sử dụng để phân giải. Lưu ý phân quyền: cộng tiền là **SUPER_ADMIN-only**, mà SUPER_ADMIN cũng có quyền gọi endpoint tra cứu — nên không phát sinh xung đột quyền.

**Các bước tái hiện:**
1. Đăng nhập Admin portal với tài khoản SUPER_ADMIN.
2. Vào "Cộng tiền" (`/admin/wallet`).
3. Quan sát: chỉ có ô "ID người dùng" dạng số; không thể nhập username/email.

### BUG 2 — Xem trước khóa học chưa xuất bản

```pascal
FUNCTION isBugCondition_2(X)
  INPUT: X = { course, viewerRole, viewerContext }
  OUTPUT: boolean

  // Lỗi xảy ra khi admin xem trước một khóa học CHƯA xuất bản
  // qua route công khai chạy dưới ngữ cảnh người học (public).
  RETURN X.course.published = FALSE
     AND viewerCanPreview(X.viewerRole)        // admin/instructor được phép xem
     AND X.viewerContext = PUBLIC               // nhưng đang ở ngữ cảnh public
END FUNCTION
```

```pascal
// Property: Fix Checking — xem trước với quyền admin
FOR ALL X WHERE isBugCondition_2(X) DO
  result ← previewCourse'(X)
  ASSERT result.showsUnpublishedContent = TRUE
     AND result.usesAdminAuthorization = TRUE
     AND result.behavesAsPublicEnrollPage = FALSE
END FOR
```

Bằng chứng tái hiện: `AdminPendingCoursesPage.jsx` gọi `window.open('/courses/${course.id}', '_blank')`. Tab mới render `CourseDetailPage.jsx` — trang công khai dùng `api` (public token) qua `getCourseByIdApi`. Backend `GET /api/v1/courses/{id}` cho phép owner/admin xem khóa chưa publish **chỉ khi** request mang JWT của admin/owner; nhưng tab công khai dùng `publicToken` (hoặc anonymous) chứ không phải `adminToken`, nên nội dung chưa xuất bản không hiển thị.

**Các bước tái hiện:**
1. Đăng nhập Admin portal (STAFF/SUPER_ADMIN), vào "Chờ duyệt".
2. Bấm "Xem trước" trên một khóa chưa xuất bản (ví dụ course id = 2).
3. Quan sát: tab mở `http://localhost:5173/courses/2` ở chế độ công khai; nội dung khóa chưa xuất bản không hiển thị / báo lỗi, và hiện hành động Đăng nhập/Đăng ký.

### BUG 3 — Sidebar tô sáng hai mục cùng lúc

```pascal
FUNCTION isBugCondition_3(X)
  INPUT: X = { currentPath, navItems }
  OUTPUT: boolean

  // Lỗi xảy ra khi đường dẫn hiện tại là tiền tố con của nhiều mục nav,
  // khiến nhiều hơn một mục được đánh dấu active bằng startsWith().
  count ← COUNT(item IN X.navItems WHERE X.currentPath STARTS_WITH item.url)
  RETURN count > 1
END FUNCTION
```

```pascal
// Property: Fix Checking — chỉ một mục active
FOR ALL X WHERE isBugCondition_3(X) DO
  activeItems ← computeActiveItems'(X)
  ASSERT COUNT(activeItems) = 1
     AND activeItems[0] = mostSpecificMatch(X.currentPath, X.navItems)
END FOR
```

Bằng chứng tái hiện: trong `AdminSidebar.jsx`, `isActive(url)` trả về `location.pathname.startsWith(url)` cho mọi url khác `/admin`. Với path `/admin/courses/pending`, cả `/admin/courses` và `/admin/courses/pending` đều thỏa `startsWith` → hai mục cùng active.

**Các bước tái hiện:**
1. Đăng nhập Admin portal.
2. Điều hướng tới "Chờ duyệt" (`/admin/courses/pending`).
3. Quan sát: cả "Khóa học" và "Chờ duyệt" đều được tô sáng (active).

### BUG 4 — "Số dư ví" hiển thị "NaN đ"

```pascal
FUNCTION isBugCondition_4(X)
  INPUT: X = { page = "PurchaseConfirm", balanceSource }
  OUTPUT: boolean

  // Lỗi xảy ra khi trang xác nhận đọc số dư từ nguồn không chứa số dư
  // → giá trị undefined → định dạng ra "NaN".
  RETURN X.page = "PurchaseConfirm"
     AND readBalance(X.balanceSource) = UNDEFINED
END FUNCTION
```

```pascal
// Property: Fix Checking — hiển thị số dư đúng
FOR ALL X WHERE isBugCondition_4(X) DO
  rendered ← renderWalletBalance'(X)
  ASSERT isNumber(actualBalance(X))
     AND rendered = formatVND(actualBalance(X))
     AND rendered DOES NOT CONTAIN "NaN"
END FOR
```

Bằng chứng tái hiện: `PurchaseConfirmPage.jsx` lấy `const { balance, deduct } = useWalletStore()`, nhưng `useWalletStore` **không** chứa `balance` (cũng không có `deduct`). Số dư thực tế nằm ở `AuthContext` (state `balance`, được nạp từ `GET /api/v1/users/me/profile` trả về `data.balance` và cập nhật qua WebSocket). Vì `balance` là `undefined`, `formatMoney(undefined)` → "NaN đ"; đồng thời `insufficientBalance = undefined < finalPrice` là `false` nên việc mua vẫn tiếp tục được ở phía server.

**Các bước tái hiện:**
1. Đăng nhập website học viên (public).
2. Mở một khóa học và bấm "Đăng ký học" → vào trang "Xác nhận đăng ký".
3. Quan sát: ô "Số dư ví" hiển thị "NaN đ".

---

## Scope (high-level)

Phạm vi sửa ở mức hành vi/giao diện người dùng (chi tiết kỹ thuật và phương án triển khai sẽ ở `design.md`):

- **BUG 1**: Form cộng tiền admin — đổi ô nhập định danh sang username/email + bước phân giải người dùng.
- **BUG 2**: Luồng "Xem trước" khóa chờ duyệt — xem trước trong ngữ cảnh quản trị (có quyền xem khóa chưa xuất bản).
- **BUG 3**: Logic active của sidebar admin — chuyển sang khớp cụ thể nhất; kèm yêu cầu redesign sidebar/layout cho rõ ràng hơn.
- **BUG 4**: Hiển thị số dư trên trang xác nhận mua — đọc số dư từ đúng nguồn (AuthContext).

Yêu cầu mở rộng từ người dùng: thiết kế lại giao diện admin/sidebar để xử lý triệt để vấn đề double-active và bố cục rõ ràng hơn (sẽ chi tiết hóa ở pha thiết kế).
