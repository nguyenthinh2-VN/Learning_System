import { Badge } from '@/components/ui/badge';
import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';

export const SOURCE_LABELS = {
  MOCK: 'Nạp tiền (Mock)',
  VIETQR: 'Nạp qua QR',
  ADMIN: 'Admin cộng tiền',
  PURCHASE: 'Mua khóa học',
};

export const STATUS_LABELS = {
  COMPLETED: 'Thành công',
  PENDING: 'Đang xử lý',
  EXPIRED: 'Hết hạn',
  FAILED: 'Thất bại',
};

// Badge của dự án không có variant "success" → COMPLETED dùng secondary + class xanh.
const STATUS_VARIANT = {
  COMPLETED: 'secondary',
  PENDING: 'secondary',
  EXPIRED: 'outline',
  FAILED: 'destructive',
};
const STATUS_CLASS = {
  COMPLETED: 'bg-emerald-100 text-emerald-700',
};

const fmtMoney = (a) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(a ?? 0);

const fmtDate = (s) => {
  if (!s) return '—';
  return new Date(s).toLocaleString('vi-VN');
};

export const transactionColumns = [
  {
    accessorKey: 'createdAt',
    header: 'Thời gian',
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{fmtDate(row.original.createdAt)}</span>
    ),
  },
  {
    accessorKey: 'source',
    header: 'Loại giao dịch',
    cell: ({ row }) => {
      const isCredit = row.original.direction === 'CREDIT';
      const Icon = isCredit ? ArrowDownLeft : ArrowUpRight;
      return (
        <div className="flex items-center gap-2 min-w-0">
          <span
            className={`flex size-7 items-center justify-center rounded-full shrink-0 ${
              isCredit ? 'bg-emerald-50' : 'bg-red-50'
            }`}
          >
            <Icon className={`size-4 ${isCredit ? 'text-emerald-500' : 'text-red-500'}`} />
          </span>
          <span className="text-sm font-medium">
            {SOURCE_LABELS[row.original.source] ?? row.original.source}
          </span>
          {row.original.note && (
            <span className="text-xs text-muted-foreground truncate max-w-[160px]">
              · {row.original.note}
            </span>
          )}
        </div>
      );
    },
  },
  {
    accessorKey: 'status',
    header: 'Trạng thái',
    cell: ({ row }) => (
      <Badge
        variant={STATUS_VARIANT[row.original.status] ?? 'secondary'}
        className={STATUS_CLASS[row.original.status]}
      >
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
          {isCredit ? '+' : '−'}
          {fmtMoney(row.original.amount)}
        </div>
      );
    },
  },
];
