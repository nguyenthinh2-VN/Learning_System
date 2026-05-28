import api from './auth';

// ═══════════════════════════════════════════
// LESSON API
// Base path: /api/v1/courses/{courseId}/sections/{sectionId}/lessons
// ═══════════════════════════════════════════

/**
 * Lấy danh sách lessons của một section
 * GET /api/v1/courses/{courseId}/sections/{sectionId}/lessons
 * Quyền: Đăng nhập (tất cả role)
 * @param {number} courseId
 * @param {number} sectionId
 */
export const getLessonsApi = (courseId, sectionId) =>
  api.get(`/courses/${courseId}/sections/${sectionId}/lessons`);

/**
 * Tạo lesson mới
 * POST /api/v1/courses/{courseId}/sections/{sectionId}/lessons
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {number} sectionId
 * @param {{ title: string, contentUrl: string, orderIndex: number }} data
 */
export const createLessonApi = (courseId, sectionId, data) =>
  api.post(`/courses/${courseId}/sections/${sectionId}/lessons`, data);

/**
 * Cập nhật lesson
 * PUT /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {number} sectionId
 * @param {number} lessonId
 * @param {{ title: string, contentUrl: string, orderIndex: number }} data
 */
export const updateLessonApi = (courseId, sectionId, lessonId, data) =>
  api.put(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`, data);

/**
 * Xóa lesson
 * DELETE /api/v1/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {number} sectionId
 * @param {number} lessonId
 */
export const deleteLessonApi = (courseId, sectionId, lessonId) =>
  api.delete(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`);
