import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { createUserApi, getUsersApi, adminUpdateUserApi, adminSetUserStatusApi } from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { DataTable } from '@/components/ui/data-table';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  UserPlus, X, CheckCircle2, AlertCircle, Loader2, Search, RefreshCw,
  ChevronLeft, ChevronRight, Pencil, Lock, Unlock, Save,
} from 'lucide-react';
import { ROLE_LABELS, getRoleBadgeClass, getRoleTextClass } from '@/lib/roleColors';

const ROLES = [
  { value: 'MEMBER', label: 'Học viên' },
  { value: 'INSTRUCTOR', label: 'Giảng viên' },
  { value: 'STAFF', label: 'Nhân viên' },
  { value: 'ADMIN_USER', label: 'Quản lý' },
  { value: 'SUPER_ADMIN', label: 'Quản trị viên' },
];

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

// ─── Modal Sửa tài khoản ──────────────────────────────────
function EditUserModal({ user, onClose, onSuccess }) {
  const [form, setForm] = useState({
    name: user.name || '',
    roleName: user.role || 'MEMBER',
    isInternal: !!user.isInternal,
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
    if (!form.name.trim()) { setError('Họ tên không được để trống.'); return; }
    setLoading(true);
    try {
      const res = await adminUpdateUserApi(user.id, {
        name: form.name.trim(),
        roleName: form.roleName,
        isInternal: form.isInternal,
      });
      onSuccess(res.data.data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Cập nhật thất bại.');
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Pencil className="size-5" />
            <h2 className="text-lg font-semibold">Sửa tài khoản</h2>
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

          <div className="space-y-1 text-xs text-muted-foreground">
            <span className="font-mono">{user.username}</span> · {user.email}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="edit-name">Họ tên</Label>
            <Input id="edit-name" name="name" value={form.name} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-role">Vai trò</Label>
            <select
              id="edit-role" name="roleName" value={form.roleName} onChange={handleChange}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <input id="edit-internal" name="isInternal" type="checkbox" checked={form.isInternal} onChange={handleChange} className="rounded" />
            <Label htmlFor="edit-internal" className="cursor-pointer">Tài khoản nội bộ</Label>
          </div>
          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading ? <Loader2 className="size-4 animate-spin mr-2" /> : <Save className="size-4 mr-2" />}
              {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
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
  const [editUser, setEditUser] = useState(null);
  const [statusBusyId, setStatusBusyId] = useState(null);
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

  const handleEditSuccess = (updated) => {
    setEditUser(null);
    setSuccessMsg(`Đã cập nhật: ${updated?.name || updated?.email || 'thành công'}`);
    setTimeout(() => setSuccessMsg(''), 5000);
    fetchUsers(search, page);
  };

  const handleToggleStatus = async (user) => {
    const nextEnabled = !user.enabled;
    const action = nextEnabled ? 'mở khóa' : 'khóa';
    if (!window.confirm(`Bạn chắc chắn muốn ${action} tài khoản "${user.name}"?`)) return;
    setStatusBusyId(user.id);
    setError('');
    try {
      await adminSetUserStatusApi(user.id, nextEnabled);
      setSuccessMsg(`Đã ${action} tài khoản: ${user.name}`);
      setTimeout(() => setSuccessMsg(''), 5000);
      fetchUsers(search, page);
    } catch (err) {
      setError(err?.response?.data?.message || `Không thể ${action} tài khoản.`);
    } finally {
      setStatusBusyId(null);
    }
  };

  const isSelf = (user) => adminUser?.id === user.id;
  const canManage = (user) => {
    // ADMIN_USER không thao tác được SUPER_ADMIN; không tự thao tác chính mình (đổi role/khóa)
    if (isSelf(user)) return false;
    if (user.role === 'SUPER_ADMIN' && adminUser?.role !== 'SUPER_ADMIN') return false;
    return true;
  };

  const columns = useMemo(() => [
    {
      accessorKey: 'id',
      header: 'ID',
      cell: ({ row }) => (
        <span className="inline-flex items-center justify-center min-w-7 px-2 py-0.5 rounded-md text-xs font-semibold font-mono bg-indigo-50 text-indigo-700 border border-indigo-200">
          {row.original.id}
        </span>
      ),
    },
    {
      accessorKey: 'name',
      header: 'Họ tên',
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="text-sm font-medium truncate">{row.original.name}</p>
          <p className="text-xs text-muted-foreground font-mono truncate">{row.original.username}</p>
        </div>
      ),
    },
    {
      accessorKey: 'email',
      header: 'Email',
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="text-sm truncate">{row.original.email}</p>
          <p className="text-xs text-muted-foreground">{formatDate(row.original.createdAt)}</p>
        </div>
      ),
    },
    {
      accessorKey: 'role',
      header: 'Vai trò',
      cell: ({ row }) => (
        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold leading-none ${getRoleBadgeClass(row.original.role)}`}>
          {ROLE_LABELS[row.original.role] || row.original.role}
        </span>
      ),
    },
    {
      accessorKey: 'isInternal',
      header: () => <div className="text-center">Nội bộ</div>,
      cell: ({ row }) => (
        <div className="flex justify-center">
          <Checkbox checked={!!row.original.isInternal} />
        </div>
      ),
    },
    {
      accessorKey: 'enabled',
      header: 'Trạng thái',
      cell: ({ row }) => (
        row.original.enabled === false ? (
          <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-red-50 text-red-600 border border-red-200">
            <Lock className="size-3" />Khóa
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-200">
            <CheckCircle2 className="size-3" />Hoạt động
          </span>
        )
      ),
    },
    {
      id: 'actions',
      header: () => <div className="text-right">Hành động</div>,
      cell: ({ row }) => {
        const user = row.original;
        const manageable = canManage(user);
        return (
          <div className="flex items-center justify-end gap-1">
            <Button
              variant="outline"
              size="sm"
              className="h-8 px-2"
              disabled={!manageable}
              title={manageable ? 'Sửa' : 'Không thể chỉnh sửa tài khoản này'}
              onClick={() => setEditUser(user)}
            >
              <Pencil className="size-3.5" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              className={`h-8 px-2 ${user.enabled === false ? 'text-emerald-600' : 'text-red-600'}`}
              disabled={!manageable || statusBusyId === user.id}
              title={user.enabled === false ? 'Mở khóa' : 'Khóa'}
              onClick={() => handleToggleStatus(user)}
            >
              {statusBusyId === user.id
                ? <Loader2 className="size-3.5 animate-spin" />
                : user.enabled === false
                  ? <Unlock className="size-3.5" />
                  : <Lock className="size-3.5" />}
            </Button>
          </div>
        );
      },
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], [adminUser, statusBusyId]);

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">Quản lý người dùng</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
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
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="size-4" /><span>{successMsg}</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {/* Table */}
        {loading ? (
          <div className="rounded-xl border border-border py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <DataTable
            columns={columns}
            data={users}
            emptyMessage={search ? `Không tìm thấy kết quả cho "${search}".` : 'Chưa có người dùng nào.'}
          />
        )}

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
      {editUser && <EditUserModal user={editUser} onClose={() => setEditUser(null)} onSuccess={handleEditSuccess} />}
    </div>
  );
}
