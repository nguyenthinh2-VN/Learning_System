import { Badge } from '@/components/ui/badge';
import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';

export const getSourceLabels = (t) => ({
  MOCK: t('ui.transactions.source_mock'),
  VIETQR: t('ui.transactions.source_vietqr'),
  ADMIN: t('ui.transactions.source_admin'),
  PURCHASE: t('ui.transactions.source_purchase'),
});

export const getStatusLabels = (t) => ({
  COMPLETED: t('ui.transactions.status_completed'),
  PENDING: t('ui.transactions.status_pending'),
  EXPIRED: t('ui.transactions.status_expired'),
  FAILED: t('ui.transactions.status_failed'),
});

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

export const getTransactionColumns = (t) => [
  {
    accessorKey: 'createdAt',
    header: t('ui.transactions.col_time'),
    cell: ({ row }) => (
      <span className="text-sm text-muted-foreground">{fmtDate(row.original.createdAt)}</span>
    ),
  },
  {
    accessorKey: 'source',
    header: t('ui.transactions.col_type'),
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
            {getSourceLabels(t)[row.original.source] ?? row.original.source}
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
    header: t('ui.transactions.col_status'),
    cell: ({ row }) => (
      <Badge
        variant={STATUS_VARIANT[row.original.status] ?? 'secondary'}
        className={STATUS_CLASS[row.original.status]}
      >
        {getStatusLabels(t)[row.original.status] ?? row.original.status}
      </Badge>
    ),
  },
  {
    accessorKey: 'amount',
    header: () => <div className="text-right">{t('ui.transactions.col_amount')}</div>,
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

// ─── Cột phụ tái dùng ──────────────────────────────────────
const getUserColumn = (t) => ({
  accessorKey: 'username',
  header: t('ui.transactions.col_user'),
  cell: ({ row }) => (
    <div className="min-w-0">
      <p className="text-sm font-medium truncate">{row.original.username ?? '—'}</p>
      <p className="text-xs text-muted-foreground truncate">{row.original.email ?? ''}</p>
    </div>
  ),
});

const getRefColumn = (t) => ({
  accessorKey: 'referenceCode',
  header: t('ui.transactions.col_ref'),
  cell: ({ row }) => (
    <code className="text-xs font-mono text-muted-foreground">{row.original.referenceCode}</code>
  ),
});

/**
 * Biến thể cột cho trang admin xem giao dịch toàn hệ thống:
 * Thời gian · Người dùng · Loại · Trạng thái · Mã tham chiếu · Số tiền.
 */
export const getAdminTransactionColumns = (t) => [
  getTransactionColumns(t)[0],
  getUserColumn(t),
  getTransactionColumns(t)[1],
  getTransactionColumns(t)[2],
  getRefColumn(t),
  getTransactionColumns(t)[3],
];
