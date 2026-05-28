import { create } from 'zustand';
import { getCoursesApi } from '@/api/course';

const useCourseStore = create((set, get) => ({
  // Danh sách khóa học (trang hiện tại)
  courses: [],
  totalPages: 0,
  totalElements: 0,
  currentPage: 0,
  keyword: '',
  loading: false,
  initialized: false, // đánh dấu đã fetch lần đầu chưa

  // Khóa học nổi bật (lấy từ lần fetch đầu tiên)
  featured: [],
  featuredLoading: false,

  /**
   * Fetch danh sách khóa học với keyword + phân trang
   */
  fetchCourses: async (keyword = '', page = 0, size = 9) => {
    set({ loading: true });
    try {
      const res = await getCoursesApi({ keyword, page, size });
      const data = res.data.data;
      const state = {
        courses: data.items || [],
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
        currentPage: data.page || 0,
        keyword,
        loading: false,
        initialized: true,
      };

      // Lần fetch đầu tiên (không có keyword) → dùng luôn làm featured
      if (!get().featured.length && !keyword.trim() && page === 0) {
        state.featured = (data.items || []).slice(0, 3);
      }

      set(state);
    } catch {
      set({ courses: [], loading: false, initialized: true });
    }
  },

  /**
   * Fetch top khóa học nổi bật (chỉ gọi khi chưa có data)
   */
  fetchFeatured: async () => {
    // Nếu đã có featured data (từ fetchCourses) thì không gọi lại
    if (get().featured.length > 0) return;

    set({ featuredLoading: true });
    try {
      const res = await getCoursesApi({ page: 0, size: 3 });
      const data = res.data.data;
      set({
        featured: data.items || [],
        featuredLoading: false,
      });
    } catch {
      set({ featured: [], featuredLoading: false });
    }
  },

  setKeyword: (keyword) => set({ keyword }),
}));

export default useCourseStore;
