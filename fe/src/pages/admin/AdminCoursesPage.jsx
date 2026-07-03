import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useTranslation } from 'react-i18next';
import {
  getAdminCoursesApi, getInstructorCoursesApi, publishCourseApi, unpublishCourseApi, updateCoursePriceApi,
  adminCreateCourseApi, adminUpdateCourseApi, adminDeleteCourseApi,
} from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { getRoleTextClass } from '@/lib/roleColors';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  CheckCircle2, XCircle, Loader2, Search, DollarSign,
  RefreshCw, EyeOff, X, Plus, Pencil, Trash2, LayoutList, AlertCircle,
} from 'lucide-react';

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(amount ?? 0);
}



// ─── Modal Sửa giá ───────────────────────────────────────
function PriceEditModal({ course, onClose, onSave }) {
  const { t } = useTranslation();
  const [price, setPrice] = useState(course.price ?? 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    setLoading(true);
    try {
      await updateCoursePriceApi(course.id, parseFloat(price));
      onSave();
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_courses.update_price_fail'));
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-sm mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">{t('ui.admin_courses.update_price')}</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted"><X className="size-4" /></button>
        </div>
        <p className="text-sm text-muted-foreground line-clamp-1">{course.title}</p>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <div className="space-y-1.5">
          <Label>{t('ui.admin_courses.new_price')}</Label>
          <Input type="number" value={price} onChange={(e) => setPrice(e.target.value)} min={0} />
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={onClose} className="flex-1">{t('ui.admin_courses.cancel')}</Button>
          <Button onClick={handleSave} disabled={loading} className="flex-1">
            {loading && <Loader2 className="size-4 animate-spin mr-2" />}Lưu
          </Button>
        </div>
      </div>
    </div>
  );
}

// ─── Modal Tạo / Sửa khóa học ────────────────────────────
function CourseFormModal({ course, onClose, onSave }) {
  const { t } = useTranslation();
  const isEdit = !!course;
  const [form, setForm] = useState({
    title: course?.title || '',
    description: course?.description || '',
    price: course?.price ?? '',
    maxStudents: course?.maxStudents ?? '',
    thumbnailUrl: course?.thumbnailUrl || '',
    requestedInstructorId: course?.instructorId ?? '',
    freeForInternal: course?.freeForInternal ?? false,
    isMandatory: course?.isMandatory ?? false,
    assignedDepartment: course?.assignedDepartment || '',
    mandatoryDeadline: course?.mandatoryDeadline ? new Date(course.mandatoryDeadline).toISOString().slice(0, 16) : '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((p) => ({ ...p, [name]: value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) { setError(t('ui.admin_courses.err_empty_title')); return; }
    if (form.price === '' || form.price === null || form.price === undefined || parseFloat(form.price) < 0) { setError(t('ui.admin_courses.err_invalid_price')); return; }
    if (!form.maxStudents || parseInt(form.maxStudents) <= 0) { setError(t('ui.admin_courses.err_invalid_students')); return; }

    setLoading(true);
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        price: parseFloat(form.price),
        maxStudents: parseInt(form.maxStudents),
        thumbnailUrl: form.thumbnailUrl.trim() || null,
        freeForInternal: form.freeForInternal,
        isMandatory: form.isMandatory,
        assignedDepartment: form.assignedDepartment.trim() || null,
        mandatoryDeadline: form.mandatoryDeadline ? new Date(form.mandatoryDeadline).toISOString() : null,
      };
      if (form.requestedInstructorId) {
        payload.requestedInstructorId = parseInt(form.requestedInstructorId);
      }

      if (isEdit) {
        await adminUpdateCourseApi(course.id, payload);
      } else {
        await adminCreateCourseApi(payload);
      }
      onSave();
    } catch (err) {
      setError(err?.response?.data?.message || isEdit ? t('ui.admin_courses.update_fail') : t('ui.admin_courses.create_fail'));
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 overflow-y-auto py-8">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-lg mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold">{isEdit ? t('ui.admin_courses.edit_course') : t('ui.admin_courses.create_course')}</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted"><X className="size-4" /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
              <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <Label>{t('ui.admin_courses.title_req')}</Label>
            <Input name="title" value={form.title} onChange={handleChange} placeholder={t('ui.admin_courses.title_placeholder')} />
          </div>

          <div className="space-y-1.5">
            <Label>{t('ui.admin_courses.desc')}</Label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              placeholder={t('ui.admin_courses.desc_placeholder')}
              rows={3}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <Label>{t('ui.admin_courses.thumbnail_url')}</Label>
            <Input name="thumbnailUrl" value={form.thumbnailUrl} onChange={handleChange} placeholder={t('ui.admin_courses.thumbnail_placeholder')} />
            {form.thumbnailUrl && (
              <img src={form.thumbnailUrl} alt="preview" className="mt-2 h-24 w-full object-contain p-2 rounded-lg border border-border bg-muted/30" onError={(e) => e.target.style.display = 'none'} />
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>{t('ui.admin_courses.price_req')}</Label>
              <Input name="price" type="number" value={form.price} onChange={handleChange} placeholder={t('ui.admin_courses.price_placeholder')} min={0} />
            </div>
            <div className="space-y-1.5">
              <Label>{t('ui.admin_courses.max_students_req')}</Label>
              <Input name="maxStudents" type="number" value={form.maxStudents} onChange={handleChange} placeholder={t('ui.admin_courses.max_students_placeholder')} min={1} />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>{t('ui.admin_courses.instructor_id')} <span className="text-muted-foreground text-xs">{t('ui.admin_courses.instructor_id_desc')}</span></Label>
            <Input name="requestedInstructorId" type="number" value={form.requestedInstructorId} onChange={handleChange} placeholder={t('ui.admin_courses.instructor_id_placeholder')} min={1} />
          </div>

          <label className="flex items-start gap-3 rounded-lg border border-border p-3 cursor-pointer hover:bg-muted/40 transition-colors">
            <input
              type="checkbox"
              name="freeForInternal"
              checked={form.freeForInternal}
              onChange={(e) => setForm((p) => ({ ...p, freeForInternal: e.target.checked }))}
              className="mt-0.5 size-4 accent-emerald-500"
            />
            <span>
              <span className="text-sm font-medium">{t('ui.admin_courses.free_internal')}</span>
              <span className="block text-xs text-muted-foreground mt-0.5">
                Khi bật, tài khoản nội bộ học khóa này miễn phí. Học viên thường vẫn trả giá như bình thường.
              </span>
            </span>
          </label>

          <div className="border border-border rounded-lg p-3 space-y-3 bg-muted/20">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                name="isMandatory"
                checked={form.isMandatory}
                onChange={(e) => setForm((p) => ({ ...p, isMandatory: e.target.checked }))}
                className="mt-0.5 size-4 accent-emerald-500"
              />
              <span>
                <span className="text-sm font-medium">{t('ui.admin_courses.mandatory_course')}</span>
                <span className="block text-xs text-muted-foreground mt-0.5">
                  Bật tùy chọn này để yêu cầu thành viên của phòng ban cụ thể phải học khóa này.
                </span>
              </span>
            </label>

            {form.isMandatory && (
              <div className="grid grid-cols-2 gap-4 pt-2 border-t border-border/50">
                <div className="space-y-1.5">
                  <Label>{t('ui.admin_courses.assign_dept')}</Label>
                  <Input name="assignedDepartment" value={form.assignedDepartment} onChange={handleChange} placeholder={t('ui.admin_courses.assign_dept_placeholder')} />
                </div>
                <div className="space-y-1.5">
                  <Label>{t('ui.admin_courses.deadline')}</Label>
                  <Input name="mandatoryDeadline" type="datetime-local" value={form.mandatoryDeadline} onChange={handleChange} />
                </div>
              </div>
            )}
          </div>

          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">{t('ui.admin_courses.cancel')}</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading && <Loader2 className="size-4 animate-spin mr-2" />}
              {isEdit ? t('ui.admin_courses.save_changes') : t('ui.admin_courses.create_btn')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function AdminCoursesPage() {
  const { t } = useTranslation();
  const STATUS_CONFIG = {
    PUBLISHED: { label: t('ui.admin_courses.status_published'), variant: 'secondary', className: 'bg-emerald-100 text-emerald-700' },
    DRAFT: { label: t('ui.admin_courses.status_draft'), variant: 'secondary' },
    PENDING_REVIEW: { label: t('ui.admin_courses.status_pending'), variant: 'outline' },
  };

  const { adminUser } = useAuth();
  const role = adminUser?.role;

  // INSTRUCTOR chỉ có CREATE/EDIT/DELETE course, không có PUBLISH/LOCK_PRICE
  const canPublish = ['STAFF', 'SUPER_ADMIN'].includes(role);
  const canEditPrice = ['STAFF', 'SUPER_ADMIN', 'ADMIN_USER'].includes(role);
  // ADMIN_USER không có CREATE_SECTION/EDIT_SECTION nên ẩn nút Nội dung
  const canManageContent = ['INSTRUCTOR', 'STAFF', 'SUPER_ADMIN'].includes(role);

  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [editCourse, setEditCourse] = useState(null);       // modal sửa giá
  const [formCourse, setFormCourse] = useState(null);       // modal tạo/sửa course (null = đóng, false = tạo mới, object = sửa)
  const [actionLoading, setActionLoading] = useState({});
  const [deleteLoading, setDeleteLoading] = useState({});
  const [successMsg, setSuccessMsg] = useState('');

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(''), 4000);
  };

  const fetchCourses = async () => {
    setLoading(true); setError('');
    try {
      // INSTRUCTOR dùng endpoint riêng — chỉ thấy course của mình
      const res = role === 'INSTRUCTOR'
        ? await getInstructorCoursesApi({ size: 1000 })
        : await getAdminCoursesApi({ size: 1000 });
      const data = res.data.data;
      setCourses(data?.items ?? data?.content ?? (Array.isArray(data) ? data : []));
    } catch (err) {
      setError(err?.response?.data?.message || t('ui.admin_courses.err_load_fail'));
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchCourses(); }, []);

  const handlePublish = async (course) => {
    setActionLoading((p) => ({ ...p, [course.id]: 'publish' }));
    try { await publishCourseApi(course.id); await fetchCourses(); showSuccess(`${t('ui.admin_courses.msg_approved')} ${course.title}`); }
    catch (err) { setError(err?.response?.data?.message || t('ui.admin_courses.err_approve_fail')); }
    finally { setActionLoading((p) => ({ ...p, [course.id]: null })); }
  };

  const handleUnpublish = async (course) => {
    setActionLoading((p) => ({ ...p, [course.id]: 'unpublish' }));
    try { await unpublishCourseApi(course.id); await fetchCourses(); showSuccess(`${t('ui.admin_courses.msg_hidden')} ${course.title}`); }
    catch (err) { setError(err?.response?.data?.message || t('ui.admin_courses.err_hide_fail')); }
    finally { setActionLoading((p) => ({ ...p, [course.id]: null })); }
  };

  const handleDelete = async (course) => {
    if (!confirm(t('ui.admin_courses.confirm_delete').replace('{title}', course.title))) return;
    setDeleteLoading((p) => ({ ...p, [course.id]: true }));
    try {
      await adminDeleteCourseApi(course.id);
      setCourses((prev) => prev.filter((c) => c.id !== course.id));
      showSuccess(`${t('ui.admin_courses.msg_deleted')} ${course.title}`);
    } catch (err) { setError(err?.response?.data?.message || t('ui.admin_courses.err_delete_fail')); }
    finally { setDeleteLoading((p) => ({ ...p, [course.id]: false })); }
  };

  const getStatus = (c) => {
    return c.published === true ? 'PUBLISHED' : 'PENDING_REVIEW';
  };

  const filtered = courses.filter((c) => {
    let matchFilter = false;
    if (filter === 'ALL') matchFilter = true;
    else if (filter === 'PUBLISHED') matchFilter = c.published === true;
    else if (filter === 'PENDING_REVIEW') matchFilter = c.published === false;
    else if (filter === 'MANDATORY') matchFilter = c.isMandatory === true;

    return matchFilter && (!search || c.title?.toLowerCase().includes(search.toLowerCase()));
  });

  useEffect(() => {
    setCurrentPage(1);
  }, [filter, search]);

  const itemsPerPage = 10;
  const totalPages = Math.ceil(filtered.length / itemsPerPage);
  const paginatedCourses = filtered.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">{t('ui.admin_courses.manage_courses')}</span>
        <span className={`text-xs font-medium ml-auto ${getRoleTextClass(adminUser?.role)}`}>{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-7xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">{t('ui.admin_courses.manage_courses')}</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {role === 'INSTRUCTOR' ? t('ui.admin_courses.instructor_courses_desc') : t('ui.admin_courses.all_courses_desc')}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={fetchCourses} disabled={loading}>
              <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />{t('ui.admin_courses.refresh', 'Làm mới')}
            </Button>
            <Button size="sm" onClick={() => setFormCourse(false)}>
              <Plus className="size-4 mr-2" />{t('ui.admin_courses.create_course')}
            </Button>
          </div>
        </div>

        {/* Filter bar */}
        <div className="flex items-center gap-3 flex-wrap">
          <div className="relative flex-1 min-w-48">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input placeholder={t('ui.admin_courses.search_placeholder')} value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
          </div>
          <div className="flex items-center gap-1 border border-border rounded-lg p-1">
            {[
              { value: 'ALL', label: t('ui.admin_courses.filter_all') },
              { value: 'PUBLISHED', label: t('ui.admin_courses.status_published') },
              { value: 'PENDING_REVIEW', label: t('ui.admin_courses.status_pending') },
              { value: 'MANDATORY', label: t('ui.admin_courses.filter_mandatory') },
            ].map((f) => (
              <button
                key={f.value}
                onClick={() => setFilter(f.value)}
                className={`text-xs px-3 py-1.5 rounded-md transition-colors ${filter === f.value ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
                  }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {successMsg && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="size-4" /><span>{successMsg}</span>
          </div>
        )}
        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <XCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {/* Table */}
        <div className="rounded-xl border border-border overflow-hidden">
          <div className="bg-muted/50 px-4 py-3 border-b border-border grid grid-cols-12 gap-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
            <span className="col-span-4">{t('ui.admin_courses.col_course')}</span>
            <span className="col-span-2">{t('ui.admin_courses.col_status')}</span>
            <span className="col-span-2">{t('ui.admin_courses.col_price')}</span>
            <span className="col-span-1">{t('ui.admin_courses.col_students')}</span>
            <span className="col-span-3">{t('ui.admin_courses.col_actions')}</span>
          </div>

          {loading ? (
            <div className="py-20 flex items-center justify-center">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : paginatedCourses.length === 0 ? (
            <div className="py-20 text-center text-sm text-muted-foreground">{t('ui.admin_courses.no_courses')}</div>
          ) : (
            <div className="divide-y divide-border">
              {paginatedCourses.map((course) => {
                const status = getStatus(course);
                const st = STATUS_CONFIG[status] || { label: status, variant: 'secondary' };
                return (
                  <div key={course.id} className="grid grid-cols-12 gap-3 px-4 py-3.5 items-center hover:bg-muted/20 transition-colors">
                    <div className="col-span-4 flex items-center gap-2">
                      <div>
                        <p className="text-sm font-medium line-clamp-1 flex items-center gap-2">
                          {course.title}
                          {course.isMandatory && (
                            <span className="px-1.5 py-0.5 rounded-md bg-emerald-100 text-emerald-700 text-[10px] font-bold uppercase tracking-wider whitespace-nowrap">
                              {t('ui.admin_courses.mandatory_tag', 'Bắt buộc')}
                            </span>
                          )}
                        </p>
                        <p className="text-xs text-muted-foreground mt-0.5">ID: {course.id}</p>
                      </div>
                    </div>
                    <div className="col-span-2">
                      <Badge variant={st.variant} className={st.className}>{st.label}</Badge>
                    </div>
                    <div className="col-span-2">
                      <p className="text-sm font-medium">{course.priceLocked ? '🔒 ' : ''}{formatMoney(course.price)}</p>
                    </div>
                    <div className="col-span-1">
                      <p className="text-sm text-muted-foreground">
                        {course.enrolledCount ?? course.currentStudents ?? 0}/{course.maxStudents ?? '?'}
                      </p>
                    </div>
                    <div className="col-span-3 flex items-center gap-1.5 flex-wrap">
                      {/* Sửa giá — STAFF, SUPER_ADMIN, ADMIN_USER */}
                      {canEditPrice && (
                        <Button size="sm" variant="outline" onClick={() => setEditCourse(course)} title={t('ui.admin_courses.action_edit_price')}>
                          <DollarSign className="size-3" />
                        </Button>
                      )}
                      {/* Sửa thông tin — tất cả có EDIT_COURSE */}
                      <Button size="sm" variant="outline" onClick={() => setFormCourse(course)} title={t('ui.admin_courses.action_edit_course')}>
                        <Pencil className="size-3" />
                      </Button>
                      {/* Nội dung (Section/Lesson) — INSTRUCTOR, STAFF, SUPER_ADMIN */}
                      {canManageContent && (
                        <Button size="sm" variant="outline" asChild title={t('ui.admin_courses.action_manage_content')}>
                          <Link to={`/admin/courses/${course.id}/content`}>
                            <LayoutList className="size-3" />
                          </Link>
                        </Button>
                      )}
                      {/* Duyệt / Ẩn — STAFF, SUPER_ADMIN */}
                      {canPublish && status !== 'PUBLISHED' && (
                        <Button size="sm" onClick={() => handlePublish(course)} disabled={!!actionLoading[course.id]} title={t('ui.admin_courses.action_approve')}>
                          {actionLoading[course.id] === 'publish'
                            ? <Loader2 className="size-3 animate-spin" />
                            : <CheckCircle2 className="size-3" />}
                        </Button>
                      )}
                      {canPublish && status === 'PUBLISHED' && (
                        <Button size="sm" variant="outline" onClick={() => handleUnpublish(course)} disabled={!!actionLoading[course.id]} title={t('ui.admin_courses.action_hide')}>
                          {actionLoading[course.id] === 'unpublish'
                            ? <Loader2 className="size-3 animate-spin" />
                            : <EyeOff className="size-3" />}
                        </Button>
                      )}
                      {/* Xóa — tất cả có DELETE_COURSE */}
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => handleDelete(course)}
                        disabled={!!deleteLoading[course.id]}
                        title={t('ui.admin_courses.action_delete')}
                      >
                        {deleteLoading[course.id] ? <Loader2 className="size-3 animate-spin" /> : <Trash2 className="size-3" />}
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-4 pb-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
              >
                {t('ui.admin_courses.prev', 'Trước')}
              </Button>
              <span className="text-sm font-medium">
                {t('ui.admin_courses.page_info').replace('{currentPage}', currentPage).replace('{totalPages}', totalPages)}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
              >
                {t('ui.admin_courses.next', 'Sau')}
              </Button>
            </div>
          )}
        </div>

        {/* Legend */}
        <p className="text-xs text-muted-foreground">
          {t('ui.admin_courses.legend')}
        </p>
      </div>

      {/* Modals */}
      {editCourse && (
        <PriceEditModal
          course={editCourse}
          onClose={() => setEditCourse(null)}
          onSave={() => { setEditCourse(null); fetchCourses(); showSuccess(t('ui.admin_courses.msg_price_updated')); }}
        />
      )}
      {formCourse !== null && (
        <CourseFormModal
          course={formCourse || null}
          onClose={() => setFormCourse(null)}
          onSave={() => { setFormCourse(null); fetchCourses(); showSuccess(formCourse ? t('ui.admin_courses.msg_course_updated') : t('ui.admin_courses.msg_course_created')); }}
        />
      )}
    </div>
  );
}
