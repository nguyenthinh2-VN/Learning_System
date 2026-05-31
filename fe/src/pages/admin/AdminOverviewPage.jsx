import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { getAdminCoursesApi, getInstructorCoursesApi, getPendingCoursesApi, getVouchersApi, getRevenueReportApi } from '@/api/adminApi';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  Users, BookOpen, ClipboardCheck, Ticket, TrendingUp, Wallet,
  ArrowRight, Loader2, ShieldCheck, GraduationCap, ShoppingCart, Receipt,
} from 'lucide-react';
import { ROLE_LABELS, getRoleBadgeClass, getRoleTextClass } from '@/lib/roleColors';
import RevenueChart from '@/features/wallet/RevenueChart';

const fmtMoney = (a) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(a ?? 0);

const STAT_TONES = {
  indigo: {
    icon: 'bg-indigo-100 text-indigo-600',
    ring: 'hover:border-indigo-300 hover:shadow-indigo-100/60',
    value: 'text-indigo-950',
  },
  amber: {
    icon: 'bg-amber-100 text-amber-600',
    ring: 'hover:border-amber-300 hover:shadow-amber-100/60',
    value: 'text-amber-950',
  },
  violet: {
    icon: 'bg-violet-100 text-violet-600',
    ring: 'hover:border-violet-300 hover:shadow-violet-100/60',
    value: 'text-violet-950',
  },
  emerald: {
    icon: 'bg-emerald-100 text-emerald-600',
    ring: 'hover:border-emerald-300 hover:shadow-emerald-100/60',
    value: 'text-emerald-700',
  },
  sky: {
    icon: 'bg-sky-100 text-sky-600',
    ring: 'hover:border-sky-300 hover:shadow-sky-100/60',
    value: 'text-sky-950',
  },
  slate: {
    icon: 'bg-slate-100 text-slate-500',
    ring: 'hover:border-slate-300',
    value: 'text-foreground',
  },
};

function StatCard({ icon: Icon, label, value, sub, loading: isLoading, tone = 'slate' }) {
  const t = STAT_TONES[tone] ?? STAT_TONES.slate;
  return (
    <div
      className={`rounded-xl border border-border bg-card p-5 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg ${t.ring}`}
    >
      <div className="flex items-center gap-3 mb-3">
        <div className={`size-10 rounded-lg flex items-center justify-center shrink-0 ${t.icon}`}>
          <Icon className="size-5" />
        </div>
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
      <p className={`text-3xl font-bold tracking-tight ${t.value}`}>
        {isLoading ? <Loader2 className="size-6 animate-spin text-muted-foreground" /> : (value ?? '—')}
      </p>
      {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
    </div>
  );
}

function QuickAction({ to, icon: Icon, title, desc, tone = 'slate' }) {
  const t = STAT_TONES[tone] ?? STAT_TONES.slate;
  return (
    <Link
      to={to}
      className="group flex items-center gap-4 p-4 rounded-xl border border-border bg-card hover:bg-accent hover:border-transparent hover:shadow-md transition-all"
    >
      <div className={`size-10 rounded-lg flex items-center justify-center shrink-0 transition-transform group-hover:scale-105 ${t.icon}`}>
        <Icon className="size-5" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-muted-foreground mt-0.5">{desc}</p>
      </div>
      <ArrowRight className="size-4 text-muted-foreground opacity-0 -translate-x-1 group-hover:opacity-100 group-hover:translate-x-0 transition-all shrink-0" />
    </Link>
  );
}

export default function AdminOverviewPage() {
  const { adminUser } = useAuth();
  const role = adminUser?.role;
  const isInstructor = role === 'INSTRUCTOR';

  const isSuperAdmin = role === 'SUPER_ADMIN';

  const [stats, setStats] = useState({ courses: null, pending: null, vouchers: null });
  const [loading, setLoading] = useState(true);

  // Doanh thu — chỉ SUPER_ADMIN
  const [revenue, setRevenue] = useState(null);
  const [revenueLoading, setRevenueLoading] = useState(isSuperAdmin);
  const [granularity, setGranularity] = useState('DAY');

  useEffect(() => {
    const fetchStats = async () => {
      setLoading(true);
      try {
        if (isInstructor) {
          // INSTRUCTOR chỉ được xem course của mình
          const res = await getInstructorCoursesApi({ page: 0, size: 1 });
          setStats({
            courses: res.data?.data?.totalElements ?? null,
            pending: null,
            vouchers: null,
          });
        } else {
          const results = await Promise.allSettled([
            getAdminCoursesApi({ page: 0, size: 1 }),
            getPendingCoursesApi({ page: 0, size: 1 }),
            ['STAFF', 'SUPER_ADMIN'].includes(role) ? getVouchersApi({ page: 0, size: 1 }) : null,
          ]);
          setStats({
            courses: results[0].status === 'fulfilled'
              ? (results[0].value?.data?.data?.totalElements ?? null) : null,
            pending: results[1].status === 'fulfilled'
              ? (results[1].value?.data?.data?.totalElements ?? null) : null,
            vouchers: results[2]?.status === 'fulfilled' && results[2].value
              ? (results[2].value?.data?.data?.totalElements ?? null) : null,
          });
        }
      } catch { /* ignore */ }
      finally { setLoading(false); }
    };
    fetchStats();
  }, [role, isInstructor]);

  // Doanh thu theo granularity (chỉ SUPER_ADMIN)
  useEffect(() => {
    if (!isSuperAdmin) return;
    let cancelled = false;
    const fetchRevenue = async () => {
      setRevenueLoading(true);
      try {
        const res = await getRevenueReportApi({ granularity });
        if (!cancelled) setRevenue(res.data?.data ?? null);
      } catch {
        if (!cancelled) setRevenue(null);
      } finally {
        if (!cancelled) setRevenueLoading(false);
      }
    };
    fetchRevenue();
    return () => { cancelled = true; };
  }, [isSuperAdmin, granularity]);

  // Quick actions theo role
  const quickActions = isInstructor
    ? [
        { to: '/admin/courses', icon: BookOpen, title: 'Khóa học của tôi', desc: 'Xem và quản lý khóa học', tone: 'indigo' },
      ]
    : [
        { to: '/admin/users', icon: Users, title: 'Thêm tài khoản', desc: 'Tạo tài khoản người dùng mới', tone: 'violet', roles: ['ADMIN_USER', 'SUPER_ADMIN'] },
        { to: '/admin/courses/pending', icon: ClipboardCheck, title: 'Duyệt khóa học', desc: 'Xem danh sách chờ duyệt', tone: 'amber', roles: ['STAFF', 'SUPER_ADMIN'] },
        { to: '/admin/courses', icon: BookOpen, title: 'Quản lý khóa học', desc: 'Tất cả khóa học hệ thống', tone: 'indigo' },
        { to: '/admin/vouchers', icon: Ticket, title: 'Quản lý voucher', desc: 'Tạo và quản lý mã giảm giá', tone: 'violet', roles: ['STAFF', 'SUPER_ADMIN'] },
        { to: '/admin/transactions', icon: Receipt, title: 'Giao dịch hệ thống', desc: 'Xem mọi giao dịch của người dùng', tone: 'sky', roles: ['SUPER_ADMIN'] },
        { to: '/admin/wallet', icon: Wallet, title: 'Cộng tiền thủ công', desc: 'Cộng tiền vào ví người dùng', tone: 'emerald', roles: ['SUPER_ADMIN'] },
      ].filter((a) => !a.roles || a.roles.includes(role));

  return (
    <div className="min-h-screen">
      {/* Header */}
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        {isInstructor
          ? <GraduationCap className="size-4 text-muted-foreground" />
          : <ShieldCheck className="size-4 text-muted-foreground" />
        }
        <span className="text-sm font-medium">
          {isInstructor ? 'Instructor Portal' : 'Admin Portal'}
        </span>
        <div className="ml-auto flex items-center gap-2">
          <span className={`px-2 py-0.5 rounded-full text-[11px] font-medium ${getRoleBadgeClass(role)}`}>
            {ROLE_LABELS[role] || role}
          </span>
          <span className={`text-xs font-medium ${getRoleTextClass(role)}`}>{adminUser?.name}</span>
        </div>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-8">
        {/* Welcome */}
        <div>
          <h1 className="text-2xl font-bold">Xin chào, {adminUser?.name}</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {ROLE_LABELS[role] || role}
            {isInstructor ? ' — Quản lý nội dung khóa học' : ' — Quản trị hệ thống LearnSpace'}
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            icon={BookOpen}
            tone="indigo"
            label={isInstructor ? 'Khóa học của tôi' : 'Tổng khóa học'}
            value={stats.courses}
            loading={loading}
          />
          {!isInstructor && (
            <StatCard icon={ClipboardCheck} tone="amber" label="Chờ duyệt" value={stats.pending} loading={loading} />
          )}
          {['STAFF', 'SUPER_ADMIN'].includes(role) && (
            <StatCard icon={Ticket} tone="violet" label="Voucher" value={stats.vouchers} loading={loading} />
          )}
          {isSuperAdmin ? (
            <>
              {/* Doanh thu — card nổi bật gradient emerald */}
              <div className="relative overflow-hidden rounded-xl p-5 text-white shadow-lg shadow-emerald-500/20 bg-gradient-to-br from-emerald-500 to-teal-600 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-emerald-500/30">
                <div className="absolute -right-4 -top-4 size-24 rounded-full bg-white/10" />
                <div className="absolute -right-8 top-8 size-20 rounded-full bg-white/5" />
                <div className="relative">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="size-10 rounded-lg flex items-center justify-center shrink-0 bg-white/20 backdrop-blur">
                      <TrendingUp className="size-5" />
                    </div>
                    <p className="text-sm text-emerald-50">
                      Doanh thu ({granularity === 'MONTH' ? '12 tháng' : '30 ngày'})
                    </p>
                  </div>
                  <p className="text-3xl font-bold tracking-tight">
                    {revenueLoading
                      ? <Loader2 className="size-6 animate-spin text-white/80" />
                      : (revenue ? fmtMoney(revenue.totalRevenue) : '—')}
                  </p>
                  {revenue && (
                    <p className="text-xs text-emerald-50/90 mt-1">
                      {revenue.paidPurchaseCount} giao dịch có doanh thu
                    </p>
                  )}
                </div>
              </div>
              <StatCard
                icon={ShoppingCart}
                tone="sky"
                label="Khóa bán ra"
                value={revenue ? revenue.coursesSold : '—'}
                sub={revenue ? `Nạp ví: ${fmtMoney(revenue.totalTopUp)}` : null}
                loading={revenueLoading}
              />
            </>
          ) : (
            !isInstructor && (
              <StatCard icon={TrendingUp} tone="emerald" label="Doanh thu" value="—" sub="Chỉ Quản trị viên" loading={false} />
            )
          )}
        </div>

        {/* Revenue Chart — chỉ SUPER_ADMIN */}
        {isSuperAdmin && (
          <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="size-9 rounded-lg bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
                  <TrendingUp className="size-4.5" />
                </div>
                <div>
                  <h2 className="text-sm font-semibold">Biểu đồ doanh thu</h2>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    Tiền thu từ bán khóa học theo {granularity === 'MONTH' ? 'tháng' : 'ngày'}
                  </p>
                </div>
              </div>
              <div className="flex items-center rounded-lg bg-muted p-0.5">
                {[['DAY', 'Ngày'], ['MONTH', 'Tháng']].map(([val, label]) => (
                  <button
                    key={val}
                    onClick={() => setGranularity(val)}
                    className={`px-3 py-1 text-xs font-medium rounded-md transition-all ${
                      granularity === val
                        ? 'bg-card text-emerald-600 shadow-sm'
                        : 'text-muted-foreground hover:text-foreground'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
            {revenueLoading ? (
              <div className="flex h-72 items-center justify-center">
                <Loader2 className="size-6 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <RevenueChart data={revenue?.series} granularity={granularity} />
            )}
          </div>
        )}

        {/* Quick Actions */}
        <div>
          <h2 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
            Thao tác nhanh
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {quickActions.map((action) => (
              <QuickAction key={action.to} {...action} />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
