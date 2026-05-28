import '@/styles/brand.css';
import { useEffect, useState, useCallback, useRef } from 'react';
import useCourseStore from '@/store/useCourseStore';
import CourseCard from './CourseCard';
import { Skeleton } from '@/components/ui/skeleton';
import { Search, ChevronLeft, ChevronRight, BookOpen } from 'lucide-react';

export default function CourseListSection() {
  const { courses, totalPages, currentPage, keyword, loading, initialized, fetchCourses } = useCourseStore();
  const [searchInput, setSearchInput] = useState(keyword);
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (!initialized) fetchCourses('', 0);
  }, [initialized, fetchCourses]);

  useEffect(() => {
    if (isFirstRender.current) { isFirstRender.current = false; return; }
    const timer = setTimeout(() => fetchCourses(searchInput, 0), 400);
    return () => clearTimeout(timer);
  }, [searchInput, fetchCourses]);

  const handlePageChange = useCallback((page) => {
    fetchCourses(keyword, page);
    document.getElementById('course-list-section')?.scrollIntoView({ behavior: 'smooth' });
  }, [keyword, fetchCourses]);

  return (
    <section id="course-list-section" className="px-6 py-12" style={{ background: 'var(--brand-bg-section)' }}>
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <BookOpen className="size-5" style={{ color: 'var(--brand-primary)' }} />
              <h2 className="text-xl font-bold" style={{ color: 'var(--brand-text-primary)' }}>
                Tất cả khóa học
              </h2>
            </div>
            <p className="text-sm" style={{ color: 'var(--brand-text-secondary)' }}>
              Tìm kiếm và khám phá khóa học phù hợp với bạn
            </p>
          </div>

          {/* Search */}
          <div className="relative w-full sm:w-72">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 size-4"
              style={{ color: 'var(--brand-text-tertiary)' }}
            />
            <input
              id="course-search"
              type="text"
              placeholder="Tìm kiếm khóa học..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-sm rounded-xl bg-white outline-none transition-all"
              style={{
                border: '1px solid var(--brand-border)',
                color: 'var(--brand-text-primary)',
              }}
              onFocus={(e) => { e.target.style.borderColor = 'var(--brand-primary)'; e.target.style.boxShadow = '0 0 0 3px var(--brand-primary-mid)'; }}
              onBlur={(e) => { e.target.style.borderColor = 'var(--brand-border)'; e.target.style.boxShadow = 'none'; }}
            />
          </div>
        </div>

        {/* Grid */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="space-y-3">
                <Skeleton className="aspect-video w-full rounded-xl" />
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
                thumbnailUrl={course.thumbnailUrl}
              />
            ))}
          </div>
        ) : (
          <div
            className="text-center py-16 text-sm rounded-xl bg-white"
            style={{ border: '1px dashed var(--brand-border)', color: 'var(--brand-text-secondary)' }}
          >
            {searchInput
              ? `Không tìm thấy khóa học nào cho "${searchInput}"`
              : 'Chưa có khóa học nào.'}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-1.5 mt-8">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
              className="size-8 flex items-center justify-center rounded-lg border transition-colors disabled:opacity-40"
              style={{ borderColor: 'var(--brand-border)', background: 'white' }}
            >
              <ChevronLeft className="size-4" />
            </button>

            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => handlePageChange(i)}
                className="size-8 flex items-center justify-center rounded-lg text-sm font-medium border transition-colors"
                style={i === currentPage ? {
                  background: 'var(--brand-primary)',
                  color: 'white',
                  borderColor: 'var(--brand-primary)',
                } : {
                  background: 'white',
                  color: 'var(--brand-text-primary)',
                  borderColor: 'var(--brand-border)',
                }}
              >
                {i + 1}
              </button>
            ))}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="size-8 flex items-center justify-center rounded-lg border transition-colors disabled:opacity-40"
              style={{ borderColor: 'var(--brand-border)', background: 'white' }}
            >
              <ChevronRight className="size-4" />
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
