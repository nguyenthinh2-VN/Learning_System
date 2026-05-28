import { create } from 'zustand';
import { getCourseByIdApi } from '@/api/course';
import { getSectionsApi } from '@/api/section';
import { getLessonsApi } from '@/api/lesson';

const useCourseDetailStore = create((set, get) => ({
  cache: {},         // public view cache (không có lessons)
  enrolledCache: {}, // enrolled view cache (có lessons)
  currentCourse: null,
  loading: false,
  error: null,

  /**
   * Fetch chi tiết khóa học cho PUBLIC VIEW
   * Chỉ gọi: getCourseByIdApi + getSectionsApi (KHÔNG gọi lessons)
   * Lý do: MEMBER chưa enrolled sẽ bị 403 nếu gọi lessons API
   */
  fetchCourseDetail: async (id) => {
    const cached = get().cache[id];
    if (cached) {
      set({ currentCourse: cached, loading: false, error: null });
      return;
    }

    set({ loading: true, error: null, currentCourse: null });
    try {
      const [courseRes, sectionsRes] = await Promise.all([
        getCourseByIdApi(id),
        getSectionsApi(id),
      ]);

      const course = courseRes.data.data;
      // sections từ /sections API — chỉ có { id, title, orderIndex, lessonCount? }
      // KHÔNG fetch lessons ở đây
      const sections = sectionsRes.data.data || [];

      const merged = { ...course, sections };

      set((state) => ({
        currentCourse: merged,
        cache: { ...state.cache, [id]: merged },
        loading: false,
      }));
    } catch (err) {
      const message = err?.response?.data?.message || 'Không thể tải thông tin khóa học.';
      set({ error: message, loading: false, currentCourse: null });
    }
  },

  /**
   * Fetch chi tiết khóa học cho ENROLLED VIEW
   * Gọi thêm lessons cho từng section (server cho phép vì đã enrolled)
   */
  fetchEnrolledDetail: async (id) => {
    const cached = get().enrolledCache[id];
    if (cached) {
      set({ currentCourse: cached, loading: false, error: null });
      return;
    }

    set({ loading: true, error: null, currentCourse: null });
    try {
      const [courseRes, sectionsRes] = await Promise.all([
        getCourseByIdApi(id),
        getSectionsApi(id),
      ]);

      const course = courseRes.data.data;
      const sections = sectionsRes.data.data || [];

      // Fetch lessons cho từng section song song
      const lessonsResults = await Promise.all(
        sections.map((section) =>
          getLessonsApi(id, section.id)
            .then((res) => res.data.data?.lessons || [])
            .catch(() => [])
        )
      );

      const sectionsWithLessons = sections.map((section, idx) => ({
        ...section,
        lessons: lessonsResults[idx] || [],
      }));

      const merged = { ...course, sections: sectionsWithLessons };

      set((state) => ({
        currentCourse: merged,
        enrolledCache: { ...state.enrolledCache, [id]: merged },
        loading: false,
      }));
    } catch (err) {
      const message = err?.response?.data?.message || 'Không thể tải nội dung khóa học.';
      set({ error: message, loading: false, currentCourse: null });
    }
  },

  invalidateCache: (id) =>
    set((state) => {
      const newCache = { ...state.cache };
      const newEnrolledCache = { ...state.enrolledCache };
      delete newCache[id];
      delete newEnrolledCache[id];
      return { cache: newCache, enrolledCache: newEnrolledCache };
    }),

  clearCurrent: () => set({ currentCourse: null, error: null }),
}));

export default useCourseDetailStore;
