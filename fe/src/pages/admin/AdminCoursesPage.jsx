import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  getAdminCoursesApi, getInstructorCoursesApi, publishCourseApi, unpublishCourseApi, updateCoursePriceApi,
  adminCreateCourseApi, adminUpdateCourseApi, adminDeleteCourseApi,
} from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
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

const STATUS_CONFIG = {
  PUBLISHED: { label: 'Đã xuất bản', variant: 'default' },
  DRAFT: { label: 'Bản nháp', variant: 'secondary' },
  PENDING_REVIEW: { label: 'Chờ duyệt', variant: 'outline' },
};

// ─── Modal Sửa giá ───────────────────────────────────────
function PriceEditModal({ course, onClose, onSave }) {
  const [price, setPrice] = useState(course.price ?? 0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    setLoading(true);
    try {
      await updateCoursePriceApi(course.id, parseFloat(price));
      onSave();
    } catch (err) {
      setError(err?.response?.data?.message || 'Cập nhật giá thất bại.');
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-sm mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Cập nhật giá</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted"><X className="size-4" /></button>
        </div>
        <p className="text-sm text-muted-foreground line-clamp-1">{course.title}</p>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <div className="space-y-1.5">
          <Label>Giá mới (VNĐ)</Label>
          <Input type="number" value={price} onChange={(e) => setPrice(e.target.value)} min={0} />
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
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
  const isEdit = !!course;
  const [form, setForm] = useState({
    title: course?.title || '',
    description: course?.description || '',
    price: course?.price ?? '',
    maxStudents: course?.maxStudents ?? '',
    thumbnailUrl: course?.thumbnailUrl || '',
    requestedInstructorId: course?.instructorId ?? '',
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
    if (!form.title.trim()) { setError('Tiêu đề không được để trống.'); return; }
    if (!form.price || parseFloat(form.price) < 0) { setError('Giá không hợp lệ.'); return; }
    if (!form.maxStudents || parseInt(form.maxStudents) <= 0) { setError('Số học viên tối đa phải > 0.'); return; }

    setLoading(true);
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        price: parseFloat(form.price),
        maxStudents: parseInt(form.maxStudents),
        thumbnailUrl: form.thumbnailUrl.trim() || null,
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
      setError(err?.response?.data?.message || `${isEdit ? 'Cập nhật' : 'Tạo'} khóa học thất bại.`);
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 overflow-y-auto py-8">
      <div className="bg-card rounded-2xl border shadow-xl w-full max-w-lg mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold">{isEdit ? 'Sửa khóa học' : 'Tạo khóa học mới'}</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-muted"><X className="size-4" /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
              <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
            </div>
          )}

          <div className="space-y-1.5">
            <Label>Tiêu đề *</Label>
            <Input name="title" value={form.title} onChange={handleChange} placeholder="VD: Spring Boot Clean Architecture" />
          </div>

          <div className="space-y-1.5">
            <Label>Mô tả</Label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              placeholder="Mô tả ngắn về khóa học..."
              rows={3}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <Label>URL ảnh thumbnail</Label>
            <Input name="thumbnailUrl" value={form.thumbnailUrl} onChange={handleChange} placeholder="https://example.com/image.jpg" />
            {form.thumbnailUrl && (
              <img src={form.thumbnailUrl} alt="preview" className="mt-2 h-24 w-full object-cover rounded-lg border border-border" onError={(e) => e.target.style.display='none'} />
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Giá (VNĐ) *</Label>
              <Input name="price" type="number" value={form.price} onChange={handleChange} placeholder="VD: 500000" min={0} />
            </div>
            <div className="space-y-1.5">
              <Label>Số học viên tối đa *</Label>
              <Input name="maxStudents" type="number" value={form.maxStudents} onChange={handleChange} placeholder="VD: 100" min={1} />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>ID Giảng viên <span className="text-muted-foreground text-xs">(để trống = dùng tài khoản hiện tại)</span></Label>
            <Input name="requestedInstructorId" type="number" value={form.requestedInstructorId} onChange={handleChange} placeholder="VD: 2" min={1} />
          </div>

          <div className="flex gap-2 pt-2">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading && <Loader2 className="size-4 animate-spin mr-2" />}
              {isEdit ? 'Lưu thay đổi' : 'Tạo khóa học'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function AdminCoursesPage() {
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
        ? await getInstructorCoursesApi()
        : await getAdminCoursesApi();
      const data = res.data.data;
      setCourses(data?.items ?? data?.content ?? (Array.isArray(data) ? data : []));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách khóa học.');
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchCourses(); }, []);

  const handlePublish = async (course) => {
    setActionLoading((p) => ({ ...p, [course.id]: 'publish' }));
    try { await publishCourseApi(course.id); await fetchCourses(); showSuccess(`Đã duyệt: ${course.title}`); }
    catch (err) { setError(err?.response?.data?.message || 'Duyệt thất bại.'); }
    finally { setActionLoading((p) => ({ ...p, [course.id]: null })); }
  };

  const handleUnpublish = async (course) => {
    setActionLoading((p) => ({ ...p, [course.id]: 'unpublish' }));
    try { await unpublishCourseApi(course.id); await fetchCourses(); showSuccess(`Đã ẩn: ${course.title}`); }
    catch (err) { setError(err?.response?.data?.message || 'Ẩn khóa học thất bại.'); }
    finally { setActionLoading((p) => ({ ...p, [course.id]: null })); }
  };

  const handleDelete = async (course) => {
    if (!confirm(`Xóa khóa học "${course.title}"? Hành động này không thể hoàn tác.`)) return;
    setDeleteLoading((p) => ({ ...p, [course.id]: true }));
    try {
      await adminDeleteCourseApi(course.id);
      setCourses((prev) => prev.filter((c) => c.id !== course.id));
      showSuccess(`Đã xóa: ${course.title}`);
    } catch (err) { setError(err?.response?.data?.message || 'Xóa khóa học thất bại.'); }
    finally { setDeleteLoading((p) => ({ ...p, [course.id]: false })); }
  };

  const getStatus = (c) => {
    if (c.published === true || c.status === 'PUBLISHED') return 'PUBLISHED';
    if (c.status === 'PENDING_REVIEW') return 'PENDING_REVIEW';
    return 'DRAFT';
  };

  const filtered = courses.filter((c) => {
    const st = getStatus(c);
    return (filter === 'ALL' || st === filter) && (!search || c.title?.toLowerCase().includes(search.toLowerCase()));
  });

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">Quản lý khóa học</span>
        <span className="text-xs text-muted-foreground ml-auto">{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-7xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Quản lý khóa học</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {role === 'INSTRUCTOR' ? 'Khóa học của bạn' : 'Tất cả khóa học trong hệ thống'}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={fetchCourses} disabled={loading}>
              <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />Làm mới
            </Button>
            <Button size="sm" onClick={() => setFormCourse(false)}>
              <Plus className="size-4 mr-2" />Tạo khóa học
            </Button>
          </div>
        </div>

        {/* Filter bar */}
        <div className="flex items-center gap-3 flex-wrap">
          <div className="relative flex-1 min-w-48">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input placeholder="Tìm khóa học..." value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
          </div>
          <div className="flex items-center gap-1 border border-border rounded-lg p-1">
            {[
              { value: 'ALL', label: 'Tất cả' },
              { value: 'PUBLISHED', label: 'Đã xuất bản' },
              { value: 'PENDING_REVIEW', label: 'Chờ duyệt' },
              { value: 'DRAFT', label: 'Bản nháp' },
            ].map((f) => (
              <button
                key={f.value}
                onClick={() => setFilter(f.value)}
                className={`text-xs px-3 py-1.5 rounded-md transition-colors ${
                  filter === f.value ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
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
            <span className="col-span-4">Khóa học</span>
            <span className="col-span-2">Trạng thái</span>
            <span className="col-span-2">Giá</span>
            <span className="col-span-1">HV</span>
            <span className="col-span-3">Thao tác</span>
          </div>

          {loading ? (
            <div className="py-20 flex items-center justify-center">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="py-20 text-center text-sm text-muted-foreground">Không có khóa học nào.</div>
          ) : (
            <div className="divide-y divide-border">
              {filtered.map((course) => {
                const status = getStatus(course);
                const st = STATUS_CONFIG[status] || { label: status, variant: 'secondary' };
                return (
                  <div key={course.id} className="grid grid-cols-12 gap-3 px-4 py-3.5 items-center hover:bg-muted/20 transition-colors">
                    <div className="col-span-4">
                      <p className="text-sm font-medium line-clamp-1">{course.title}</p>
                      <p className="text-xs text-muted-foreground mt-0.5">ID: {course.id}</p>
                    </div>
                    <div className="col-span-2">
                      <Badge variant={st.variant}>{st.label}</Badge>
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
                        <Button size="sm" variant="outline" onClick={() => setEditCourse(course)} title="Sửa giá">
                          <DollarSign className="size-3" />
                        </Button>
                      )}
                      {/* Sửa thông tin — tất cả có EDIT_COURSE */}
                      <Button size="sm" variant="outline" onClick={() => setFormCourse(course)} title="Sửa khóa học">
                        <Pencil className="size-3" />
                      </Button>
                      {/* Nội dung (Section/Lesson) — INSTRUCTOR, STAFF, SUPER_ADMIN */}
                      {canManageContent && (
                        <Button size="sm" variant="outline" asChild title="Quản lý nội dung">
                          <Link to={`/admin/courses/${course.id}/content`}>
                            <LayoutList className="size-3" />
                          </Link>
                        </Button>
                      )}
                      {/* Duyệt / Ẩn — STAFF, SUPER_ADMIN */}
                      {canPublish && status !== 'PUBLISHED' && (
                        <Button size="sm" onClick={() => handlePublish(course)} disabled={!!actionLoading[course.id]} title="Duyệt">
                          {actionLoading[course.id] === 'publish'
                            ? <Loader2 className="size-3 animate-spin" />
                            : <CheckCircle2 className="size-3" />}
                        </Button>
                      )}
                      {canPublish && status === 'PUBLISHED' && (
                        <Button size="sm" variant="outline" onClick={() => handleUnpublish(course)} disabled={!!actionLoading[course.id]} title="Ẩn">
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
                        title="Xóa"
                      >
                        {deleteLoading[course.id] ? <Loader2 className="size-3 animate-spin" /> : <Trash2 className="size-3" />}
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Legend */}
        <p className="text-xs text-muted-foreground">
          💡 Icon: <span className="font-mono">$</span> = Sửa giá &nbsp;|&nbsp;
          <span className="font-mono">✏</span> = Sửa thông tin &nbsp;|&nbsp;
          <span className="font-mono">☰</span> = Quản lý nội dung &nbsp;|&nbsp;
          <span className="font-mono">✓</span> = Duyệt &nbsp;|&nbsp;
          <span className="font-mono">👁</span> = Ẩn &nbsp;|&nbsp;
          <span className="font-mono">🗑</span> = Xóa
        </p>
      </div>

      {/* Modals */}
      {editCourse && (
        <PriceEditModal
          course={editCourse}
          onClose={() => setEditCourse(null)}
          onSave={() => { setEditCourse(null); fetchCourses(); showSuccess('Đã cập nhật giá.'); }}
        />
      )}
      {formCourse !== null && (
        <CourseFormModal
          course={formCourse || null}
          onClose={() => setFormCourse(null)}
          onSave={() => { setFormCourse(null); fetchCourses(); showSuccess(formCourse ? 'Đã cập nhật khóa học.' : 'Đã tạo khóa học mới.'); }}
        />
      )}
    </div>
  );
}
