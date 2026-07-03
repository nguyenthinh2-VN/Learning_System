import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useTranslation } from 'react-i18next';
import { getPermissionMatrixApi, updateRolePermissionsApi } from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import { ShieldCheck, Loader2, RefreshCw, Save, AlertCircle, CheckCircle2, Lock, Undo2 } from 'lucide-react';
import { getRoleTextClass } from '@/lib/roleColors';
import { PERMISSION_LABELS, PERMISSION_GROUPS, ROLE_LABELS_PERM } from '@/lib/permissions';

export default function AdminPermissionsPage() {
  const { t } = useTranslation();
  const { adminUser } = useAuth();
  const [allPermissions, setAllPermissions] = useState([]);
  const [roles, setRoles] = useState([]); // [{ roleName, permissions: [], locked }]
  const [draft, setDraft] = useState({}); // { roleName: Set<permName> }
  const [loading, setLoading] = useState(true);
  const [savingRole, setSavingRole] = useState(null);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const fetchMatrix = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getPermissionMatrixApi();
      const data = res.data.data;
      setAllPermissions(data.allPermissions ?? []);
      setRoles(data.roles ?? []);
      const initialDraft = {};
      (data.roles ?? []).forEach((r) => {
        initialDraft[r.roleName] = new Set(r.permissions ?? []);
      });
      setDraft(initialDraft);
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_permissions.err_load'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMatrix();
  }, []);

  // Sắp xếp permission theo nhóm; permission lạ (không nằm trong nhóm nào) gom vào "Khác"
  const groupedPermissions = useMemo(() => {
    const known = new Set(allPermissions.map((p) => p.name));
    const groups = PERMISSION_GROUPS
      .map((g) => ({ label: g.label, permissions: g.permissions.filter((p) => known.has(p)) }))
      .filter((g) => g.permissions.length > 0);

    const grouped = new Set(groups.flatMap((g) => g.permissions));
    const others = allPermissions.map((p) => p.name).filter((p) => !grouped.has(p));
    if (others.length) groups.push({ label: t('ui.admin_permissions.other_group'), permissions: others });
    return groups;
  }, [allPermissions]);

  const toggle = (roleName, permName) => {
    setDraft((prev) => {
      const next = { ...prev };
      const set = new Set(next[roleName]);
      if (set.has(permName)) set.delete(permName);
      else set.add(permName);
      next[roleName] = set;
      return next;
    });
    setSuccessMsg('');
  };

  // Role nào có thay đổi so với dữ liệu gốc
  const dirtyRoles = useMemo(() => {
    const result = {};
    roles.forEach((r) => {
      const original = new Set(r.permissions ?? []);
      const current = draft[r.roleName] ?? new Set();
      const changed = original.size !== current.size
        || [...current].some((p) => !original.has(p));
      result[r.roleName] = changed;
    });
    return result;
  }, [roles, draft]);

  const anyDirty = Object.values(dirtyRoles).some(Boolean);

  const handleSave = async (roleName) => {
    setSavingRole(roleName);
    setError('');
    try {
      const perms = [...(draft[roleName] ?? new Set())];
      await updateRolePermissionsApi(roleName, perms);
      setSuccessMsg(t('ui.admin_permissions.msg_updated').replace('{role}', ROLE_LABELS_PERM[roleName] || roleName));
      setTimeout(() => setSuccessMsg(''), 4000);
      await fetchMatrix();
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_permissions.err_update'));
    } finally {
      setSavingRole(null);
    }
  };

  const handleReset = (roleName) => {
    const original = roles.find((r) => r.roleName === roleName);
    setDraft((prev) => ({ ...prev, [roleName]: new Set(original?.permissions ?? []) }));
    setSuccessMsg('');
  };

  const countFor = (roleName) => (draft[roleName]?.size ?? 0);

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">{t('ui.admin_permissions.title')}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-[1400px] mx-auto space-y-6">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold flex items-center gap-2">
              <ShieldCheck className="size-6" />Phân quyền động
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {t('ui.admin_permissions.subtitle')}
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={fetchMatrix} disabled={loading}>
            <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />Làm mới
          </Button>
        </div>

        {successMsg && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-500/10 text-emerald-600 border border-emerald-500/20">
            <CheckCircle2 className="size-4" /><span>{successMsg}</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <AlertCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="rounded-xl border overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="bg-muted/50 border-b">
                  <th className="text-left px-4 py-3 font-medium sticky left-0 bg-muted/50 min-w-[280px] z-10">
                    {t('ui.admin_permissions.col_permission')}
                  </th>
                  {roles.map((r) => (
                    <th key={r.roleName} className="px-3 py-3 font-medium text-center min-w-[140px]">
                      <div className="flex flex-col items-center gap-0.5">
                        <span className={getRoleTextClass(r.roleName)}>{ROLE_LABELS_PERM[r.roleName] || r.roleName}</span>
                        <span className="text-[10px] text-muted-foreground font-normal font-mono">{r.roleName}</span>
                        {r.locked ? (
                          <span className="inline-flex items-center gap-0.5 text-[10px] text-amber-600">
                            <Lock className="size-2.5" />full quyền
                          </span>
                        ) : (
                          <span className="text-[10px] text-muted-foreground">{t('ui.admin_permissions.rights_count').replace('{count}', countFor(r.roleName))}</span>
                        )}
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {groupedPermissions.map((group) => (
                  <GroupBlock
                    key={group.label}
                    group={group}
                    roles={roles}
                    draft={draft}
                    toggle={toggle}
                  />
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t-2 bg-muted/40">
                  <td className="px-4 py-3 sticky left-0 bg-muted/40 font-medium z-10">{t('ui.admin_permissions.save_changes')}</td>
                  {roles.map((r) => (
                    <td key={r.roleName} className="px-3 py-3 text-center align-top">
                      {r.locked ? (
                        <span className="text-xs text-muted-foreground">—</span>
                      ) : (
                        <div className="flex flex-col items-center gap-1.5">
                          <Button
                            size="sm"
                            className="w-full"
                            disabled={!dirtyRoles[r.roleName] || savingRole === r.roleName}
                            onClick={() => handleSave(r.roleName)}
                          >
                            {savingRole === r.roleName
                              ? <Loader2 className="size-3 animate-spin mr-1" />
                              : <Save className="size-3 mr-1" />}
                            {t('ui.admin_permissions.save')}
                          </Button>
                          {dirtyRoles[r.roleName] && (
                            <button
                              className="inline-flex items-center gap-1 text-[11px] text-muted-foreground hover:text-foreground"
                              onClick={() => handleReset(r.roleName)}
                            >
                              <Undo2 className="size-3" />Hoàn tác
                            </button>
                          )}
                        </div>
                      )}
                    </td>
                  ))}
                </tr>
              </tfoot>
            </table>
          </div>
        )}

        {anyDirty && !loading && (
          <p className="text-xs text-amber-600 flex items-center gap-1.5">
            <AlertCircle className="size-3.5" />
            {t('ui.admin_permissions.unsaved_changes')}
          </p>
        )}

        <p className="text-xs text-muted-foreground">
          <span dangerouslySetInnerHTML={{ __html: t('ui.admin_permissions.note') }} />
        </p>
      </div>
    </div>
  );
}

// Một nhóm permission: hàng tiêu đề nhóm + các hàng permission con
function GroupBlock({ group, roles, draft, toggle }) {
  return (
    <>
      <tr className="bg-muted/20 border-b">
        <td
          colSpan={1 + roles.length}
          className="px-4 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground sticky left-0"
        >
          {group.label}
        </td>
      </tr>
      {group.permissions.map((permName) => (
        <tr key={permName} className="border-b hover:bg-muted/30 transition-colors">
          <td className="px-4 py-2.5 sticky left-0 bg-background z-10">
            <div className="flex flex-col">
              <span className="font-medium">{PERMISSION_LABELS[permName] || permName}</span>
              <code className="text-[10px] text-muted-foreground font-mono">{permName}</code>
            </div>
          </td>
          {roles.map((r) => {
            const checked = draft[r.roleName]?.has(permName) ?? false;
            return (
              <td key={r.roleName} className="px-3 py-2.5 text-center">
                <input
                  type="checkbox"
                  className="size-4 accent-primary cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                  checked={r.locked ? true : checked}
                  disabled={r.locked}
                  onChange={() => toggle(r.roleName, permName)}
                  aria-label={`${permName} cho ${r.roleName}`}
                />
              </td>
            );
          })}
        </tr>
      ))}
    </>
  );
}
