import api from './auth';

// ═══════════════════════════════════════════
// COURSE API
// ═══════════════════════════════════════════

/**
 * Lấy danh sách khóa học (public, chỉ published)
 * GET /api/v1/courses?keyword=&page=0&size=10
 */
export const getCoursesApi = ({ keyword = '', page = 0, size = 10 } = {}) => {
  const params = { page, size };
  if (keyword.trim()) params.keyword = keyword.trim();
  return api.get('/courses', { params });
};

/**
 * Lấy chi tiết khóa học (bao gồm sections + lessons)
 * GET /api/v1/courses/{id}
 */
export const getCourseByIdApi = (id) => api.get(`/courses/${id}`);

/**
 * Tạo khóa học mới
 * POST /api/v1/courses
 * Quyền: INSTRUCTOR, STAFF, ADMIN_USER, SUPER_ADMIN
 * @param {{ title, description, maxStudents, price, requestedInstructorId?, sections? }} data
 */
export const createCourseApi = (data) => api.post('/courses', data);

/**
 * Chỉnh sửa khóa học
 * PUT /api/v1/courses/{id}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, ADMIN_USER, SUPER_ADMIN
 */
export const updateCourseApi = (id, data) => api.put(`/courses/${id}`, data);

/**
 * Xóa khóa học
 * DELETE /api/v1/courses/{id}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, ADMIN_USER, SUPER_ADMIN
 */
export const deleteCourseApi = (id) => api.delete(`/courses/${id}`);

// ═══════════════════════════════════════════
// LESSON PROGRESS API (Theo dõi tiến độ học)
// ═══════════════════════════════════════════

/**
 * Đánh dấu một lesson đã hoàn thành.
 * POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete
 * Quyền: MEMBER (đã enrolled) / INSTRUCTOR (owner) / STAFF / SUPER_ADMIN
 */
export const markLessonCompleteApi = (courseId, lessonId) =>
  api.post(`/courses/${courseId}/lessons/${lessonId}/complete`);

/**
 * Bỏ đánh dấu hoàn thành (học lại).
 * DELETE /api/v1/courses/{courseId}/lessons/{lessonId}/complete
 */
export const unmarkLessonCompleteApi = (courseId, lessonId) =>
  api.delete(`/courses/${courseId}/lessons/${lessonId}/complete`);

/**
 * Tiến độ tổng quan của khóa học cho người gọi.
 * GET /api/v1/courses/{courseId}/progress
 * @returns {{ courseId, totalLessons, completedLessons, progressPercent, completedLessonIds }}
 */
export const getCourseProgressApi = (courseId) =>
  api.get(`/courses/${courseId}/progress`);

// ═══════════════════════════════════════════
// SECTION TEST API
// ═══════════════════════════════════════════

export const getSectionTestApi = (sectionId) =>
  api.get(`/sections/${sectionId}/test`);

export const submitSectionTestApi = (sectionId, answers) =>
  api.post(`/sections/${sectionId}/submit-test`, { answers });

