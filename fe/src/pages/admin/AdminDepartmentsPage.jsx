import { useState, useEffect, useCallback, Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Building2, Plus, X, Pencil, Trash2, ChevronRight, Loader2 } from 'lucide-react';
import { SidebarTrigger } from '@/components/ui/sidebar';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import {
  getDepartmentsApi,
  createDepartmentApi,
  updateDepartmentApi,
  deleteDepartmentApi,
} from '@/api/departmentApi';

// ─── Modal Thêm / Sửa phòng ban ───────────────────────────────────────────────
function DepartmentFormModal({ department, departments, onClose, onSave }) {
  const { t } = useTranslation();
  const isEdit = !!department;

  const [form, setForm] = useState({
    code: department?.code || '',
    name: department?.name || '',
    parentId: department?.parentId ?? '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((p) => ({ ...p, [name]: value }));
    setError('');
  };

  // Loại trừ chính nó và các con của nó khỏi danh sách cha
  const getValidParents = () => {
    if (!isEdit) return departments;
    const excludeIds = new Set();
    const collectDescendants = (id) => {
      excludeIds.add(id);
      departments.filter((d) => d.parentId === id).forEach((d) => collectDescendants(d.id));
    };
    collectDescendants(department.id);
    return departments.filter((d) => !excludeIds.has(d.id));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.code.trim()) { setError(t('ui.admin_departments.err_empty_code')); return; }
    if (!form.name.trim()) { setError(t('ui.admin_departments.err_empty_name')); return; }

    setLoading(true);
    try {
      const payload = {
        code: form.code.trim(),
        name: form.name.trim(),
        parentId: form.parentId !== '' ? Number(form.parentId) : null,
      };
      if (isEdit) {
        await updateDepartmentApi(department.id, payload);
        toast.success(t('ui.admin_departments.msg_updated'));
      } else {
        await createDepartmentApi(payload);
        toast.success(t('ui.admin_departments.msg_created'));
      }
      onSave();
    } catch (err) {
      setError(err?.response?.data?.message || (isEdit ? t('ui.admin_departments.err_update') : t('ui.admin_departments.err_create')));
    } finally { setLoading(false); }
  };

  const validParents = getValidParents();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Building2 className="size-5" />
            <h2 className="text-lg font-semibold">
              {isEdit ? t('ui.admin_departments.edit') : t('ui.admin_departments.add')}
            </h2>
          </div>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted">
            <X className="size-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
              {error}
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="code">{t('ui.admin_departments.code')} *</Label>
            <Input
              id="code"
              name="code"
              value={form.code}
              onChange={handleChange}
              placeholder={t('ui.admin_departments.code_placeholder')}
              className="uppercase"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="name">{t('ui.admin_departments.name')} *</Label>
            <Input
              id="name"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder={t('ui.admin_departments.name_placeholder')}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="parentId">{t('ui.admin_departments.parent')}</Label>
            <select
              id="parentId"
              name="parentId"
              value={form.parentId}
              onChange={handleChange}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring"
            >
              <option value="">{t('ui.admin_departments.parent_none')}</option>
              {validParents.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.code} — {d.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex gap-3 pt-2">
            <Button type="submit" disabled={loading} className="flex-1">
              {loading && <Loader2 className="size-4 mr-2 animate-spin" />}
              {t('ui.admin_departments.save')}
            </Button>
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">
              {t('ui.admin_departments.cancel')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Component hiển thị phòng ban (đã chuyển vào trong render để linh hoạt) ────

// ─── Main Page ─────────────────────────────────────────────────────────────────
export default function AdminDepartmentsPage() {
  const { t } = useTranslation();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null | 'add' | {edit: dept}

  const loadDepartments = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getDepartmentsApi();
      setDepartments(res.data?.data || []);
    } catch {
      toast.error(t('ui.admin_departments.err_load'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => { loadDepartments(); }, [loadDepartments]);

  const handleDelete = async (dept) => {
    const msg = t('ui.admin_departments.confirm_delete', { name: dept.name });
    if (!window.confirm(msg)) return;
    try {
      await deleteDepartmentApi(dept.id);
      toast.success(t('ui.admin_departments.msg_deleted'));
      loadDepartments();
    } catch (err) {
      toast.error(err?.response?.data?.message || t('ui.admin_departments.err_delete'));
    }
  };

  const handleSave = () => {
    setModal(null);
    loadDepartments();
  };

  // Lấy chỉ phòng ban cấp gốc (parentId = null) để render cây từ gốc
  const rootDepartments = departments.filter((d) => d.parentId == null);

  return (
    <div className="flex flex-col min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-10 flex h-14 shrink-0 items-center gap-3 border-b bg-background/95 backdrop-blur-sm px-4">
        <SidebarTrigger />
        <Separator orientation="vertical" className="h-4" />
        <div className="flex items-center gap-2">
          <Building2 className="size-4 text-muted-foreground" />
          <h1 className="text-sm font-semibold">{t('ui.admin_departments.title')}</h1>
        </div>
        <div className="ml-auto">
          <Button size="sm" onClick={() => setModal('add')} className="gap-1.5">
            <Plus className="size-4" />
            {t('ui.admin_departments.add')}
          </Button>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 p-4 md:p-6">
        <div className="max-w-4xl mx-auto">
          <div className="rounded-xl border bg-card shadow-sm overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center py-16">
                <Loader2 className="size-6 animate-spin text-muted-foreground" />
              </div>
            ) : departments.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-muted-foreground gap-2">
                <Building2 className="size-10 opacity-30" />
                <p className="text-sm">{t('ui.admin_departments.no_data')}</p>
                <Button variant="outline" size="sm" className="mt-2" onClick={() => setModal('add')}>
                  <Plus className="size-4 mr-1" />
                  {t('ui.admin_departments.add')}
                </Button>
              </div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground w-32">
                      {t('ui.admin_departments.col_code')}
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">
                      {t('ui.admin_departments.col_name')}
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">
                      {t('ui.admin_departments.col_parent')}
                    </th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground w-24">
                      {t('ui.admin_departments.col_actions')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rootDepartments.map((root) => {
                    const children = departments.filter((d) => d.parentId === root.id);

                    return (
                      <Fragment key={root.id}>
                        {/* Dòng hiển thị cho chính phòng ban cha (Khối) */}
                        <tr className="border-b hover:bg-muted/10 transition-colors bg-muted/5">
                          <td className="px-4 py-3">
                            <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300 whitespace-nowrap">
                              {root.code}
                            </span>
                          </td>
                          <td className="px-4 py-3 font-semibold text-foreground/90">{root.name}</td>
                          <td className="px-4 py-3 text-muted-foreground">—</td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2">
                              <button
                                onClick={() => setModal({ edit: root })}
                                className="rounded-lg p-1.5 hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                                title={t('ui.admin_departments.edit')}
                              >
                                <Pencil className="size-3.5" />
                              </button>
                              <button
                                onClick={() => handleDelete(root)}
                                className="rounded-lg p-1.5 hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-colors"
                                title={t('ui.admin_departments.delete')}
                              >
                                <Trash2 className="size-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>

                        {/* Các dòng hiển thị cho phòng ban con */}
                        {children.map((child) => (
                          <tr key={child.id} className="border-b hover:bg-muted/30 transition-colors bg-background">
                            <td className="px-4 py-3">
                              <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 whitespace-nowrap">
                                {child.code}
                              </span>
                            </td>
                            <td className="px-4 py-3">
                              {/* Cột Level 1 của con để trống để tạo cảm giác group */}
                            </td>
                            <td className="px-4 py-3">
                              <div className="flex items-center gap-2">
                                <span className="text-muted-foreground border-l-2 border-b-2 border-muted-foreground/30 w-3 h-3 mb-1 inline-block rounded-bl-sm"></span>
                                <span>{child.name}</span>
                              </div>
                            </td>
                            <td className="px-4 py-3">
                              <div className="flex items-center gap-2">
                                <button
                                  onClick={() => setModal({ edit: child })}
                                  className="rounded-lg p-1.5 hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                                  title={t('ui.admin_departments.edit')}
                                >
                                  <Pencil className="size-3.5" />
                                </button>
                                <button
                                  onClick={() => handleDelete(child)}
                                  className="rounded-lg p-1.5 hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-colors"
                                  title={t('ui.admin_departments.delete')}
                                >
                                  <Trash2 className="size-3.5" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>

      {/* Modals */}
      {modal === 'add' && (
        <DepartmentFormModal
          departments={departments}
          onClose={() => setModal(null)}
          onSave={handleSave}
        />
      )}
      {modal?.edit && (
        <DepartmentFormModal
          department={modal.edit}
          departments={departments}
          onClose={() => setModal(null)}
          onSave={handleSave}
        />
      )}
    </div>
  );
}
