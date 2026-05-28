import '@/styles/brand.css';
import useCourseStore from '@/store/useCourseStore';
import CourseCard from './CourseCard';
import { Skeleton } from '@/components/ui/skeleton';
import { Flame } from 'lucide-react';

export default function FeaturedCourses() {
  const { featured, featuredLoading } = useCourseStore();

  if (featuredLoading) {
    return (
      <section className="px-6 py-12 bg-white">
        <div className="max-w-5xl mx-auto">
          <div className="mb-8">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-64 mt-2" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="space-y-3">
                <Skeleton className="aspect-video w-full rounded-xl" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-2 w-full" />
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (featured.length === 0) return null;

  return (
    <section className="px-6 py-12 bg-white">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-end justify-between mb-8">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Flame className="size-5" style={{ color: 'var(--brand-primary)' }} />
              <h2 className="text-xl font-bold" style={{ color: 'var(--brand-text-primary)' }}>
                Khóa học nổi bật
              </h2>
            </div>
            <p className="text-sm" style={{ color: 'var(--brand-text-secondary)' }}>
              Những khóa học được học viên yêu thích nhất
            </p>
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {featured.map((course) => (
            <CourseCard
              key={course.id}
              id={course.id}
              title={course.title}
              description={course.description}
              price={course.price}
              enrolledCount={course.enrolledCount}
              maxStudents={course.maxStudents}
              thumbnailUrl={course.thumbnailUrl}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
