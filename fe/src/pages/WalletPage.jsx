import { useState } from 'react';
import useWalletStore from '@/store/useWalletStore';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Wallet,
  Plus,
  TrendingUp,
  TrendingDown,
  ArrowUpCircle,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

const QUICK_AMOUNTS = [100000, 200000, 500000, 1000000];

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatDate(iso) {
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function WalletPage() {
  const { publicUser } = useAuth();
  const { balance, transactions, topUp } = useWalletStore();

  const [amount, setAmount] = useState('');
  const [status, setStatus] = useState(null); // 'success' | 'error' | null
  const [statusMsg, setStatusMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const handleTopUp = async () => {
    const value = parseFloat(amount);
    if (!value || value <= 0) {
      setStatus('error');
      setStatusMsg('Vui lòng nhập số tiền hợp lệ (> 0)');
      return;
    }
    setLoading(true);
    try {
      // TODO: Thay bằng API thật khi sẵn sàng: await topUpApi(value)
      await new Promise((r) => setTimeout(r, 600)); // simulate API delay
      const newBalance = topUp(value);
      setStatus('success');
      setStatusMsg(`Nạp thành công ${formatMoney(value)}. Số dư mới: ${formatMoney(newBalance)}`);
      setAmount('');
    } catch {
      setStatus('error');
      setStatusMsg('Nạp tiền thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
      setTimeout(() => setStatus(null), 4000);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-6 py-10 space-y-8">

      {/* ── Header ───────────────────────────────────────────────────── */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Ví tiền</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Quản lý số dư và giao dịch của bạn
        </p>
      </div>

      {/* ── Balance Card ─────────────────────────────────────────────── */}
      <div className="relative overflow-hidden rounded-2xl bg-foreground text-background p-6">
        {/* Background decoration */}
        <div className="absolute -top-10 -right-10 w-48 h-48 rounded-full bg-background/5" />
        <div className="absolute -bottom-8 -left-8 w-36 h-36 rounded-full bg-background/5" />

        <div className="relative">
          <div className="flex items-center gap-2 mb-1">
            <Wallet className="size-5 text-background/60" />
            <p className="text-sm text-background/60">Số dư hiện tại</p>
          </div>
          <p className="text-4xl font-bold tracking-tight">
            {formatMoney(balance)}
          </p>
          <p className="text-xs text-background/40 mt-2">
            Tài khoản: {publicUser?.name || 'Người dùng'}
          </p>
        </div>
      </div>

      {/* ── Top-up Form ──────────────────────────────────────────────── */}
      <div className="border rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-2 mb-2">
          <Plus className="size-5" />
          <h2 className="font-semibold">Nạp tiền</h2>
        </div>

        {/* Quick amount buttons */}
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
          <p className="text-xs text-muted-foreground">Hoặc nhập số tiền (VND):</p>
          <div className="flex gap-2">
            <Input
              id="topup-amount"
              type="number"
              placeholder="Ví dụ: 500000"
              value={amount}
              min={0}
              onChange={(e) => setAmount(e.target.value)}
              className="flex-1"
            />
            <Button
              id="topup-submit"
              onClick={handleTopUp}
              disabled={loading || !amount}
              className="min-w-24"
            >
              {loading ? 'Đang nạp...' : 'Nạp tiền'}
            </Button>
          </div>
        </div>

        {/* Status message */}
        {status && (
          <div
            className={`flex items-start gap-2 text-sm px-4 py-3 rounded-lg ${
              status === 'success'
                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                : 'bg-red-50 text-red-800 border border-red-200'
            }`}
          >
            {status === 'success' ? (
              <CheckCircle2 className="size-4 shrink-0 mt-0.5 text-emerald-600" />
            ) : (
              <AlertCircle className="size-4 shrink-0 mt-0.5 text-red-600" />
            )}
            <span>{statusMsg}</span>
          </div>
        )}

        <p className="text-xs text-muted-foreground/70">
          * Tính năng nạp tiền đang trong giai đoạn thử nghiệm. Số dư được lưu cục bộ.
        </p>
      </div>

      {/* ── Transaction History ───────────────────────────────────────── */}
      <div className="space-y-3">
        <h2 className="font-semibold">Lịch sử giao dịch</h2>

        {transactions.length === 0 ? (
          <div className="text-center py-12 border border-dashed rounded-xl text-sm text-muted-foreground">
            <ArrowUpCircle className="size-8 mx-auto mb-2 opacity-20" />
            Chưa có giao dịch nào.
          </div>
        ) : (
          <div className="border rounded-xl divide-y overflow-hidden">
            {transactions.map((tx) => (
              <div key={tx.id} className="flex items-center justify-between px-4 py-3.5">
                <div className="flex items-center gap-3">
                  <div
                    className={`size-8 rounded-full flex items-center justify-center ${
                      tx.amount > 0 ? 'bg-emerald-50' : 'bg-red-50'
                    }`}
                  >
                    {tx.amount > 0 ? (
                      <TrendingUp className="size-4 text-emerald-600" />
                    ) : (
                      <TrendingDown className="size-4 text-red-500" />
                    )}
                  </div>
                  <div>
                    <p className="text-sm font-medium">{tx.description}</p>
                    <p className="text-xs text-muted-foreground">{formatDate(tx.createdAt)}</p>
                  </div>
                </div>
                <p
                  className={`text-sm font-semibold ${
                    tx.amount > 0 ? 'text-emerald-600' : 'text-red-500'
                  }`}
                >
                  {tx.amount > 0 ? '+' : ''}{formatMoney(tx.amount)}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
