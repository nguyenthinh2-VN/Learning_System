import React, { useState, useEffect, useCallback, useRef } from 'react';
import { getAdminTransactionsApi } from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { DataTable } from '@/components/ui/data-table';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  Search, RefreshCw, Loader2, AlertCircle, ChevronLeft, ChevronRight, Receipt, X,
} from 'lucide-react';
import { getRoleTextClass } from '@/lib/roleColors';
import { getAdminTransactionColumns, getSourceLabels, getStatusLabels } from '@/features/wallet/transactionColumns';
import { useTranslation } from 'react-i18next';




const PAGE_SIZE = 20;

const selectClass =
  'rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring';

export default function AdminTransactionsPage() {
  const { t } = useTranslation();
  const adminTransactionColumns = React.useMemo(() => getAdminTransactionColumns(t), [t]);
  const SOURCE_OPTIONS = React.useMemo(() => Object.entries(getSourceLabels(t)), [t]);
  const STATUS_OPTIONS = React.useMemo(() => Object.entries(getStatusLabels(t)), [t]);
  const DIRECTION_OPTIONS = React.useMemo(() => [
    ['CREDIT', t('ui.admin_transactions.credit')],
    ['DEBIT', t('ui.admin_transactions.debit')],
  ], [t]);
  const { adminUser } = useAuth();

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Bộ lọc
  const [keyword, setKeyword] = useState('');
  const [source, setSource] = useState('');
  const [status, setStatus] = useState('');
  const [direction, setDirection] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const debounceRef = useRef(null);

  const fetchData = useCallback(async (pageNum) => {
    setLoading(true);
    setError('');
    try {
      const params = { page: pageNum, size: PAGE_SIZE };
      if (keyword.trim()) params.keyword = keyword.trim();
      if (source) params.source = source;
      if (status) params.status = status;
      if (direction) params.direction = direction;
      if (from) params.from = from;
      if (to) params.to = to;

      const res = await getAdminTransactionsApi(params);
      const data = res.data.data;
      setRows(data?.items ?? []);
      setTotalPages(data?.totalPages ?? 0);
      setTotalElements(data?.totalElements ?? 0);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_transactions.err_load'));
    } finally {
      setLoading(false);
    }
  }, [keyword, source, status, direction, from, to]);

  // Lọc đổi → debounce, reset về trang 0
  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      fetchData(0);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [fetchData]);

  // Đổi trang
  useEffect(() => {
    fetchData(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const hasFilter = keyword || source || status || direction || from || to;
  const clearFilters = () => {
    setKeyword(''); setSource(''); setStatus(''); setDirection(''); setFrom(''); setTo('');
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <Receipt className="size-4 text-muted-foreground" />
        <span className="text-sm font-medium">{t('ui.admin_transactions.title')}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">{t('ui.admin_transactions.heading')}</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {totalElements > 0 ? t('ui.admin_transactions.count_transactions').replace('{count}', totalElements) : t('ui.admin_transactions.subtitle')}
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={() => fetchData(page)} disabled={loading}>
            <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />Làm mới
          </Button>
        </div>

        {/* Bộ lọc */}
        <div className="space-y-3">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              placeholder={t('ui.admin_transactions.search_placeholder')}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="pl-9"
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <select value={source} onChange={(e) => setSource(e.target.value)} className={selectClass}>
              <option value="">{t('ui.admin_transactions.all_types')}</option>
              {SOURCE_OPTIONS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
            </select>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectClass}>
              <option value="">{t('ui.admin_transactions.all_statuses')}</option>
              {STATUS_OPTIONS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
            </select>
            <select value={direction} onChange={(e) => setDirection(e.target.value)} className={selectClass}>
              <option value="">{t('ui.admin_transactions.in_out')}</option>
              {DIRECTION_OPTIONS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
            </select>
            <div className="flex items-center gap-1.5 text-sm">
              <span className="text-muted-foreground text-xs">{t('ui.admin_transactions.from')}</span>
              <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="w-auto" />
              <span className="text-muted-foreground text-xs">{t('ui.admin_transactions.to')}</span>
              <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="w-auto" />
            </div>
            {hasFilter && (
              <Button variant="ghost" size="sm" onClick={clearFilters} className="text-muted-foreground">
                <X className="size-4 mr-1" />Xóa lọc
              </Button>
            )}
          </div>
        </div>

        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {/* Bảng */}
        {loading ? (
          <div className="rounded-xl border border-border py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <DataTable
            columns={adminTransactionColumns}
            data={rows}
            emptyMessage={hasFilter ? t('ui.admin_transactions.no_matching') : t('ui.admin_transactions.no_transactions')}
          />
        )}

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              {t('ui.admin_transactions.pagination_info').replace('{page}', page + 1).replace('{totalPages}', totalPages).replace('{total}', totalElements)}
            </p>
            <div className="flex items-center gap-1">
              <Button variant="outline" size="sm" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0 || loading}>
                <ChevronLeft className="size-4" />
              </Button>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                const pageNum = Math.max(0, Math.min(page - 2, totalPages - 5)) + i;
                return (
                  <Button
                    key={pageNum}
                    variant={pageNum === page ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setPage(pageNum)}
                    disabled={loading}
                    className="w-8 h-8 p-0 text-xs"
                  >
                    {pageNum + 1}
                  </Button>
                );
              })}
              <Button variant="outline" size="sm" onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1 || loading}>
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
