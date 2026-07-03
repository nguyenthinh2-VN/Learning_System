import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useTranslation } from 'react-i18next';
import { getVouchersApi, createVoucherApi, updateVoucherApi, deleteVoucherApi } from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import { Ticket, Plus, X, Loader2, RefreshCw, Pencil, Trash2, AlertCircle, CheckCircle2 } from 'lucide-react';
import { getRoleTextClass } from '@/lib/roleColors';

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount ?? 0);
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

const EMPTY_FORM = {
  code: '', type: 'PERCENT', value: '', scope: 'ALL_COURSES', validFrom: '', validTo: '',
  minOrderAmount: 0, maxDiscount: 0, usageLimit: 0, usagePerUser: 0, applicableCourseIds: '', status: 'ACTIVE',
};

function VoucherFormModal({ voucher, onClose, onSave }) {
  const { t } = useTranslation();
  const isEdit = !!voucher;
  const [form, setForm] = useState(() => {
    if (voucher) {
      return {
        code: voucher.code || '', type: voucher.type || 'PERCENT', value: voucher.value ?? '',
        scope: voucher.scope || 'ALL_COURSES', validFrom: voucher.validFrom ? voucher.validFrom.slice(0, 16) : '',
        validTo: voucher.validTo ? voucher.validTo.slice(0, 16) : '', minOrderAmount: voucher.minOrderAmount ?? 0,
        maxDiscount: voucher.maxDiscount ?? 0, usageLimit: voucher.usageLimit ?? 0, usagePerUser: voucher.usagePerUser ?? 0,
        applicableCourseIds: voucher.applicableCourseIds ? voucher.applicableCourseIds.join(', ') : '', status: voucher.status || 'ACTIVE',
      };
    }
    return { ...EMPTY_FORM };
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const hasUsage = voucher?.usedCount > 0;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.code || !form.value || !form.validFrom || !form.validTo) {
      setError(t('ui.admin_vouchers.err_empty_fields')); return;
    }
    setLoading(true);
    try {
      const payload = {
        code: form.code.toUpperCase(), type: form.type, value: parseFloat(form.value), scope: form.scope,
        validFrom: form.validFrom + ':00', validTo: form.validTo + ':00', minOrderAmount: parseFloat(form.minOrderAmount) || 0,
        maxDiscount: parseFloat(form.maxDiscount) || 0, usageLimit: parseInt(form.usageLimit) || 0, usagePerUser: parseInt(form.usagePerUser) || 0,
        applicableCourseIds: form.scope === 'SPECIFIC_COURSES' && form.applicableCourseIds
          ? form.applicableCourseIds.split(',').map((s) => parseInt(s.trim())).filter(Boolean) : null,
        status: form.status,
      };

      if (isEdit) {
        if (hasUsage) { delete payload.code; delete payload.type; delete payload.value; }
        await updateVoucherApi(voucher.id, payload);
      } else {
        await createVoucherApi(payload);
      }
      onSave();
    } catch (err) { setError(err?.response?.data?.message || isEdit ? t('ui.admin_vouchers.err_update_fail') : t('ui.admin_vouchers.err_create_fail')); }
    finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 overflow-y-auto py-8">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-lg mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Ticket className="size-5" />
            <h2 className="text-lg font-semibold">{isEdit ? t('ui.admin_vouchers.edit_voucher') : t('ui.admin_vouchers.create_voucher_new')}</h2>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted"><X className="size-4" /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
              <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
            </div>
          )}
          {hasUsage && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-amber-500/10 text-amber-500 border border-amber-500/20">
              <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{t('ui.admin_vouchers.err_voucher_used').replace('{usedCount}', voucher.usedCount)}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.code_req')}</Label>
              <Input name="code" value={form.code} onChange={handleChange} placeholder="VD: WELCOME50" disabled={isEdit && hasUsage} className="uppercase" />
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.status')}</Label>
              <select name="status" value={form.status} onChange={handleChange} className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                <option value="ACTIVE">{t('ui.admin_vouchers.status_active')}</option><option value="INACTIVE">{t('ui.admin_vouchers.status_inactive')}</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.type_req')}</Label>
              <select name="type" value={form.type} onChange={handleChange} disabled={isEdit && hasUsage} className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                <option value="PERCENT">{t('ui.admin_vouchers.type_percent')}</option><option value="FIXED">{t('ui.admin_vouchers.type_fixed')}</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.value_req')} {form.type === 'PERCENT' ? '(%)' : '(VNĐ)'}</Label>
              <Input name="value" type="number" value={form.value} onChange={handleChange} placeholder={form.type === 'PERCENT' ? 'VD: 50' : 'VD: 100000'} disabled={isEdit && hasUsage} min={0} />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>{t('ui.admin_vouchers.scope')}</Label>
            <select name="scope" value={form.scope} onChange={handleChange} className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              <option value="ALL_COURSES">{t('ui.admin_vouchers.scope_all')}</option><option value="SPECIFIC_COURSES">{t('ui.admin_vouchers.scope_specific')}</option>
            </select>
          </div>

          {form.scope === 'SPECIFIC_COURSES' && (
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.specific_course_ids')}</Label>
              <Input name="applicableCourseIds" value={form.applicableCourseIds} onChange={handleChange} placeholder="VD: 1, 5, 12" />
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.valid_from')}</Label>
              <Input name="validFrom" type="datetime-local" value={form.validFrom} onChange={handleChange} />
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.valid_to')}</Label>
              <Input name="validTo" type="datetime-local" value={form.validTo} onChange={handleChange} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.min_order_amount')}</Label>
              <Input name="minOrderAmount" type="number" value={form.minOrderAmount} onChange={handleChange} min={0} />
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.max_discount')} {form.type === 'PERCENT' ? '' : t('ui.admin_vouchers.not_applicable')}</Label>
              <Input name="maxDiscount" type="number" value={form.maxDiscount} onChange={handleChange} min={0} disabled={form.type !== 'PERCENT'} />
              <p className="text-xs text-muted-foreground">{t('ui.admin_vouchers.no_limit')}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.usage_limit')}</Label>
              <Input name="usageLimit" type="number" value={form.usageLimit} onChange={handleChange} min={0} />
              <p className="text-xs text-muted-foreground">{t('ui.admin_vouchers.no_limit')}</p>
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_vouchers.usage_per_user')}</Label>
              <Input name="usagePerUser" type="number" value={form.usagePerUser} onChange={handleChange} min={0} />
              <p className="text-xs text-muted-foreground">{t('ui.admin_vouchers.no_limit')}</p>
            </div>
          </div>

          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">{t('ui.admin_vouchers.cancel')}</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading && <Loader2 className="size-4 animate-spin mr-2" />}
              {isEdit ? t('ui.admin_vouchers.edit_btn') : t('ui.admin_vouchers.create_btn')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AdminVouchersPage() {
  const { t } = useTranslation();
  const { adminUser } = useAuth();
  const [vouchers, setVouchers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editVoucher, setEditVoucher] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState({});
  const [successMsg, setSuccessMsg] = useState('');

  const fetchVouchers = async () => {
    setLoading(true); setError('');
    try {
      const res = await getVouchersApi({ size: 100 });
      const data = res.data.data;
      setVouchers(data?.items ?? data?.content ?? (Array.isArray(data) ? data : []));
    } catch (err) { setError(err?.response?.data?.message || t('ui.admin_vouchers.err_load_fail')); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchVouchers(); }, []);

  const handleDelete = async (voucher) => {
    if (!confirm(t('ui.admin_vouchers.confirm_delete').replace('{code}', voucher.code))) return;
    setDeleteLoading((p) => ({ ...p, [voucher.id]: true }));
    try {
      await deleteVoucherApi(voucher.id);
      setSuccessMsg(t('ui.admin_vouchers.msg_deleted').replace('{code}', voucher.code)); setTimeout(() => setSuccessMsg(''), 4000);
      fetchVouchers();
    } catch (err) { alert(err?.response?.data?.message || t('ui.admin_vouchers.err_delete_fail')); }
    finally { setDeleteLoading((p) => ({ ...p, [voucher.id]: false })); }
  };

  const handleFormSave = () => {
    setShowForm(false); setEditVoucher(null);
    setSuccessMsg(editVoucher ? t('ui.admin_vouchers.msg_updated_success') : t('ui.admin_vouchers.msg_created_success'));
    setTimeout(() => setSuccessMsg(''), 4000); fetchVouchers();
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">{t('ui.admin_vouchers.title')}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">{t('ui.admin_vouchers.title')}</h1>
            <p className="text-sm text-muted-foreground mt-1">{t('ui.admin_vouchers.subtitle')}</p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={fetchVouchers} disabled={loading}>
              <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />Làm mới
            </Button>
            <Button size="sm" onClick={() => { setEditVoucher(null); setShowForm(true); }}>
              <Plus className="size-4 mr-2" />Tạo voucher
            </Button>
          </div>
        </div>

        {successMsg && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
            <CheckCircle2 className="size-4" /><span>{successMsg}</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        <div className="rounded-xl border overflow-hidden">
          <div className="bg-muted/50 px-4 py-3 border-b grid grid-cols-12 gap-2 text-xs font-medium text-muted-foreground uppercase tracking-wider">
            <span className="col-span-2">{t('ui.admin_vouchers.col_code')}</span>
            <span className="col-span-2">{t('ui.admin_vouchers.col_type_value')}</span>
            <span className="col-span-2">{t('ui.admin_vouchers.col_scope')}</span>
            <span className="col-span-2">{t('ui.admin_vouchers.col_validity')}</span>
            <span className="col-span-1">{t('ui.admin_vouchers.col_used')}</span>
            <span className="col-span-1">{t('ui.admin_vouchers.status')}</span>
            <span className="col-span-2">{t('ui.admin_vouchers.col_actions')}</span>
          </div>

          {loading ? (
            <div className="py-20 flex items-center justify-center">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : vouchers.length === 0 ? (
            <div className="py-20 text-center text-sm text-muted-foreground">{t('ui.admin_vouchers.no_vouchers')}</div>
          ) : (
            <div className="divide-y">
              {vouchers.map((v) => (
                <div key={v.id} className="grid grid-cols-12 gap-2 px-4 py-3 items-center hover:bg-muted/30 transition-colors">
                  <div className="col-span-2">
                    <code className="text-sm font-mono font-semibold text-primary bg-primary/10 px-2 py-0.5 rounded">
                      {v.code}
                    </code>
                  </div>
                  <div className="col-span-2">
                    <p className="text-sm font-medium">{v.type === 'PERCENT' ? `${v.value}%` : formatMoney(v.value)}</p>
                    <p className="text-xs text-muted-foreground">
                      {v.type === 'PERCENT' ? t('ui.admin_vouchers.percent') : t('ui.admin_vouchers.fixed')}
                      {v.type === 'PERCENT' && v.maxDiscount > 0 && ` ${t('ui.admin_vouchers.max_suffix').replace('{max}', formatMoney(v.maxDiscount))}`}
                    </p>
                  </div>
                  <div className="col-span-2">
                    <p className="text-xs text-muted-foreground">{v.scope === 'ALL_COURSES' ? t('ui.admin_vouchers.all') : t('ui.admin_vouchers.specific')}</p>
                    {v.minOrderAmount > 0 && <p className="text-xs text-muted-foreground">{t('ui.admin_vouchers.min_suffix').replace('{min}', formatMoney(v.minOrderAmount))}</p>}
                  </div>
                  <div className="col-span-2">
                    <p className="text-xs text-muted-foreground">{formatDate(v.validFrom)}</p>
                    <p className="text-xs text-muted-foreground">→ {formatDate(v.validTo)}</p>
                  </div>
                  <div className="col-span-1">
                    <p className="text-sm font-medium">{v.usedCount ?? 0}{v.usageLimit > 0 ? `/${v.usageLimit}` : ''}</p>
                  </div>
                  <div className="col-span-1">
                    <Badge variant={v.status === 'ACTIVE' ? 'default' : 'secondary'}>
                      {v.status === 'ACTIVE' ? t('ui.admin_vouchers.status_active') : t('ui.admin_vouchers.status_inactive')}
                    </Badge>
                  </div>
                  <div className="col-span-2 flex items-center gap-1.5">
                    <Button size="sm" variant="outline" onClick={() => { setEditVoucher(v); setShowForm(true); }} title={t('ui.admin_vouchers.action_edit')}>
                      <Pencil className="size-3 mr-1" />Sửa
                    </Button>
                    <Button size="sm" variant="destructive" onClick={() => handleDelete(v)} disabled={deleteLoading[v.id]} title={t('ui.admin_vouchers.action_delete')}>
                      {deleteLoading[v.id] ? <Loader2 className="size-3 animate-spin mr-1" /> : <Trash2 className="size-3 mr-1" />}
                      Xóa
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {showForm && <VoucherFormModal voucher={editVoucher} onClose={() => { setShowForm(false); setEditVoucher(null); }} onSave={handleFormSave} />}
      </div>
    </div>
  );
}
