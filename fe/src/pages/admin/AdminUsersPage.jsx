import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { createUserApi, getUsersApi, adminUpdateUserApi, adminSetUserStatusApi } from '@/api/adminApi';
import { getDepartmentsApi } from '@/api/departmentApi';
import { useAuth } from '@/context/AuthContext';
import { useTranslation } from 'react-i18next';
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



function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

// ─── Modal Thêm tài khoản ─────────────────────────────────
function AddUserModal({ onClose, onSuccess }) {
  const { t } = useTranslation();
  const ROLES = [
    { value: 'MEMBER', label: t('ui.admin_users.role_member') },
    { value: 'INSTRUCTOR', label: t('ui.admin_users.role_instructor') },
    { value: 'STAFF', label: t('ui.admin_users.role_staff') },
    { value: 'ADMIN_USER', label: t('ui.admin_users.role_admin') },
    { value: 'SUPER_ADMIN', label: t('ui.admin_users.role_super_admin') },
  ];

  const [form, setForm] = useState({
    email: '', password: '', name: '', roleName: 'MEMBER', isInternal: false,
  });
  const [departments, setDepartments] = useState([]);
  const [selectedRootId, setSelectedRootId] = useState('');
  const [selectedChildId, setSelectedChildId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getDepartmentsApi().then((res) => setDepartments(res.data?.data || [])).catch(() => { });
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password || !form.name) {
      setError(t('ui.admin_users.err_empty_fields'));
      return;
    }
    setLoading(true);
    try {
      const payload = {
        ...form,
        departmentId: selectedChildId !== '' ? Number(selectedChildId) : (selectedRootId !== '' ? Number(selectedRootId) : null)
      };
      const res = await createUserApi(payload);
      onSuccess(res.data.data || res.data);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_users.err_create_fail'));
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <UserPlus className="size-5" />
            <h2 className="text-lg font-semibold">{t('ui.admin_users.add_user')}</h2>
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
            <Label htmlFor="add-name">{t('ui.admin_users.name_req')}</Label>
            <Input id="add-name" name="name" placeholder="Nguyễn Văn A" value={form.name} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-email">{t('ui.admin_users.email_req')}</Label>
            <Input id="add-email" name="email" type="email" placeholder="example@email.com" value={form.email} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-password">{t('ui.admin_users.password_req')}</Label>
            <Input id="add-password" name="password" type="password" placeholder="••••••••" value={form.password} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="add-role">{t('ui.admin_users.role')}</Label>
            <select
              id="add-role" name="roleName" value={form.roleName} onChange={handleChange}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <input id="add-internal" name="isInternal" type="checkbox" checked={form.isInternal} onChange={handleChange} className="rounded" />
            <Label htmlFor="add-internal" className="cursor-pointer">{t('ui.admin_users.internal_account')}</Label>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Khối (Cấp 1)</Label>
              <select
                value={selectedRootId}
                onChange={(e) => { setSelectedRootId(e.target.value); setSelectedChildId(''); }}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">— Chọn Khối —</option>
                {departments.filter(d => d.parentId == null).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>Phòng Ban Con (Cấp 2)</Label>
              <select
                value={selectedChildId}
                onChange={(e) => setSelectedChildId(e.target.value)}
                disabled={!selectedRootId}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
              >
                <option value="">— Chọn Phòng Ban Con —</option>
                {departments.filter(d => d.parentId == selectedRootId).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">{t('ui.admin_users.cancel')}</Button>
            <Button type="submit" disabled={loading} className="flex-1" id="add-user-submit">
              {loading && <Loader2 className="size-4 animate-spin mr-2" />}
              {loading ? t('ui.admin_users.creating') : t('ui.admin_users.create_btn')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Modal Sửa tài khoản ──────────────────────────────────
function EditUserModal({ user, onClose, onSuccess }) {
  const { t } = useTranslation();
  const ROLES = [
    { value: 'MEMBER', label: t('ui.admin_users.role_member') },
    { value: 'INSTRUCTOR', label: t('ui.admin_users.role_instructor') },
    { value: 'STAFF', label: t('ui.admin_users.role_staff') },
    { value: 'ADMIN_USER', label: t('ui.admin_users.role_admin') },
    { value: 'SUPER_ADMIN', label: t('ui.admin_users.role_super_admin') },
  ];

  const [form, setForm] = useState({
    name: user.name || '',
    roleName: user.role || 'MEMBER',
    isInternal: !!user.isInternal,
  });
  const [departments, setDepartments] = useState([]);
  const [selectedRootId, setSelectedRootId] = useState('');
  const [selectedChildId, setSelectedChildId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getDepartmentsApi().then((res) => {
      const depts = res.data?.data || [];
      setDepartments(depts);
      if (user.departmentId) {
        const dept = depts.find(d => d.id === user.departmentId);
        if (dept) {
          if (dept.parentId) {
            setSelectedRootId(dept.parentId);
            setSelectedChildId(dept.id);
          } else {
            setSelectedRootId(dept.id);
            setSelectedChildId('');
          }
        }
      }
    }).catch(() => { });
  }, [user]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) { setError(t('ui.admin_users.err_empty_name')); return; }
    setLoading(true);
    try {
      const res = await adminUpdateUserApi(user.id, {
        name: form.name.trim(),
        roleName: form.roleName,
        isInternal: form.isInternal,
        departmentId: selectedChildId !== '' ? Number(selectedChildId) : (selectedRootId !== '' ? Number(selectedRootId) : null),
      });
      onSuccess(res.data.data);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_users.err_update_fail'));
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Pencil className="size-5" />
            <h2 className="text-lg font-semibold">{t('ui.admin_users.edit_user')}</h2>
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
            <Label htmlFor="edit-name">{t('ui.admin_users.name')}</Label>
            <Input id="edit-name" name="name" value={form.name} onChange={handleChange} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-role">{t('ui.admin_users.role')}</Label>
            <select
              id="edit-role" name="roleName" value={form.roleName} onChange={handleChange}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <input id="edit-internal" name="isInternal" type="checkbox" checked={form.isInternal} onChange={handleChange} className="rounded" />
            <Label htmlFor="edit-internal" className="cursor-pointer">{t('ui.admin_users.internal_account')}</Label>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Khối (Cấp 1)</Label>
              <select
                value={selectedRootId}
                onChange={(e) => { setSelectedRootId(e.target.value); setSelectedChildId(''); }}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">— Chọn Khối —</option>
                {departments.filter(d => d.parentId == null).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>Phòng Ban Con (Cấp 2)</Label>
              <select
                value={selectedChildId}
                onChange={(e) => setSelectedChildId(e.target.value)}
                disabled={!selectedRootId}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
              >
                <option value="">— Chọn Phòng Ban Con —</option>
                {departments.filter(d => d.parentId == selectedRootId).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.code} — {d.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">{t('ui.admin_users.cancel')}</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading ? <Loader2 className="size-4 animate-spin mr-2" /> : <Save className="size-4 mr-2" />}
              {loading ? t('ui.admin_users.saving') : t('ui.admin_users.save_changes')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function AdminUsersPage() {
  const { t } = useTranslation();
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
      setError(err?.response?.data?.message || t('ui.admin_users.err_load_fail'));
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
    setSuccessMsg(t('ui.admin_users.msg_created_success').replace('{name}', user?.email || user?.name || ''));
    setTimeout(() => setSuccessMsg(''), 5000);
    fetchUsers(search, page);
  };

  const handleEditSuccess = (updated) => {
    setEditUser(null);
    setSuccessMsg(t('ui.admin_users.msg_updated_success').replace('{name}', updated?.name || updated?.email || ''));
    setTimeout(() => setSuccessMsg(''), 5000);
    fetchUsers(search, page);
  };

  const handleToggleStatus = async (user) => {
    const nextEnabled = !user.enabled;
    const action = nextEnabled ? t('ui.admin_users.action_unlock') : t('ui.admin_users.action_lock');
    if (!window.confirm(t('ui.admin_users.confirm_toggle_status').replace('{action}', action).replace('{name}', user.name))) return;
    setStatusBusyId(user.id);
    setError('');
    try {
      await adminSetUserStatusApi(user.id, nextEnabled);
      setSuccessMsg(t('ui.admin_users.msg_toggled_status').replace('{action}', action).replace('{name}', user.name));
      setTimeout(() => setSuccessMsg(''), 5000);
      fetchUsers(search, page);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_users.err_toggle_status').replace('{action}', action));
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
      header: t('ui.admin_users.col_id'),
      cell: ({ row }) => (
        <span className="inline-flex items-center justify-center min-w-7 px-2 py-0.5 rounded-md text-xs font-semibold font-mono bg-indigo-50 text-indigo-700 border border-indigo-200">
          {row.original.id}
        </span>
      ),
    },
    {
      accessorKey: 'name',
      header: t('ui.admin_users.name'),
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="text-sm font-medium truncate">{row.original.name}</p>
          <p className="text-xs text-muted-foreground font-mono truncate">{row.original.username}</p>
        </div>
      ),
    },
    {
      accessorKey: 'email',
      header: t('ui.admin_users.col_email'),
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="text-sm truncate">{row.original.email}</p>
          <p className="text-xs text-muted-foreground">{formatDate(row.original.createdAt)}</p>
        </div>
      ),
    },
    {
      accessorKey: 'role',
      header: t('ui.admin_users.col_role'),
      cell: ({ row }) => {
        const getRoleLabel = (r) => {
          const map = {
            MEMBER: t('ui.admin_users.role_member', 'Học viên'),
            INSTRUCTOR: t('ui.admin_users.role_instructor', 'Giảng viên'),
            STAFF: t('ui.admin_users.role_staff', 'Nhân viên'),
            ADMIN_USER: t('ui.admin_users.role_admin', 'Quản lý'),
            SUPER_ADMIN: t('ui.admin_users.role_super_admin', 'Quản trị viên'),
          };
          return map[r] || ROLE_LABELS[r] || r;
        };
        return (
          <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold leading-none ${getRoleBadgeClass(row.original.role)}`}>
            {getRoleLabel(row.original.role)}
          </span>
        );
      },
    },
    {
      accessorKey: 'isInternal',
      header: () => <div className="text-center">{t('ui.admin_users.col_internal')}</div>,
      cell: ({ row }) => (
        <div className="flex justify-center">
          <Checkbox checked={!!row.original.isInternal} />
        </div>
      ),
    },
    {
      accessorKey: 'enabled',
      header: t('ui.admin_users.col_status'),
      cell: ({ row }) => (
        row.original.enabled === false ? (
          <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-red-50 text-red-600 border border-red-200">
            <Lock className="size-3" />{t('ui.admin_users.status_locked', 'Khóa')}
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-200">
            <CheckCircle2 className="size-3" />{t('ui.admin_users.status_active', 'Hoạt động')}
          </span>
        )
      ),
    },
    {
      id: 'actions',
      header: () => <div className="text-right">{t('ui.admin_users.col_actions')}</div>,
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
              title={manageable ? t('ui.admin_users.action_edit') : t('ui.admin_users.no_edit_permission')}
              onClick={() => setEditUser(user)}
            >
              <Pencil className="size-3.5" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              className={`h-8 px-2 ${user.enabled === false ? 'text-emerald-600' : 'text-red-600'}`}
              disabled={!manageable || statusBusyId === user.id}
              title={user.enabled === false ? t('ui.admin_users.action_unlock_tooltip') : t('ui.admin_users.action_lock_tooltip')}
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
        <span className="text-sm font-medium">{t('ui.admin_users.title')}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">{t('ui.admin_users.title')}</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {totalElements > 0 ? t('ui.admin_users.users_count').replace('{count}', totalElements) : t('ui.admin_users.subtitle')}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => fetchUsers(search, page)} disabled={loading}>
              <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />{t('ui.admin_users.refresh', 'Làm mới')}
            </Button>
            <Button id="open-add-user" onClick={() => setShowModal(true)}>
              <UserPlus className="size-4 mr-2" />{t('ui.admin_users.add_user', 'Thêm tài khoản')}
            </Button>
          </div>
        </div>

        {/* Search */}
        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <Input
            placeholder={t('ui.admin_users.search_placeholder')}
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
            emptyMessage={search ? t('ui.admin_users.no_search_results').replace('{search}', search) : t('ui.admin_users.no_users')}
          />
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              {t('ui.admin_users.pagination_info').replace('{page}', page + 1).replace('{totalPages}', totalPages).replace('{total}', totalElements)}
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
