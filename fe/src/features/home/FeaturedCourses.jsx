import useCourseStore from '@/store/useCourseStore';
import CourseCard from './CourseCard';
import { Skeleton } from '@/components/ui/skeleton';

export default function FeaturedCourses() {
  const { featured, featuredLoading } = useCourseStore();

  // Không fetch ở đây — data lấy từ fetchCourses lần đầu trong CourseListSection

  if (featuredLoading) {
    return (
      <section className="px-6 py-12">
        <div className="max-w-5xl mx-auto">
          <div className="mb-8">
            <h2 className="text-xl font-bold tracking-tight">Khóa học nổi bật</h2>
            <p className="text-sm text-muted-foreground mt-1">
              Những khóa học được yêu thích nhất
            </p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="space-y-3">
                <Skeleton className="aspect-video w-full rounded-lg" />
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
    <section className="px-6 py-12">
      <div className="max-w-5xl mx-auto">
        <div className="mb-8">
          <h2 className="text-xl font-bold tracking-tight">Khóa học nổi bật</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Những khóa học được yêu thích nhất
          </p>
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
            />
          ))}
        </div>
      </div>
    </section>
  );
}
