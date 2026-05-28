import { create } from 'zustand';
import { getMyEnrollmentsApi } from '@/api/enrollment';
import { getCourseByIdApi } from '@/api/course';

const useEnrollmentStore = create((set, get) => ({
  // Danh sách enrollment kèm course info (sau khi join)
  enrollments: [],
  // Set courseId để check nhanh isEnrolled
  enrolledCourseIds: new Set(),
  loading: false,
  initialized: false,

  /**
   * Fetch danh sách khóa học đã mua + join course info song song
   * Chỉ fetch 1 lần, dùng initialized để tránh duplicate
   */
  fetchEnrollments: async (force = false) => {
    if (get().initialized && !force) return;

    set({ loading: true });
    try {
      const res = await getMyEnrollmentsApi({ page: 0, size: 100 });
      const items = res.data.data?.items || [];

      // Join course info song song (Promise.all)
      const enrollmentsWithCourse = await Promise.all(
        items.map(async (enrollment) => {
          try {
            const courseRes = await getCourseByIdApi(enrollment.courseId);
            return { ...enrollment, course: courseRes.data.data };
          } catch {
            return { ...enrollment, course: null };
          }
        })
      );

      const enrolledCourseIds = new Set(items.map((e) => e.courseId));

      set({
        enrollments: enrollmentsWithCourse,
        enrolledCourseIds,
        loading: false,
        initialized: true,
      });
    } catch {
      set({ loading: false, initialized: true });
    }
  },

  /**
   * Kiểm tra nhanh user đã enrolled course chưa
   * @param {number} courseId
   */
  isEnrolled: (courseId) => get().enrolledCourseIds.has(Number(courseId)),

  /**
   * Invalidate cache để re-fetch (dùng sau khi mua thành công)
   */
  invalidate: () => set({ initialized: false }),
}));

export default useEnrollmentStore;
