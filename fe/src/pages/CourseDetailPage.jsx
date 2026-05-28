import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useCourseDetailStore from '@/store/useCourseDetailStore';
import useEnrollmentStore from '@/store/useEnrollmentStore';
import { useAuth } from '@/context/AuthContext';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Users, BookOpen, ChevronDown, ArrowLeft,
  DollarSign, Calendar, LogIn, PlayCircle,
} from 'lucide-react';

function formatPrice(price) {
  if (price === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(price);
}

function formatDate(dateStr) {
  if (!dateStr) return null;
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export default function CourseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isPublicAuthenticated } = useAuth();
  const { currentCourse, loading, error, fetchCourseDetail, invalidateCache } = useCourseDetailStore();
  const { isEnrolled, fetchEnrollments, initialized: enrollInitialized } = useEnrollmentStore();

  const [openSections, setOpenSections] = useState({});

  // Fetch course (public — không có lessons)
  useEffect(() => {
    if (id) {
      invalidateCache(Number(id));
      fetchCourseDetail(Number(id));
    }
  }, [id, fetchCourseDetail, invalidateCache]);

  // Fetch enrollments nếu đã login (để biết đã mua chưa)
  useEffect(() => {
    if (isPublicAuthenticated && !enrollInitialized) {
      fetchEnrollments();
    }
  }, [isPublicAuthenticated, enrollInitialized, fetchEnrollments]);

  const toggleSection = (idx) =>
    setOpenSections((prev) => ({ ...prev, [idx]: !prev[idx] }));

  const courseId = Number(id);
  const enrolled = isPublicAuthenticated && isEnrolled(courseId);

  const sortedSections = currentCourse?.sections
    ? [...currentCourse.sections].sort((a, b) => a.orderIndex - b.orderIndex)
    : [];

  const totalLessons = sortedSections.reduce(
    (acc, s) => acc + (s.lessonCount ?? s.lessons?.length ?? 0), 0
  );

  // ── Loading ───────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-6 py-10 space-y-6">
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <div className="grid grid-cols-3 gap-4 mt-6">
          {[1, 2, 3].map((i) => <Skeleton key={i} className="h-20 rounded-lg" />)}
        </div>
        <Skeleton className="h-64 rounded-lg mt-6" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto px-6 py-20 text-center">
        <BookOpen className="size-12 mx-auto mb-4 text-muted-foreground/30" />
        <p className="text-muted-foreground text-sm">{error}</p>
        <Button variant="outline" size="sm" className="mt-4" onClick={() => navigate('/')}>
          <ArrowLeft className="size-4 mr-1" /> Về trang chủ
        </Button>
      </div>
    );
  }

  if (!currentCourse) return null;

  const progress = currentCourse.maxStudents > 0
    ? Math.round((currentCourse.enrolledCount / currentCourse.maxStudents) * 100) : 0;
  const spotsLeft = currentCourse.maxStudents - currentCourse.enrolledCount;
  const isFull = spotsLeft <= 0;

  // ── Action button (3 trạng thái) ──────────────────────────────────────────
  const renderActionButton = () => {
    if (!isPublicAuthenticated) {
      return (
        <Button className="w-full" size="lg" onClick={() => navigate('/login')}>
          <LogIn className="size-4 mr-1" /> Đăng nhập để đăng ký
        </Button>
      );
    }
    if (enrolled) {
      return (
        <Button className="w-full" size="lg" onClick={() => navigate(`/my-courses/${courseId}`)}>
          <PlayCircle className="size-4 mr-1" /> Vào học ngay
        </Button>
      );
    }
    if (isFull) {
      return (
        <div className="w-full text-center py-2 px-4 rounded-lg bg-muted text-muted-foreground text-sm font-medium">
          Hết chỗ
        </div>
      );
    }
    return (
      <Button id="course-enroll-btn" className="w-full" size="lg"
        onClick={() => navigate(`/courses/${courseId}/purchase`)}>
        <DollarSign className="size-4 mr-1" /> Đăng ký học
      </Button>
    );
  };

  return (
    <div className="max-w-4xl mx-auto px-6 py-10">
      {/* Back */}
      <button onClick={() => navigate(-1)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-6">
        <ArrowLeft className="size-4" /> Quay lại
      </button>

      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <div className="grid md:grid-cols-3 gap-8">
        {/* Left */}
        <div className="md:col-span-2 space-y-4">
          <h1 className="text-2xl font-bold leading-tight">{currentCourse.title}</h1>
          {enrolled && (
            <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 hover:bg-emerald-100 w-fit">
              <PlayCircle className="size-3 mr-1" /> Đã đăng ký
            </Badge>
          )}
          {currentCourse.description && (
            <p className="text-muted-foreground leading-relaxed text-sm">
              {currentCourse.description}
            </p>
          )}
          <div className="flex flex-wrap gap-4">
            <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
              <Users className="size-4" />
              {currentCourse.enrolledCount}/{currentCourse.maxStudents} học viên
            </span>
            <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
              <BookOpen className="size-4" />
              {sortedSections.length} chương · {totalLessons} bài học
            </span>
            {currentCourse.publishedAt && (
              <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                <Calendar className="size-4" />
                Ngày đăng: {formatDate(currentCourse.publishedAt)}
              </span>
            )}
          </div>

          {/* Progress */}
          <div className="space-y-1">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>Đã đăng ký</span><span>{progress}%</span>
            </div>
            <div className="h-2 bg-muted rounded-full overflow-hidden">
              <div className="h-full bg-foreground/70 rounded-full transition-all duration-300"
                style={{ width: `${Math.min(progress, 100)}%` }} />
            </div>
          </div>
        </div>

        {/* Right — Price card */}
        <div className="border rounded-xl p-5 space-y-4 h-fit">
          <div className="aspect-video bg-muted rounded-lg flex items-center justify-center overflow-hidden">
            {currentCourse.thumbnailUrl ? (
              <img
                src={currentCourse.thumbnailUrl}
                alt={currentCourse.title}
                className="w-full h-full object-cover"
                onError={(e) => { e.target.style.display = 'none'; }}
              />
            ) : (
              <BookOpen className="size-10 text-muted-foreground/20" />
            )}
          </div>
          <p className="text-2xl font-bold">{formatPrice(currentCourse.price)}</p>
          {renderActionButton()}
          <p className="text-xs text-muted-foreground text-center">
            {isFull ? 'Không còn chỗ trống' : `Còn ${spotsLeft} chỗ trống`}
          </p>
        </div>
      </div>

      {/* ── Curriculum (public — chỉ section + badge bài) ──────────────── */}
      {sortedSections.length > 0 && (
        <div className="mt-10">
          <h2 className="text-lg font-semibold mb-1">Nội dung khóa học</h2>
          <p className="text-xs text-muted-foreground mb-4">
            {sortedSections.length} chương · {totalLessons} bài học
          </p>
          <div className="border rounded-xl overflow-hidden divide-y">
            {sortedSections.map((section, idx) => {
              const lessonCount = section.lessonCount ?? section.lessons?.length ?? 0;
              const isOpen = !!openSections[idx];
              return (
                <div key={idx}>
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

                  {/* Khi mở rộng: chỉ thông báo — không hiện lesson content */}
                  {isOpen && (
                    <div className="bg-muted/20 px-6 py-4">
                      {lessonCount === 0 ? (
                        <p className="text-xs text-muted-foreground">Chưa có bài học.</p>
                      ) : (
                        <p className="text-xs text-muted-foreground flex items-center gap-1.5">
                          <PlayCircle className="size-3.5 text-muted-foreground/50" />
                          {lessonCount} bài học trong chương này
                          {!enrolled && (
                            <span className="ml-1">· Đăng ký để xem nội dung</span>
                          )}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
