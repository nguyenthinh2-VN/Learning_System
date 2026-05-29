import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { adminTopUpByIdentifierApi } from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import { Loader2, CheckCircle2, AlertCircle } from 'lucide-react';

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount ?? 0);
}

export default function AdminTopUpPage() {
  const { adminUser } = useAuth();
  const [form, setForm] = useState({ identifier: '', amount: '', note: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setError(''); setResult(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.identifier.trim() || !form.amount) { setError('Vui lòng nhập username/email và số tiền.'); return; }
    const amount = parseFloat(form.amount);
    if (amount <= 0) { setError('Số tiền phải lớn hơn 0.'); return; }

    setLoading(true); setError(''); setResult(null);
    try {
      const res = await adminTopUpByIdentifierApi({ identifier: form.identifier.trim(), amount, note: form.note || undefined });
      setResult(res.data.data || res.data);
      setForm({ identifier: '', amount: '', note: '' });
    } catch (err) { setError(err?.response?.data?.message || 'Cộng tiền thất bại.'); }
    finally { setLoading(false); }
  };

  const quickAmounts = [100000, 200000, 500000, 1000000];

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">Cộng tiền thủ công</span>
        <span className="text-xs text-muted-foreground ml-auto">{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-2xl mx-auto space-y-6">
        <div>
          <h1 className="text-2xl font-bold">Cộng tiền thủ công</h1>
          <p className="text-sm text-muted-foreground mt-1">Cộng tiền trực tiếp vào ví người dùng (chỉ SUPER_ADMIN)</p>
        </div>

        <Card>
          <CardContent className="pt-6">
            <form onSubmit={handleSubmit} className="space-y-5">
              {error && (
                <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
                  <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
                </div>
              )}

              <div className="space-y-1.5">
                <Label htmlFor="topup-identifier">Username hoặc Email *</Label>
                <Input id="topup-identifier" name="identifier" type="text" value={form.identifier} onChange={handleChange} placeholder="VD: MEM2B4A1D hoặc user@example.com" />
                <p className="text-xs text-muted-foreground">Nhập username hoặc email của người dùng cần cộng tiền</p>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="topup-amount">Số tiền (VNĐ) *</Label>
                <Input id="topup-amount" name="amount" type="number" value={form.amount} onChange={handleChange} placeholder="VD: 200000" min={1} />
                <div className="flex items-center gap-2 mt-2">
                  {quickAmounts.map((amt) => (
                    <Button key={amt} type="button" variant="outline" size="sm" onClick={() => { setForm((prev) => ({ ...prev, amount: String(amt) })); setError(''); }} className="h-7 text-xs">
                      {formatMoney(amt)}
                    </Button>
                  ))}
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="topup-note">Ghi chú (tùy chọn)</Label>
                <Input id="topup-note" name="note" value={form.note} onChange={handleChange} placeholder="VD: Bù lỗi giao dịch #123" />
              </div>

              <Button type="submit" className="w-full" disabled={loading}>
                {loading && <Loader2 className="size-4 animate-spin mr-2" />}
                {loading ? 'Đang xử lý...' : 'Cộng tiền'}
              </Button>
            </form>
          </CardContent>
        </Card>

        {result && (
          <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-6 space-y-4">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="size-5 text-emerald-500" />
              <h3 className="text-lg font-semibold text-emerald-600 dark:text-emerald-400">Cộng tiền thành công!</h3>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div><p className="text-xs text-muted-foreground mb-1">Người dùng</p><p className="font-medium">{result.username || `ID: ${result.userId}`}</p></div>
              <div><p className="text-xs text-muted-foreground mb-1">Số tiền cộng</p><p className="font-semibold text-emerald-500">+{formatMoney(result.addedAmount)}</p></div>
              <div><p className="text-xs text-muted-foreground mb-1">Số dư mới</p><p className="font-semibold">{formatMoney(result.newBalance)}</p></div>
              <div><p className="text-xs text-muted-foreground mb-1">Mã tham chiếu</p><code className="text-xs font-mono bg-muted px-2 py-0.5 rounded">{result.referenceCode}</code></div>
              {result.note && <div className="col-span-2"><p className="text-xs text-muted-foreground mb-1">Ghi chú</p><p className="text-sm">{result.note}</p></div>}
            </div>
          </div>
        )}

        <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 p-4">
          <p className="text-sm text-amber-600 dark:text-amber-400">
            <strong>Lưu ý:</strong> Thao tác cộng tiền sẽ được ghi vào audit log. Người dùng sẽ nhận thông báo WebSocket realtime khi ví được cập nhật.
          </p>
        </div>
      </div>
    </div>
  );
}
