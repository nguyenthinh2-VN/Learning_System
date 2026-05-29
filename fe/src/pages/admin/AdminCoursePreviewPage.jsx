import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  adminGetCourseDetailApi,
  adminGetSectionsApi,
  publishCourseApi,
} from '@/api/adminApi';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';
import {
  Users, BookOpen, ChevronDown, ArrowLeft, Calendar,
  PlayCircle, Loader2, CheckCircle2, Eye, Video,
} from 'lucide-react';

function formatPrice(price) {
  if (price === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(price ?? 0);
}

function formatDate(dateStr) {
  if (!dateStr) return null;
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export default function AdminCoursePreviewPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { adminUser } = useAuth();

  const [course, setCourse] = useState(null);
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [openSections, setOpenSections] = useState({});

  const fetchData = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [courseRes, sectionsRes] = await Promise.all([
        adminGetCourseDetailApi(id),
        adminGetSectionsApi(id),
      ]);
      setCourse(courseRes.data.data);
      const sectionsData = sectionsRes.data.data;
      setSections(Array.isArray(sectionsData) ? sectionsData : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải nội dung khóa học.');
    } finally { setLoading(false); }
  }, [id]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const toggleSection = (idx) =>
    setOpenSections((prev) => ({ ...prev, [idx]: !prev[idx] }));

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await publishCourseApi(id);
      navigate('/admin/courses/pending');
    } catch (err) {
      alert(err?.response?.data?.message || 'Duyệt xuất bản thất bại.');
    } finally { setPublishing(false); }
  };

  const sortedSections = [...sections].sort((a, b) => a.orderIndex - b.orderIndex);
  const totalLessons = sortedSections.reduce(
    (acc, s) => acc + (s.lessonCount ?? s.lessons?.length ?? 0), 0
  );
  const isPublished = !!course?.published || !!course?.publishedAt;

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex h-12 shrink-0 items-center gap-2 border-b bg-background/80 backdrop-blur px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 h-4" />
        <button
          onClick={() => navigate('/admin/courses/pending')}
          className="flex items-center gap-1.5 text-muted-foreground hover:text-foreground transition-colors text-sm"
        >
          <ArrowLeft className="size-3.5" />
          Chờ duyệt
        </button>
        <span className="text-muted-foreground">/</span>
        <span className="text-sm font-medium truncate max-w-xs">{course?.title || `Course #${id}`}</span>
        <span className="text-xs text-muted-foreground ml-auto">{adminUser?.name}</span>
      </header>

      <div className="p-6 max-w-4xl mx-auto space-y-6">
        {/* Preview banner */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 rounded-xl border border-amber-300 bg-amber-50 text-amber-900">
          <div className="flex items-center gap-2 text-sm font-medium">
            <Eye className="size-4" />
            Chế độ xem trước {isPublished ? '(đã xuất bản)' : '(chưa xuất bản)'}
          </div>
          {!isPublished && (
            <Button size="sm" onClick={handlePublish} disabled={publishing}>
              {publishing ? <Loader2 className="size-4 animate-spin mr-2" /> : <CheckCircle2 className="size-4 mr-2" />}
              Duyệt xuất bản
            </Button>
          )}
        </div>

        {error && (
          <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <div className="py-20 flex items-center justify-center">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : !course ? null : (
          <>
            {/* Hero */}
            <div className="grid md:grid-cols-3 gap-8">
              <div className="md:col-span-2 space-y-4">
                <h1 className="text-2xl font-bold leading-tight">{course.title}</h1>
                {course.description && (
                  <p className="text-muted-foreground leading-relaxed text-sm">{course.description}</p>
                )}
                <div className="flex flex-wrap gap-4">
                  <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Users className="size-4" />
                    {course.enrolledCount ?? 0}/{course.maxStudents ?? '?'} học viên
                  </span>
                  <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <BookOpen className="size-4" />
                    {sortedSections.length} chương · {totalLessons} bài học
                  </span>
                  {course.publishedAt && (
                    <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                      <Calendar className="size-4" />
                      Ngày đăng: {formatDate(course.publishedAt)}
                    </span>
                  )}
                </div>
              </div>

              {/* Right — info card (KHÔNG có nút mua/đăng ký public) */}
              <div className="border rounded-xl p-5 space-y-4 h-fit">
                <div className="aspect-video bg-muted rounded-lg flex items-center justify-center overflow-hidden">
                  {course.thumbnailUrl ? (
                    <img
                      src={course.thumbnailUrl}
                      alt={course.title}
                      className="w-full h-full object-cover"
                      onError={(e) => { e.target.style.display = 'none'; }}
                    />
                  ) : (
                    <BookOpen className="size-10 text-muted-foreground/20" />
                  )}
                </div>
                <p className="text-2xl font-bold">{formatPrice(course.price)}</p>
                <Badge variant="outline" className="w-fit">
                  {isPublished ? 'Đã xuất bản' : 'Chưa xuất bản'}
                </Badge>
              </div>
            </div>

            {/* Curriculum — admin xem đầy đủ section + lesson */}
            {sortedSections.length > 0 && (
              <div className="mt-2">
                <h2 className="text-lg font-semibold mb-1">Nội dung khóa học</h2>
                <p className="text-xs text-muted-foreground mb-4">
                  {sortedSections.length} chương · {totalLessons} bài học
                </p>
                <div className="border rounded-xl overflow-hidden divide-y">
                  {sortedSections.map((section, idx) => {
                    const lessons = [...(section.lessons ?? [])].sort(
                      (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)
                    );
                    const lessonCount = section.lessonCount ?? lessons.length;
                    const isOpen = !!openSections[idx];
                    return (
                      <div key={section.id ?? idx}>
                        <button
                          className="w-full flex items-center justify-between px-4 py-3.5 hover:bg-muted/40 transition-colors text-left"
                          onClick={() => toggleSection(idx)}
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <span className="font-medium text-sm truncate">{section.title}</span>
                            <Badge variant="outline" className="text-[10px] font-normal shrink-0">
                              {lessonCount} bài
                            </Badge>
                          </div>
                          <ChevronDown
                            className="size-4 text-muted-foreground shrink-0 ml-2 transition-transform duration-200"
                            style={{ transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}
                          />
                        </button>

                        {isOpen && (
                          <div className="bg-muted/20 px-4 py-3 space-y-1">
                            {lessons.length === 0 ? (
                              <p className="text-xs text-muted-foreground">Chưa có bài học.</p>
                            ) : (
                              lessons.map((lesson) => (
                                <div key={lesson.id} className="flex items-start gap-2 px-2 py-1.5">
                                  <Video className="size-3.5 text-muted-foreground mt-0.5 shrink-0" />
                                  <div className="min-w-0">
                                    <p className="text-sm leading-tight">{lesson.title}</p>
                                    {lesson.contentUrl && (
                                      <p className="text-xs text-muted-foreground truncate max-w-md">
                                        {lesson.contentUrl}
                                      </p>
                                    )}
                                  </div>
                                </div>
                              ))
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
