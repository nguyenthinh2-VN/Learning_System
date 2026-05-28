import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { getAdminCoursesApi, getInstructorCoursesApi, getPendingCoursesApi, getVouchersApi } from '@/api/adminApi';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  Users, BookOpen, ClipboardCheck, Ticket, TrendingUp, Wallet,
  ArrowRight, Loader2, ShieldCheck, GraduationCap,
} from 'lucide-react';

const ROLE_LABELS = {
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý',
  SUPER_ADMIN: 'Quản trị viên',
};

function StatCard({ icon: Icon, label, value, sub, loading: isLoading }) {
  return (
    <div className="rounded-xl border border-border bg-card p-5 transition-colors hover:bg-card/80">
      <div className="flex items-center gap-3 mb-3">
        <div className="size-10 rounded-lg bg-muted flex items-center justify-center">
          <Icon className="size-5 text-muted-foreground" />
        </div>
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
      <p className="text-3xl font-bold">
        {isLoading ? <Loader2 className="size-6 animate-spin text-muted-foreground" /> : (value ?? '—')}
      </p>
      {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
    </div>
  );
}

function QuickAction({ to, icon: Icon, title, desc }) {
  return (
    <Link
      to={to}
      className="group flex items-center gap-4 p-4 rounded-xl border border-border bg-card hover:bg-accent transition-colors"
    >
      <div className="size-10 rounded-lg bg-muted flex items-center justify-center text-muted-foreground group-hover:text-foreground transition-colors shrink-0">
        <Icon className="size-5" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-muted-foreground mt-0.5">{desc}</p>
      </div>
      <ArrowRight className="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
    </Link>
  );
}

export default function AdminOverviewPage() {
  const { adminUser } = useAuth();
  const role = adminUser?.role;
  const isInstructor = role === 'INSTRUCTOR';

  const [stats, setStats] = useState({ courses: null, pending: null, vouchers: null });
  const [loading, setLoading] = useState(true);

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

  // Quick actions theo role
  const quickActions = isInstructor
    ? [
        { to: '/admin/courses', icon: BookOpen, title: 'Khóa học của tôi', desc: 'Xem và quản lý khóa học' },
      ]
    : [
        { to: '/admin/users', icon: Users, title: 'Thêm tài khoản', desc: 'Tạo tài khoản người dùng mới', roles: ['ADMIN_USER', 'SUPER_ADMIN'] },
        { to: '/admin/courses/pending', icon: ClipboardCheck, title: 'Duyệt khóa học', desc: 'Xem danh sách chờ duyệt', roles: ['STAFF', 'SUPER_ADMIN'] },
        { to: '/admin/courses', icon: BookOpen, title: 'Quản lý khóa học', desc: 'Tất cả khóa học hệ thống' },
        { to: '/admin/vouchers', icon: Ticket, title: 'Quản lý voucher', desc: 'Tạo và quản lý mã giảm giá', roles: ['STAFF', 'SUPER_ADMIN'] },
        { to: '/admin/wallet', icon: Wallet, title: 'Cộng tiền thủ công', desc: 'Cộng tiền vào ví người dùng', roles: ['SUPER_ADMIN'] },
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
          <span className={`px-2 py-0.5 rounded-full text-[11px] font-medium border ${
            isInstructor
              ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
              : 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30'
          }`}>
            {role}
          </span>
          <span className="text-xs text-muted-foreground">{adminUser?.name}</span>
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
            label={isInstructor ? 'Khóa học của tôi' : 'Tổng khóa học'}
            value={stats.courses}
            loading={loading}
          />
          {!isInstructor && (
            <StatCard icon={ClipboardCheck} label="Chờ duyệt" value={stats.pending} loading={loading} />
          )}
          {['STAFF', 'SUPER_ADMIN'].includes(role) && (
            <StatCard icon={Ticket} label="Voucher" value={stats.vouchers} loading={loading} />
          )}
          {!isInstructor && (
            <StatCard icon={TrendingUp} label="Doanh thu" value="—" sub="Cần BE endpoint" loading={false} />
          )}
        </div>

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
