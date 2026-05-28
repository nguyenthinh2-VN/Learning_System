import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useEnrollmentStore from '@/store/useEnrollmentStore';
import { useAuth } from '@/context/AuthContext';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  BookOpen, PlayCircle, Users, LogIn,
  Clock, DollarSign,
} from 'lucide-react';

function formatPrice(amount) {
  if (amount === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(amount);
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

function EnrolledCourseCard({ enrollment, onClick }) {
  const course = enrollment.course;
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
      className="group border rounded-xl overflow-hidden hover:shadow-md transition-shadow cursor-pointer bg-background"
    >
      {/* Thumbnail */}
      <div className="aspect-video bg-muted flex items-center justify-center relative overflow-hidden">
        {course?.thumbnailUrl ? (
          <img
            src={course.thumbnailUrl}
            alt={course?.title}
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
          />
        ) : null}
        <div className={`absolute inset-0 flex items-center justify-center ${course?.thumbnailUrl ? 'hidden' : 'flex'}`}>
          <BookOpen className="size-10 text-muted-foreground/20" />
        </div>
        <Badge className="absolute top-2 left-2 bg-emerald-600 text-white text-[10px] hover:bg-emerald-600">
          <PlayCircle className="size-2.5 mr-1" /> Đang học
        </Badge>
      </div>

      <div className="p-4 space-y-3">
        <h3 className="font-semibold text-sm leading-snug line-clamp-2 group-hover:underline underline-offset-2">
          {course?.title || `Khóa học #${enrollment.courseId}`}
        </h3>

        {course?.description && (
          <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
            {course.description}
          </p>
        )}

        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <Users className="size-3" />
            {course?.enrolledCount ?? '—'}/{course?.maxStudents ?? '—'}
          </span>
          <span className="flex items-center gap-1">
            <DollarSign className="size-3" />
            {formatPrice(enrollment.paidPrice)}
          </span>
        </div>

        <div className="flex items-center gap-1 text-[11px] text-muted-foreground border-t pt-2 mt-1">
          <Clock className="size-3" />
          Đã đăng ký: {formatDate(enrollment.enrolledAt)}
        </div>
      </div>
    </div>
  );
}

export default function MyCoursesPage() {
  const navigate = useNavigate();
  const { isPublicAuthenticated } = useAuth();
  const { enrollments, loading, initialized, fetchEnrollments } = useEnrollmentStore();

  useEffect(() => {
    if (isPublicAuthenticated) {
      fetchEnrollments(true); // force refresh khi vào trang này
    }
  }, [isPublicAuthenticated, fetchEnrollments]);

  // Chưa login
  if (!isPublicAuthenticated) {
    return (
      <div className="max-w-xl mx-auto px-6 py-24 text-center space-y-4">
        <LogIn className="size-12 mx-auto text-muted-foreground/30" />
        <h2 className="text-xl font-bold">Đăng nhập để xem khóa học</h2>
        <p className="text-sm text-muted-foreground">
          Bạn cần đăng nhập để xem danh sách khóa học đã đăng ký.
        </p>
        <Button onClick={() => navigate('/login')}>Đăng nhập ngay</Button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-6 py-10">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight">Khóa học của tôi</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Danh sách khóa học bạn đã đăng ký
        </p>
      </div>

      {/* Loading */}
      {(loading || !initialized) && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="space-y-3">
              <Skeleton className="aspect-video w-full rounded-xl" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-full" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          ))}
        </div>
      )}

      {/* Empty */}
      {initialized && !loading && enrollments.length === 0 && (
        <div className="text-center py-20 border border-dashed rounded-xl text-muted-foreground">
          <BookOpen className="size-12 mx-auto mb-4 opacity-20" />
          <p className="text-sm font-medium">Bạn chưa đăng ký khóa học nào</p>
          <p className="text-xs mt-1 mb-4">Khám phá các khóa học và bắt đầu hành trình học tập</p>
          <Button variant="outline" size="sm" onClick={() => navigate('/')}>
            Khám phá khóa học
          </Button>
        </div>
      )}

      {/* Grid */}
      {initialized && !loading && enrollments.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {enrollments.map((enrollment) => (
            <EnrolledCourseCard
              key={enrollment.enrollmentId}
              enrollment={enrollment}
              onClick={() => navigate(`/my-courses/${enrollment.courseId}`)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
