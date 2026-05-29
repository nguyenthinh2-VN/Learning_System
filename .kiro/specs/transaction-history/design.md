# Design Document — Transaction History (Lịch sử giao dịch)

## Overview

Tính năng bổ sung khả năng **xem lịch sử giao dịch ví** cho user đã đăng nhập, hiển thị cả tiền vào (nạp tiền) và tiền ra (mua khóa học). Thiết kế tuân thủ Clean Architecture 4 tầng của dự án (`domain → application → adapter/infrastructure`), tái dùng tối đa hạ tầng đã có (`WalletTransaction`, `PageResult`, `ApiResponse`, pattern `/me/*`).

Ba khối công việc:

1. **List API** — `GET /api/v1/users/me/transactions` trả về giao dịch của chính người gọi, phân trang, mới nhất trước.
2. **Purchase recording** — thêm `TxSource.PURCHASE` và ghi một `WalletTransaction` (`status = COMPLETED`, `amount = paidPrice`) khi mua khóa học có `paidPrice > 0`, bên trong transaction checkout sẵn có.
3. **FE UI** — thay placeholder tĩnh trên `WalletPage` bằng danh sách giao dịch thật, có loading/empty/error, tải thêm, và refresh khi nhận WebSocket `WALLET_UPDATED`.

### Quyết định thiết kế chính (rationale)

- **Tái dùng bảng `wallet_transactions` thay vì tạo bảng mới.** Bảng đã có `userId`, `amount`, `status`, `source`, `note`, `createdAt`, `completedAt`, index `idx_wallet_tx_user`. Thêm giá trị enum `PURCHASE` đủ để biểu diễn tiền ra. Không cần migration cột mới (cột `source` đã là `VARCHAR(16)` `EnumType.STRING`).
- **`direction` là trường suy diễn (derived), không lưu DB.** `CREDIT` cho `{MOCK, VIETQR, ADMIN}`, `DEBIT` cho `{PURCHASE}`. Giữ schema tối giản; nếu sau này có giao dịch refund/withdraw thì mở rộng map.
- **Ghi purchase transaction trong CÙNG `@Transactional` của checkout.** Bảo đảm nguyên tử: rollback checkout ⇒ không có bản ghi giao dịch mồ côi (Req 3.4, 3.7).
- **`amount` luôn dương trong DB và response.** Hướng tiền biểu diễn bằng `direction`, không bằng dấu (Req 2.4). `WalletTransaction.createCompleted` đã validate `amount > 0` nên không cần đổi.
- **`expiredAt` của purchase tx đặt xa tương lai.** Cột `expired_at` là `NOT NULL`; tái dùng quy ước của `createCompleted` (`now + 100 năm`) để không phải sửa schema.

---

## Architecture

### Luồng List API

```
GET /api/v1/users/me/transactions?page&size
  → UserController.getMyTransactions (adapter/controller)
    → userId = JWT claim "userId"
    → GetMyTransactionsUseCase.execute(userId, page, size)   (application/usecase/Wallet)
        → validate page/size
        → WalletTransactionRepository.findByUserId(userId, page, size)  (port)
            → WalletTransactionRepositoryImpl (adapter) → JpaWalletTransactionRepository.findByUserId(.., Pageable sort createdAt DESC)
            → map JpaEntity → domain WalletTransaction → PageResult<WalletTransaction>
        → map → PageResult<TransactionItemOutput> (direction suy ra từ source)
    → PageResult<TransactionItemResponse> (adapter map)
  → ApiResponse<PageResult<TransactionItemResponse>>
```

### Luồng Purchase recording (chèn vào checkout có sẵn)

```
POST /api/v1/courses/{id}/purchase
  → ApplyVoucherCheckoutUseCase.execute (@Transactional)   [KHÔNG đổi chữ ký]
      processWithoutVoucher / processWithVoucher:
        ... deductBalance, course.enroll, save enrollment, (voucher usage), ledger log ...
        + walletTransactionRepository.save(WalletTransaction.createPurchase(userId, paidPrice, note))   ← THÊM
      processInternal:
        paidPrice = 0 → KHÔNG ghi transaction   (Req 3.3)
```

Toàn bộ nằm trong transaction sẵn có; không thêm `@Transactional` mới, không đổi thứ tự lock (User → Course → Voucher).

---

## Components and Interfaces

### Domain Layer

#### `TxSource` (sửa — thêm enum value)
```java
public enum TxSource {
    MOCK,
    VIETQR,
    ADMIN,
    PURCHASE   // ← mới: giao dịch tiền ra khi mua khóa học
}
```

#### `WalletTransaction` (sửa — thêm factory `createPurchase`)
Thêm một factory method thuần domain, song song với `createCompleted`:
```java
/** Tạo completed transaction cho giao dịch mua khóa học (tiền ra khỏi ví). */
public static WalletTransaction createPurchase(Long userId, BigDecimal amount, String note) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Số tiền mua phải lớn hơn 0");
    }
    WalletTransaction tx = new WalletTransaction();
    tx.userId = userId;
    tx.referenceCode = generateReferenceCode();   // tái dùng, prefix "NAP" — xem ghi chú dưới
    tx.amount = amount;
    tx.status = TxStatus.COMPLETED;
    tx.source = TxSource.PURCHASE;
    tx.note = note;
    tx.createdAt = LocalDateTime.now();
    tx.completedAt = LocalDateTime.now();
    tx.expiredAt = LocalDateTime.now().plusYears(100);
    return tx;
}
```

> **Ghi chú `referenceCode`:** Hiện `generateReferenceCode()` sinh prefix `"NAP"`. Để mã purchase dễ phân biệt khi đối soát, design dùng prefix `"BUY"` cho giao dịch mua. Thực hiện bằng cách thêm một biến thể nội bộ `generateReferenceCode(String prefix)` (giữ method cũ gọi vào nó với `"NAP"`), tránh đụng các call site khác. Đây là thay đổi nội bộ domain, không ảnh hưởng hành vi top-up.

### Application Layer

#### Port: `WalletTransactionRepository` (sửa — thêm method)
```java
public interface WalletTransactionRepository {
    WalletTransaction save(WalletTransaction tx);
    Optional<WalletTransaction> findByReferenceCode(String referenceCode);
    Optional<WalletTransaction> findPendingByRefForUpdate(String referenceCode);
    // ← mới
    PageResult<WalletTransaction> findByUserId(Long userId, int page, int size);
}
```

#### DTO: `TransactionItemOutput` (mới, `application/dto/Wallet/`)
```java
public record TransactionItemOutput(
        Long id,
        String referenceCode,
        BigDecimal amount,        // luôn > 0
        String direction,         // "CREDIT" | "DEBIT"
        TxStatus status,
        TxSource source,
        String note,              // nullable
        LocalDateTime createdAt,
        LocalDateTime completedAt // nullable
) {
    public static TransactionItemOutput from(WalletTransaction tx) {
        String direction = (tx.getSource() == TxSource.PURCHASE) ? "DEBIT" : "CREDIT";
        return new TransactionItemOutput(
                tx.getId(), tx.getReferenceCode(), tx.getAmount(), direction,
                tx.getStatus(), tx.getSource(), tx.getNote(),
                tx.getCreatedAt(), tx.getCompletedAt());
    }
}
```

> Logic suy `direction` đặt trong `from(...)` (application) chứ không trong domain, vì đây là khái niệm hiển thị. Nếu muốn test riêng, tách thành `TxSource.isCredit()` — nhưng để tối giản, giữ trong `from`.

#### UseCase: `GetMyTransactionsUseCase` (mới, `application/usecase/Wallet/`)
```java
@Service
@RequiredArgsConstructor
public class GetMyTransactionsUseCase {
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public PageResult<TransactionItemOutput> execute(Long userId, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page phải >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size phải trong khoảng [1, 100]");
        return walletTransactionRepository.findByUserId(userId, page, size)
                .map(TransactionItemOutput::from);
    }
}
```

#### `ApplyVoucherCheckoutUseCase` (sửa — inject repo + ghi tx)
- Thêm dependency `private final WalletTransactionRepository walletTransactionRepository;` (constructor qua `@RequiredArgsConstructor`).
- Trong `processWithoutVoucher` và `processWithVoucher`, sau khi `enrollmentRepository.save(...)` và trước/sau ledger log, thêm:
  ```java
  if (paidPrice.signum() > 0) {
      walletTransactionRepository.save(
          WalletTransaction.createPurchase(input.requesterId(), paidPrice,
              "Mua khóa học #" + input.courseId()));
  }
  ```
- `processInternal` KHÔNG ghi (paidPrice = 0).
- Không đổi giá trị trả về `PurchaseCourseOutput`, không đổi thứ tự lock.

### Adapter Layer

#### `JpaWalletTransactionRepository` (sửa — thêm query method)
```java
Page<WalletTransactionJpaEntity> findByUserId(Long userId, Pageable pageable);
```

#### `WalletTransactionRepositoryImpl` (sửa — implement `findByUserId`)
```java
@Override
public PageResult<WalletTransaction> findByUserId(Long userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<WalletTransactionJpaEntity> jpaPage = jpaRepo.findByUserId(userId, pageable);
    List<WalletTransaction> items = jpaPage.getContent().stream()
            .map(WalletTransactionJpaEntity::toDomain).toList();
    return PageResult.of(jpaPage.getTotalElements(), jpaPage.getTotalPages(), page, size, items);
}
```
(Mirror đúng pattern của `EnrollmentRepositoryImpl.findByUserId`.)

#### DTO: `TransactionItemResponse` (mới, `adapter/dto/response/`)
```java
public record TransactionItemResponse(
        Long id,
        String referenceCode,
        BigDecimal amount,
        String direction,
        String status,
        String source,
        String note,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TransactionItemResponse from(TransactionItemOutput o) {
        return new TransactionItemResponse(
                o.id(), o.referenceCode(), o.amount(), o.direction(),
                o.status().name(), o.source().name(), o.note(),
                o.createdAt(), o.completedAt());
    }
}
```
(`status`/`source` serialize thành String để hợp đồng API ổn định, không lộ enum nội bộ — Req 2.3.)

#### `UserController` (sửa — thêm endpoint)
```java
@GetMapping("/me/transactions")
public ResponseEntity<ApiResponse<PageResult<TransactionItemResponse>>> getMyTransactions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest request) {
    Long requesterId = getUserId(request);
    PageResult<TransactionItemOutput> result = getMyTransactionsUseCase.execute(requesterId, page, size);
    return ResponseEntity.ok(ApiResponse.success(result.map(TransactionItemResponse::from)));
}
```
Inject `private final GetMyTransactionsUseCase getMyTransactionsUseCase;`. Đặt ở `UserController` (đã quản lý `/me/*`) để nhất quán; không tạo controller mới.

> **Lưu ý security:** `/api/v1/users/me/**` đã thuộc nhánh `anyRequest().authenticated()` trong `SecurityConfig`, nên không cần thêm rule. Token thiếu → 401 qua `CustomAuthenticationEntryPoint` (Req 4.1).

### Infrastructure Layer

Không cần thay đổi `SecurityConfig`, `GlobalExceptionHandler`, hay `ErrorCode`:
- `page/size` không hợp lệ → `IllegalArgumentException` → handler hiện có trả `400 BAD_REQUEST` (Req 1.3).
- Không phát sinh mã lỗi mới.

---

## Data Models

### Bảng `wallet_transactions` (không đổi schema)
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | BIGINT PK | |
| user_id | BIGINT, index `idx_wallet_tx_user` | dùng cho `findByUserId` |
| reference_code | VARCHAR(32) UNIQUE | `NAP...` (nạp) / `BUY...` (mua) |
| amount | DECIMAL(15,2) | luôn > 0 |
| status | VARCHAR(16) | PENDING/COMPLETED/EXPIRED/FAILED |
| source | VARCHAR(16) | MOCK/VIETQR/ADMIN/**PURCHASE** |
| note | VARCHAR(255) | nullable; mô tả khóa học cho PURCHASE |
| created_at | DATETIME | sort key DESC |
| completed_at | DATETIME nullable | |
| expired_at | DATETIME NOT NULL | purchase: now + 100y |

> Với `ddl-auto: update`, thêm enum value `PURCHASE` không cần thay đổi cột (vẫn là String length 16). Không có migration thủ công.

### Response shape (client)
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalElements": 3, "totalPages": 1, "page": 0, "size": 20,
    "items": [
      {
        "id": 12, "referenceCode": "BUY1A2B3C4D5",
        "amount": 300000.00, "direction": "DEBIT",
        "status": "COMPLETED", "source": "PURCHASE",
        "note": "Mua khóa học #5",
        "createdAt": "2026-05-29T10:05:00", "completedAt": "2026-05-29T10:05:00"
      },
      {
        "id": 8, "referenceCode": "NAP4F8A2C1B3",
        "amount": 500000.00, "direction": "CREDIT",
        "status": "COMPLETED", "source": "MOCK",
        "note": null,
        "createdAt": "2026-05-28T09:00:00", "completedAt": "2026-05-28T09:01:00"
      }
    ]
  },
  "timestamp": "2026-05-29T12:00:00"
}
```

---

## Frontend Design

FE dùng **shadcn DataTable** (xây trên `@tanstack/react-table`) để có sẵn cấu trúc cột, render header/cell và phân trang gọn. Vì lịch sử giao dịch chỉ cần bảng + phân trang server-side (không cần ẩn/hiện cột, không cần lọc client), ta dùng **bản DataTable rút gọn** — KHÔNG kéo theo `dropdown-menu` của Radix (dự án dùng `@base-ui/react`, không có Radix).

### Phụ thuộc & primitives cần thêm

1. **Cài `@tanstack/react-table`** (chưa có trong `package.json`):
   ```bash
   npm install @tanstack/react-table
   ```
2. **Thêm primitive `table.jsx`** vào `fe/src/components/ui/` (chưa có). Đây là bộ wrapper thuần JSX + Tailwind của shadcn (`Table`, `TableHeader`, `TableBody`, `TableRow`, `TableHead`, `TableCell`, `TableCaption`) — không phụ thuộc Radix/base-ui, chỉ là `<table>` styled. Dán nguyên bản shadcn (đổi sang `.jsx`, bỏ type).
3. Tái dùng `Button`, `Badge`, `Skeleton` đã có cho thanh phân trang, badge trạng thái và loading.

> Lý do không dùng full DataTable mặc định của shadcn: bản đầy đủ có toolbar ẩn/hiện cột bằng `DropdownMenu` (Radix). Dự án này dùng `@base-ui/react` nên thêm Radix sẽ lệch convention. Ta chỉ lấy phần lõi: `useReactTable` + `flexRender` + primitive `table`.

### `fe/src/api/wallet.js` (thêm hàm)
```js
/**
 * Lịch sử giao dịch của chính mình (phân trang)
 * GET /api/v1/users/me/transactions?page&size
 */
export const getTransactionsApi = (page = 0, size = 20) =>
  api.get('/users/me/transactions', { params: { page, size } });
```

### Component `DataTable` dùng chung (mới: `fe/src/components/ui/data-table.jsx`)

Bản rút gọn, server-side pagination (table chỉ render đúng trang hiện tại do BE trả về):

```jsx
import { flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';

export function DataTable({ columns, data }) {
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true, // phân trang do server lo
  });

  return (
    <div className="rounded-xl border overflow-hidden">
      <Table>
        <TableHeader>
          {table.getHeaderGroups().map((hg) => (
            <TableRow key={hg.id}>
              {hg.headers.map((h) => (
                <TableHead key={h.id}>
                  {h.isPlaceholder ? null : flexRender(h.column.columnDef.header, h.getContext())}
                </TableHead>
              ))}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows.length ? (
            table.getRowModel().rows.map((row) => (
              <TableRow key={row.id}>
                {row.getVisibleCells().map((cell) => (
                  <TableCell key={cell.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </TableCell>
                ))}
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                Chưa có giao dịch nào.
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}
```

### Định nghĩa cột giao dịch (`WalletPage` cục bộ hoặc `fe/src/features/wallet/transactionColumns.jsx`)

Cột hiển thị: **Thời gian · Loại/Nguồn · Trạng thái · Số tiền (±, có màu)**.

```jsx
import { Badge } from '@/components/ui/badge';
import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';

const SOURCE_LABELS = {
  MOCK: 'Nạp tiền (Mock)', VIETQR: 'Nạp qua QR',
  ADMIN: 'Admin cộng tiền', PURCHASE: 'Mua khóa học',
};
const STATUS_LABELS = {
  COMPLETED: 'Thành công', PENDING: 'Đang xử lý',
  EXPIRED: 'Hết hạn', FAILED: 'Thất bại',
};
const STATUS_VARIANT = { // map sang variant của Badge hiện có (default/secondary/destructive/outline/ghost)
  COMPLETED: 'secondary', PENDING: 'secondary',
  EXPIRED: 'outline', FAILED: 'destructive',
};
// COMPLETED tô xanh bằng className thêm (Badge chưa có variant "success"):
const STATUS_CLASS = {
  COMPLETED: 'bg-emerald-100 text-emerald-700',
};
```

> `Badge` của dự án (`@base-ui/react`) chỉ có variant `default/secondary/destructive/outline/ghost/link`, KHÔNG có `success`. Vì vậy với `COMPLETED` ta dùng `variant="secondary"` kèm `className={STATUS_CLASS.COMPLETED}` để tô xanh, không cần sửa component dùng chung.

const fmtMoney = (a) => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
}).format(a ?? 0);
const fmtDate = (s) => new Date(s).toLocaleString('vi-VN');

export const transactionColumns = [
  {
    accessorKey: 'createdAt',
    header: 'Thời gian',
    cell: ({ row }) => <span className="text-sm">{fmtDate(row.original.createdAt)}</span>,
  },
  {
    accessorKey: 'source',
    header: 'Loại giao dịch',
    cell: ({ row }) => {
      const isCredit = row.original.direction === 'CREDIT';
      const Icon = isCredit ? ArrowDownLeft : ArrowUpRight;
      return (
        <div className="flex items-center gap-2">
          <Icon className={`size-4 ${isCredit ? 'text-emerald-500' : 'text-red-500'}`} />
          <span className="text-sm">{SOURCE_LABELS[row.original.source] ?? row.original.source}</span>
          {row.original.note && (
            <span className="text-xs text-muted-foreground truncate max-w-[160px]">· {row.original.note}</span>
          )}
        </div>
      );
    },
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => (
      <Badge variant={STATUS_VARIANT[row.original.status] ?? 'secondary'}
        className={STATUS_CLASS[row.original.status]}>
        {STATUS_LABELS[row.original.status] ?? row.original.status}
      </Badge>
    ),
  },
  {
    accessorKey: 'amount',
    header: () => <div className="text-right">Số tiền</div>,
    cell: ({ row }) => {
      const isCredit = row.original.direction === 'CREDIT';
      return (
        <div className={`text-right font-semibold ${isCredit ? 'text-emerald-600' : 'text-red-600'}`}>
          {isCredit ? '+' : '−'}{fmtMoney(row.original.amount)}
        </div>
      );
    },
  },
];
```

> Nếu `Badge` hiện tại chưa có variant `success`, dùng `secondary` + class màu xanh inline để tránh sửa component dùng chung. Quyết định cuối ở bước task.

### `fe/src/pages/WalletPage.jsx` (thay placeholder bằng DataTable)

State + tải dữ liệu (server-side pagination):
```jsx
const [txData, setTxData] = useState({ items: [], totalPages: 0, totalElements: 0, page: 0 });
const [txLoading, setTxLoading] = useState(true);
const [txError, setTxError] = useState('');
const [txPage, setTxPage] = useState(0);
const PAGE_SIZE = 10;

const loadTransactions = async (page) => {
  setTxLoading(true); setTxError('');
  try {
    const res = await getTransactionsApi(page, PAGE_SIZE);
    setTxData(res.data.data);
  } catch (e) {
    setTxError(e?.response?.data?.message || 'Không tải được lịch sử giao dịch.');
  } finally { setTxLoading(false); }
};

useEffect(() => { loadTransactions(txPage); }, [txPage]);
// Refresh trang đầu khi balance đổi (WebSocket WALLET_UPDATED qua AuthContext)
useEffect(() => { if (balance !== null) { setTxPage(0); loadTransactions(0); } }, [balance]);
```

Render khối "Lịch sử giao dịch":
- **Loading** (`txLoading`): vài dòng `Skeleton` (hoặc render DataTable với data rỗng + overlay skeleton).
- **Error** (`txError`): thông báo đỏ + nút "Thử lại" gọi `loadTransactions(txPage)`.
- **Bình thường**: `<DataTable columns={transactionColumns} data={txData.items} />`.
- **Empty**: DataTable tự hiển thị dòng "Chưa có giao dịch nào." (đã có trong component).
- **Phân trang dưới bảng** (chỉ khi `totalPages > 1`):
  ```jsx
  <div className="flex items-center justify-between pt-3">
    <span className="text-xs text-muted-foreground">
      Trang {txData.page + 1}/{txData.totalPages} · {txData.totalElements} giao dịch
    </span>
    <div className="flex gap-2">
      <Button variant="outline" size="sm" disabled={txPage === 0}
        onClick={() => setTxPage((p) => p - 1)}>Trước</Button>
      <Button variant="outline" size="sm" disabled={txData.page + 1 >= txData.totalPages}
        onClick={() => setTxPage((p) => p + 1)}>Sau</Button>
    </div>
  </div>
  ```

> Dùng phân trang Trước/Sau (server-side) thay vì "Tải thêm" để khớp tự nhiên với `manualPagination` của react-table và `PageResult` của BE. Đây là điều chỉnh so với mô tả "Tải thêm" ở requirements — vẫn thỏa Req 6.6 (hỗ trợ xem trang tiếp theo khi `totalPages > 1`).



Map nhãn nguồn:
```js
const SOURCE_LABELS = {
  MOCK: 'Nạp tiền (Mock)',
  VIETQR: 'Nạp qua QR',
  ADMIN: 'Admin cộng tiền',
  PURCHASE: 'Mua khóa học',
};
const STATUS_LABELS = {
  COMPLETED: 'Thành công', PENDING: 'Đang xử lý',
  EXPIRED: 'Hết hạn', FAILED: 'Thất bại',
};
```

---

## Error Handling

| Tình huống | Cơ chế | HTTP | Mã |
|------------|--------|------|-----|
| `page < 0` / `size` ngoài [1,100] | `IllegalArgumentException` → handler có sẵn | 400 | `BAD_REQUEST` |
| Token thiếu/không hợp lệ | `CustomAuthenticationEntryPoint` | 401 | (entry point) |
| Người gọi chưa có giao dịch | Trả trang rỗng | 200 | — |
| Ghi purchase tx lỗi kỹ thuật | Exception lan ra → rollback cả checkout | 500 | `INTERNAL_ERROR` |

Không thêm `ErrorCode` mới. Không dùng try/catch trong controller/usecase (đúng rule dự án).

---

## Testing Strategy

### Backend (JUnit + Mockito, `mvnw test`)
1. **`GetMyTransactionsUseCaseTest`**
   - Rỗng: repo trả `PageResult` rỗng → output `items = []`.
   - Một trang: map đúng các field, `direction` đúng theo `source`.
   - Nhiều trang: `totalPages > 1`, verify `page/size` truyền xuống repo.
   - `size = 0` và `size = 101` → `IllegalArgumentException`.
   - `page = -1` → `IllegalArgumentException`.
   - Verify repo được gọi với đúng `userId` truyền vào (Req 4.3).
2. **`TransactionItemOutput` direction** (có thể gộp trong test trên hoặc test riêng): `MOCK/VIETQR/ADMIN → CREDIT`, `PURCHASE → DEBIT` (Req 8.3).
3. **`ApplyVoucherCheckoutUseCaseTest`** (mở rộng test hiện có):
   - Mua không voucher `paidPrice > 0` → verify `walletTransactionRepository.save(...)` được gọi đúng 1 lần với `source = PURCHASE`, `amount = paidPrice`.
   - Mua có voucher `finalPrice > 0` → verify save tx với `amount = finalPrice`.
   - Internal member (`paidPrice = 0`) → verify `walletTransactionRepository.save(...)` KHÔNG được gọi (Req 3.3).
   - Thêm mock `WalletTransactionRepository` vào setup test hiện có.
4. **`WalletTransactionTest`** (domain, mở rộng): `createPurchase` set đúng `source/status/amount`; ném `IllegalArgumentException` khi `amount <= 0`.

### Frontend (thủ công / smoke)
- Tải `WalletPage`: thấy loading → danh sách; cuộn "Tải thêm".
- Nạp tiền (mock) rồi kích hoạt webhook → balance đổi → danh sách tự thêm dòng CREDIT.
- Mua khóa học → quay lại Ví → thấy dòng DEBIT.
- Tài khoản chưa giao dịch → thấy empty state.

---

## Tài liệu cần cập nhật

- `docs/API/wallet.md`: thêm mục `GET /api/v1/users/me/transactions` (params, response thành công, response rỗng, bảng field, giá trị `direction`/`source`/`status`).
- `docs/API/user.md`: thêm dòng `/me/transactions` vào bảng tóm tắt `/me/*`.
- `docs/API/endpoints-summary.md`: liệt kê endpoint mới trong nhóm Wallet.

---

## Correctness Properties

Các bất biến (invariant) phải luôn đúng, dùng làm cơ sở cho test và review:

### Property 1: Ownership isolation
`GetMyTransactionsUseCase` chỉ trả về giao dịch có `userId` bằng `userId` lấy từ JWT. Với mọi input client (kể cả query/body chứa `userId` khác), kết quả KHÔNG bao giờ chứa giao dịch của user khác.

**Validates: Requirements 1.7, 4.3**

### Property 2: Amount dương + direction nhất quán
Mọi `TransactionItemOutput.amount > 0`. `direction = DEBIT ⟺ source = PURCHASE`; `direction = CREDIT ⟺ source ∈ {MOCK, VIETQR, ADMIN}`. Hàm suy diễn là toàn phần (total) trên tập `TxSource`.

**Validates: Requirements 2.2, 2.4**

### Property 3: Sắp xếp ổn định
Kết quả luôn theo `createdAt` giảm dần; với cùng state DB, hai lần gọi cùng `(userId, page, size)` trả về cùng thứ tự và cùng nội dung.

**Validates: Requirements 1.4**

### Property 4: Phân trang hợp lệ
`0 ≤ page` và `1 ≤ size ≤ 100` là điều kiện cần để UseCase trả kết quả; ngoài khoảng ⇒ `IllegalArgumentException` (không trả dữ liệu một phần).

**Validates: Requirements 1.2, 1.3**

### Property 5: Nguyên tử hóa ghi purchase
Một lần checkout thành công với `paidPrice > 0` tạo **đúng một** `WalletTransaction` (`source = PURCHASE`, `status = COMPLETED`, `amount = paidPrice`). Checkout với `paidPrice = 0` tạo **không** bản ghi nào. Checkout rollback ⇒ **không** bản ghi nào tồn tại.

**Validates: Requirements 3.2, 3.3, 3.4, 3.7**

### Property 6: Không tác dụng phụ lên dòng tiền
Việc ghi purchase transaction KHÔNG thay đổi `paidPrice`, số dư ví, `PurchaseCourseOutput`, hay thứ tự lock của checkout. Tổng tiền bị trừ khỏi ví bằng `amount` của bản ghi PURCHASE tương ứng.

**Validates: Requirements 3.6**

### Property 7: Read-only của list
`GetMyTransactionsUseCase` không ghi vào bất kỳ repository nào; gọi nhiều lần không đổi state.

**Validates: Requirements 1.8**

---

## Phạm vi & Không thuộc phạm vi

**Trong phạm vi:** list API của chính mình, ghi purchase tx (paidPrice > 0), UI lịch sử, docs, tests.

**Ngoài phạm vi (cố ý):**
- ❌ Endpoint admin xem giao dịch của user khác.
- ❌ Lọc/tìm kiếm giao dịch theo `source`/`status`/khoảng thời gian (có thể thêm sau).
- ❌ Bù ghi (backfill) giao dịch mua cho các enrollment đã tồn tại trước khi tính năng ra mắt — lịch sử chỉ ghi từ thời điểm triển khai trở đi.
- ❌ Ghi transaction cho top-up legacy `POST /me/top-up` (luồng cũ không qua gateway) — giữ nguyên hành vi hiện tại.
