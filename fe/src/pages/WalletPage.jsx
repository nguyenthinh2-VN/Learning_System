import '@/styles/brand.css';
import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import useWalletStore from '@/store/useWalletStore';
import { useAuth } from '@/context/AuthContext';
import { getTransactionsApi } from '@/api/wallet';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { DataTable } from '@/components/ui/data-table';
import { transactionColumns } from '@/features/wallet/transactionColumns';
import {
  Wallet,
  Plus,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  QrCode,
  Terminal,
} from 'lucide-react';

const QUICK_AMOUNTS = [50000, 100000, 200000, 500000];

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount ?? 0);
}

export default function WalletPage() {
  const { publicUser, balance } = useAuth();
  const {
    initTopUp, triggerMockWebhook,
    topUpLoading, topUpError, topUpResult,
    mockLoading, mockError, resetTopUp,
  } = useWalletStore();

  const [amount, setAmount] = useState('');
  const [mockRef, setMockRef] = useState('');
  const [mockSuccess, setMockSuccess] = useState(null);
  const [initSuccess, setInitSuccess] = useState(false);

  // ─── Lịch sử giao dịch ────────────────────────────────
  const PAGE_SIZE = 10;
  const [txData, setTxData] = useState({ items: [], totalPages: 0, totalElements: 0, page: 0 });
  const [txLoading, setTxLoading] = useState(true);
  const [txError, setTxError] = useState('');
  const [txPage, setTxPage] = useState(0);

  const loadTransactions = async (page) => {
    setTxLoading(true);
    setTxError('');
    try {
      const res = await getTransactionsApi(page, PAGE_SIZE);
      setTxData(res.data.data);
    } catch (e) {
      setTxError(e?.response?.data?.message || 'Không tải được lịch sử giao dịch.');
    } finally {
      setTxLoading(false);
    }
  };

  // Tải khi đổi trang
  useEffect(() => {
    loadTransactions(txPage);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [txPage]);

  // Khi số dư đổi (WebSocket WALLET_UPDATED qua AuthContext) → làm mới trang đầu
  useEffect(() => {
    if (balance === null) return;
    if (txPage === 0) loadTransactions(0);
    else setTxPage(0); // về trang đầu, effect trên sẽ tải lại
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [balance]);

  const handleTopUp = async () => {
    const value = parseFloat(amount);
    if (!value || value < 10000) return;
    setInitSuccess(false);
    setMockSuccess(null);
    try {
      await initTopUp(value);
      setInitSuccess(true);
      setAmount('');
    } catch {
      // error handled in store
    }
  };

  const handleMockWebhook = async () => {
    const ref = mockRef.trim() || topUpResult?.referenceCode;
    if (!ref) return;
    try {
      await triggerMockWebhook(ref);
      toast.success('Mock webhook gui thanh cong! Dang doi WebSocket cap nhat so du...');
      setMockRef('');
      resetTopUp();
      setInitSuccess(false);
    } catch {
      // error handled in store
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-6 py-10 space-y-8">

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Ví tiền</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Quản lý số dư và nạp tiền vào tài khoản
        </p>
      </div>

      {/* Balance Card — gradient emerald-teal-cyan (tông "tiền"), glassmorphism */}
      <div
        className="relative overflow-hidden rounded-2xl p-6"
        style={{
          background: 'linear-gradient(135deg, #059669 0%, #0D9488 55%, #0891B2 100%)',
          boxShadow: '0 8px 32px rgba(13, 148, 136, 0.35)',
        }}
      >
        {/* Blob trang trí — hiệu ứng mờ xuyên */}
        <div
          className="absolute -top-12 -right-12 w-52 h-52 rounded-full pointer-events-none"
          style={{ background: 'rgba(255,255,255,0.10)', filter: 'blur(2px)' }}
        />
        <div
          className="absolute -bottom-10 -left-10 w-40 h-40 rounded-full pointer-events-none"
          style={{ background: 'rgba(255,255,255,0.07)', filter: 'blur(1px)' }}
        />
        {/* Glassmorphism inner highlight */}
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: 'linear-gradient(135deg, rgba(255,255,255,0.15) 0%, transparent 60%)',
            borderRadius: 'inherit',
          }}
        />

        <div className="relative">
          <div className="flex items-center gap-2 mb-1">
            <Wallet className="size-4" style={{ color: 'rgba(255,255,255,0.7)' }} />
            <p className="text-sm" style={{ color: 'rgba(255,255,255,0.7)' }}>Số dư hiện tại</p>
          </div>
          <p className="text-4xl font-bold tracking-tight text-white">
            {balance !== null ? formatMoney(balance) : '...'}
          </p>
          <p className="text-xs mt-2 flex items-center gap-1" style={{ color: 'rgba(255,255,255,0.5)' }}>
            <span>Tài khoản: {publicUser?.name || 'Người dùng'}</span>
            {balance === null && <RefreshCw className="size-3 animate-spin ml-1" />}
          </p>
        </div>
      </div>

      {/* Nap tien Form */}
      <div className="border rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-2 mb-2">
          <Plus className="size-5" />
          <h2 className="font-semibold">Nạp tiền</h2>
        </div>

        {/* Quick amounts */}
        <div>
          <p className="text-xs text-muted-foreground mb-2">Chọn nhanh:</p>
          <div className="grid grid-cols-4 gap-2">
            {QUICK_AMOUNTS.map((v) => (
              <button
                key={v}
                onClick={() => setAmount(String(v))}
                className={`text-xs py-2 px-3 rounded-lg border transition-colors ${
                  amount === String(v)
                    ? 'bg-foreground text-background border-foreground'
                    : 'bg-background hover:bg-muted border-border'
                }`}
              >
                {formatMoney(v)}
              </button>
            ))}
          </div>
        </div>

        {/* Custom amount */}
        <div className="space-y-1">
          <p className="text-xs text-muted-foreground">Hoặc nhập số tiền (VND, tối thiểu 10.000d):</p>
          <div className="flex gap-2">
            <Input
              id="topup-amount"
              type="number"
              placeholder="Ví dụ: 200000"
              value={amount}
              min={10000}
              onChange={(e) => setAmount(e.target.value)}
              className="flex-1"
            />
            <Button
              id="topup-submit"
              onClick={handleTopUp}
              disabled={topUpLoading || !amount || parseFloat(amount) < 10000}
              className="min-w-24"
            >
              {topUpLoading ? 'Đang xử lý...' : 'Tiếp tục'}
            </Button>
          </div>
        </div>

        {/* Error */}
        {topUpError && (
          <div className="flex items-start gap-2 text-sm px-4 py-3 rounded-lg bg-red-50 text-red-800 border border-red-200">
            <AlertCircle className="size-4 shrink-0 mt-0.5 text-red-600" />
            <span>{topUpError}</span>
          </div>
        )}

        {/* Result: QR or mock instruction */}
        {initSuccess && topUpResult && (
          <div className="border rounded-lg p-4 space-y-3 bg-muted/30">
            {topUpResult.displayType === 'QR_URL' ? (
              <div className="flex flex-col items-center gap-3">
                <div className="flex items-center gap-2 text-sm font-medium">
                  <QrCode className="size-4" />
                  <span>Quét mã QR để thanh toán</span>
                </div>
                <img
                  src={topUpResult.displayData}
                  alt="QR Code"
                  className="w-48 h-48 border rounded-lg"
                />
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2 text-sm font-medium">
                  <Terminal className="size-4" />
                  <span>Hướng dẫn (Dev mode)</span>
                </div>
                <p className="text-xs text-muted-foreground bg-muted rounded p-3 font-mono break-all">
                  {topUpResult.displayData}
                </p>
              </div>
            )}

            <div className="text-xs text-muted-foreground text-center">
              Ma giao dich: <span className="font-mono font-semibold">{topUpResult.referenceCode}</span>
            </div>

            {/* Dev: mock webhook trigger */}
            <div className="border-t pt-3 space-y-2">
              <p className="text-xs text-muted-foreground font-medium">Dev: Kich hoat thanh toan gia lap</p>
              <div className="flex gap-2">
                <Input
                  id="mock-ref-input"
                  type="text"
                  placeholder={topUpResult.referenceCode}
                  value={mockRef}
                  onChange={(e) => setMockRef(e.target.value)}
                  className="flex-1 text-xs font-mono"
                />
                <Button
                  id="mock-webhook-submit"
                  variant="outline"
                  size="sm"
                  onClick={handleMockWebhook}
                  disabled={mockLoading}
                  className="whitespace-nowrap"
                >
                  {mockLoading ? 'Đang gửi...' : 'Kích hoạt'}
                </Button>
              </div>
              {mockError && <p className="text-xs text-red-600">{mockError}</p>}
            </div>
          </div>
        )}

        {/* Mock success */}
        {mockSuccess && (
          <div className="flex items-start gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-50 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="size-4 shrink-0 mt-0.5 text-emerald-600" />
            <span>{mockSuccess}</span>
          </div>
        )}
      </div>

      {/* Lich su giao dich */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">Lịch sử giao dịch</h2>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadTransactions(txPage)}
            disabled={txLoading}
          >
            <RefreshCw className={`size-4 mr-2 ${txLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {txError ? (
          <div className="flex items-center justify-between gap-2 text-sm px-4 py-3 rounded-lg bg-red-50 text-red-800 border border-red-200">
            <span className="flex items-center gap-2">
              <AlertCircle className="size-4 shrink-0 text-red-600" />
              {txError}
            </span>
            <Button variant="outline" size="sm" onClick={() => loadTransactions(txPage)}>
              Thử lại
            </Button>
          </div>
        ) : txLoading ? (
          <div className="rounded-xl border p-4 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="flex items-center gap-4">
                <Skeleton className="size-7 rounded-full" />
                <Skeleton className="h-4 flex-1" />
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
        ) : (
          <>
            <DataTable
              columns={transactionColumns}
              data={txData.items}
              emptyMessage="Chưa có giao dịch nào."
            />
            {txData.totalPages > 1 && (
              <div className="flex items-center justify-between pt-1">
                <span className="text-xs text-muted-foreground">
                  Trang {txData.page + 1}/{txData.totalPages} · {txData.totalElements} giao dịch
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={txPage === 0}
                    onClick={() => setTxPage((p) => Math.max(0, p - 1))}
                  >
                    Trước
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={txData.page + 1 >= txData.totalPages}
                    onClick={() => setTxPage((p) => p + 1)}
                  >
                    Sau
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
