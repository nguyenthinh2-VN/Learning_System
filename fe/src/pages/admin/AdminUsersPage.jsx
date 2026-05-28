import { useState, useEffect, useCallback, useRef } from 'react';
import { createUserApi, getUsersApi } from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  UserPlus, X, CheckCircle2, AlertCircle, Loader2, Search, RefreshCw,
  ChevronLeft, ChevronRight,
} from 'lucide-react';

const ROLES = [
  { value: 'MEMBER', label: 'Học viên' },
  { value: 'INSTRUCTOR', label: 'Giảng viên' },
  { value: 'STAFF', label: 'Nhân viên' },
  { value: 'ADMIN_USER', label: 'Quản lý' },
  { value: 'SUPER_ADMIN', label: 'Quản trị viên' },
];

const ROLE_BADGE = {
  MEMBER: 'secondary',
  INSTRUCTOR: 'outline',
  STAFF: 'outline',
  ADMIN_USER: 'default',
  SUPER_ADMIN: 'default',
};

const ROLE_LABELS = {
  MEMBER: 'Học viên',
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý',
  SUPER_ADMIN: 'Quản trị viên',
};

function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

// ─── Modal Thêm tài khoản ─────────────────────────────────
function AddUserModal({ onClose, onSuccess }) {
  const [form, setForm] = useState({
    email: '', password: '', name: '', roleName: 'MEMBER', isInternal: false,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password || !form.name) {
      setError('Vui lòng nhập đầy đủ thông tin bắt buộc.');
      return;
    }
    setLoading(true);
    try {
      const res = await createUserApi(form);
      onSuccess(res.data.data || res.data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Tạo tài khoản thất bại.');
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <UserPlus className="size-5" />
            <h2 className="text-lg font-semibold">Thêm tài khoản</h2>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted">
            <X className="size-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
              <AlertCircle className="size-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="add-name">Họ tên *</Label>
            <Input id="add-name" name="name" placeholder="Nguyễn Văn A" value={form.name} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-email">Email *</Label>
            <Input id="add-email" name="email" type="email" placeholder="example@email.com" value={form.email} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-password">Mật khẩu *</Label>
            <Input id="add-password" name="password" type="password" placeholder="••••••••" value={form.password} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-role">Vai trò</Label>
            <select
              id="add-role" name="roleName" value={form.roleName} onChange={handleChange}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <input id="add-internal" name="isInternal" type="checkbox" checked={form.isInternal} onChange={handleChange} className="rounded" />
            <Label htmlFor="add-internal" className="cursor-pointer">Tài khoản nội bộ</Label>
          </div>
          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
            <Button type="submit" disabled={loading} className="flex-1" id="add-user-submit">
              {loading && <Loader2 className="size-4 animate-spin mr-2" />}
              {loading ? 'Đang tạo...' : 'Tạo tài khoản'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function AdminUsersPage() {
  const { adminUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const PAGE_SIZE = 20;

  const [showModal, setShowModal] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  const debounceRef = useRef(null);

  const fetchUsers = useCallback(async (keyword, pageNum) => {
    setLoading(true); setError('');
    try {
      const res = await getUsersApi({ keyword, page: pageNum, size: PAGE_SIZE });
      const data = res.data.data;
      setUsers(data?.items ?? []);
      setTotalPages(data?.totalPages ?? 0);
      setTotalElements(data?.totalElements ?? 0);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách người dùng.');
    } finally { setLoading(false); }
  }, []);

  // Debounce search
  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      fetchUsers(search, 0);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [search, fetchUsers]);

  // Page change
  useEffect(() => {
    fetchUsers(search, page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const handleSuccess = (user) => {
    setShowModal(false);
    setSuccessMsg(`Đã tạo tài khoản: ${user?.email || user?.name || 'thành công'}`);
    setTimeout(() => setSuccessMsg(''), 5000);
    fetchUsers(search, page);
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">Quản lý người dùng</span>
        <span className="text-xs text-muted-foreground ml-auto">{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Quản lý người dùng</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {totalElements > 0 ? `${totalElements} người dùng` : 'Tạo và quản lý tài khoản người dùng'}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => fetchUsers(search, page)} disabled={loading}>
              <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />Làm mới
            </Button>
            <Button id="open-add-user" onClick={() => setShowModal(true)}>
              <UserPlus className="size-4 mr-2" />Thêm tài khoản
            </Button>
          </div>
        </div>

        {/* Search */}
        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <Input
            placeholder="Tìm theo tên hoặc email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>

        {successMsg && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="size-4" /><span>{successMsg}</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {/* Table */}
        <div className="rounded-xl border border-border overflow-hidden">
          <div className="bg-muted/50 px-4 py-3 border-b border-border grid grid-cols-12 gap-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
            <span className="col-span-1">ID</span>
            <span className="col-span-3">Họ tên</span>
            <span className="col-span-3">Email</span>
            <span className="col-span-2">Vai trò</span>
            <span className="col-span-1">Loại TK</span>
            <span className="col-span-2">Ngày tạo</span>
          </div>

          {loading ? (
            <div className="py-20 flex items-center justify-center">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : users.length === 0 ? (
            <div className="py-20 text-center text-sm text-muted-foreground">
              {search ? `Không tìm thấy kết quả cho "${search}".` : 'Chưa có người dùng nào.'}
            </div>
          ) : (
            <div className="divide-y divide-border">
              {users.map((user) => (
                <div key={user.id} className="grid grid-cols-12 gap-3 px-4 py-3 items-center hover:bg-muted/20 transition-colors">
                  <div className="col-span-1">
                    <span className="text-xs text-muted-foreground font-mono">{user.id}</span>
                  </div>
                  <div className="col-span-3">
                    <p className="text-sm font-medium truncate">{user.name}</p>
                    <p className="text-xs text-muted-foreground font-mono">{user.username}</p>
                  </div>
                  <div className="col-span-3">
                    <p className="text-sm truncate">{user.email}</p>
                  </div>
                  <div className="col-span-2">
                    <Badge variant={ROLE_BADGE[user.role] || 'secondary'} className="text-xs">
                      {ROLE_LABELS[user.role] || user.role}
                    </Badge>
                  </div>
                  <div className="col-span-1">
                    <span className={`text-xs px-1.5 py-0.5 rounded-full ${
                      user.isInternal
                        ? 'bg-indigo-500/15 text-indigo-300 border border-indigo-500/30'
                        : 'bg-muted text-muted-foreground'
                    }`}>
                      {user.isInternal ? 'Nội bộ' : 'Ngoài'}
                    </span>
                  </div>
                  <div className="col-span-2">
                    <p className="text-xs text-muted-foreground">{formatDate(user.createdAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              Trang {page + 1} / {totalPages} &bull; {totalElements} người dùng
            </p>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
              >
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
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || loading}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {showModal && <AddUserModal onClose={() => setShowModal(false)} onSuccess={handleSuccess} />}
    </div>
  );
}
