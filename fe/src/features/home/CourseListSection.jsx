import { useEffect, useState, useCallback, useRef } from 'react';
import useCourseStore from '@/store/useCourseStore';
import CourseCard from './CourseCard';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';

export default function CourseListSection() {
  const {
    courses,
    totalPages,
    currentPage,
    keyword,
    loading,
    initialized,
    fetchCourses,
  } = useCourseStore();

  const [searchInput, setSearchInput] = useState(keyword);
  const isFirstRender = useRef(true);

  // Fetch lần đầu tiên khi mount (chỉ 1 lần)
  useEffect(() => {
    if (!initialized) {
      fetchCourses('', 0);
    }
  }, [initialized, fetchCourses]);

  // Debounce search — chỉ khi user thực sự gõ (không phải lần đầu)
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    const timer = setTimeout(() => {
      fetchCourses(searchInput, 0);
    }, 400);

    return () => clearTimeout(timer);
  }, [searchInput, fetchCourses]);

  const handlePageChange = useCallback(
    (page) => {
      fetchCourses(keyword, page);
      const el = document.getElementById('course-list-section');
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    },
    [keyword, fetchCourses]
  );

  return (
    <section id="course-list-section" className="px-6 py-12 bg-muted/30">
      <div className="max-w-5xl mx-auto">
        {/* Header + Search */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <div>
            <h2 className="text-xl font-bold tracking-tight">Tất cả khóa học</h2>
            <p className="text-sm text-muted-foreground mt-1">
              Tìm kiếm và khám phá khóa học phù hợp
            </p>
          </div>
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
            <Input
              id="course-search"
              type="text"
              placeholder="Tìm kiếm khóa học..."
              className="pl-9"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
          </div>
        </div>

        {/* Course Grid */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="space-y-3">
                <Skeleton className="aspect-video w-full rounded-lg" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-2 w-full" />
              </div>
            ))}
          </div>
        ) : courses.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {courses.map((course) => (
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
        ) : (
          <div className="text-center py-16 text-sm text-muted-foreground border border-dashed rounded-lg bg-background">
            {searchInput
              ? `Không tìm thấy khóa học nào cho "${searchInput}"`
              : 'Chưa có khóa học nào.'}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-1 mt-8">
            <Button
              variant="outline"
              size="icon-sm"
              disabled={currentPage === 0}
              onClick={() => handlePageChange(currentPage - 1)}
            >
              <ChevronLeft className="size-4" />
            </Button>

            {Array.from({ length: totalPages }, (_, i) => (
              <Button
                key={i}
                variant={i === currentPage ? 'default' : 'outline'}
                size="sm"
                className="min-w-8"
                onClick={() => handlePageChange(i)}
              >
                {i + 1}
              </Button>
            ))}

            <Button
              variant="outline"
              size="icon-sm"
              disabled={currentPage >= totalPages - 1}
              onClick={() => handlePageChange(currentPage + 1)}
            >
              <ChevronRight className="size-4" />
            </Button>
          </div>
        )}
      </div>
    </section>
  );
}
