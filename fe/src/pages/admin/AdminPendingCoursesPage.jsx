import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { getPendingCoursesApi, publishCourseApi } from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  CheckCircle2, XCircle, Loader2, RefreshCw, ExternalLink,
} from 'lucide-react';

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(amount ?? 0);
}

export default function AdminPendingCoursesPage() {
  const { adminUser } = useAuth();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState({});

  const fetchCourses = async () => {
    setLoading(true); setError('');
    try {
      const res = await getPendingCoursesApi();
      const data = res.data.data;
      setCourses(data?.items ?? data?.content ?? (Array.isArray(data) ? data : []));
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách khóa học chờ duyệt.');
    } finally { setLoading(false); }
  };

  useEffect(() => { fetchCourses(); }, []);

  const handlePublish = async (course) => {
    setActionLoading((p) => ({ ...p, [course.id]: true }));
    try {
      await publishCourseApi(course.id);
      setCourses((prev) => prev.filter((c) => c.id !== course.id));
    } catch (err) {
      alert(err?.response?.data?.message || 'Duyệt thất bại.');
    } finally {
      setActionLoading((p) => ({ ...p, [course.id]: false }));
    }
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <span className="text-sm font-medium">Chờ duyệt</span>
        <span className="text-xs text-muted-foreground ml-auto">{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Khóa học chờ duyệt</h1>
            <p className="text-sm text-muted-foreground mt-1">{courses.length} khóa học đang chờ xét duyệt</p>
          </div>
          <Button variant="outline" size="sm" onClick={fetchCourses} disabled={loading}>
            <RefreshCw className={`size-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <XCircle className="size-4" /><span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : courses.length === 0 ? (
          <div className="py-20 text-center border border-dashed rounded-xl text-sm text-muted-foreground">
            <CheckCircle2 className="size-8 mx-auto mb-2 opacity-20" />
            Không có khóa học nào đang chờ duyệt.
          </div>
        ) : (
          <div className="rounded-xl border overflow-hidden divide-y">
            {courses.map((course) => (
              <div key={course.id} className="p-4 hover:bg-muted/30 transition-colors">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold line-clamp-1">{course.title}</p>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
                      {course.description || 'Không có mô tả.'}
                    </p>
                    <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
                      <span>ID: {course.id}</span>
                      <span>&bull;</span>
                      <span>{formatMoney(course.price)}</span>
                      <span>&bull;</span>
                      <span>Tối đa {course.maxStudents ?? '?'} học viên</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Button size="sm" variant="outline" onClick={() => window.open(`/courses/${course.id}`, '_blank')}>
                      <ExternalLink className="size-3 mr-1" />Xem trước
                    </Button>
                    <Button
                      size="sm"
                      id={`approve-course-${course.id}`}
                      onClick={() => handlePublish(course)}
                      disabled={!!actionLoading[course.id]}
                    >
                      {actionLoading[course.id] ? <Loader2 className="size-3 animate-spin mr-1" /> : <CheckCircle2 className="size-3 mr-1" />}
                      Duyệt xuất bản
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
