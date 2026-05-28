import api from './auth';

// ═══════════════════════════════════════════
// SECTION API
// Base path: /api/v1/courses/{courseId}/sections
// ═══════════════════════════════════════════

/**
 * Lấy danh sách sections của một course (bao gồm lessons bên trong)
 * GET /api/v1/courses/{courseId}/sections
 * Quyền: Đăng nhập (tất cả role)
 */
export const getSectionsApi = (courseId) =>
  api.get(`/courses/${courseId}/sections`);

/**
 * Tạo section mới
 * POST /api/v1/courses/{courseId}/sections
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {{ title: string, orderIndex: number }} data
 */
export const createSectionApi = (courseId, data) =>
  api.post(`/courses/${courseId}/sections`, data);

/**
 * Cập nhật section
 * PUT /api/v1/courses/{courseId}/sections/{sectionId}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {number} sectionId
 * @param {{ title: string, orderIndex: number }} data
 */
export const updateSectionApi = (courseId, sectionId, data) =>
  api.put(`/courses/${courseId}/sections/${sectionId}`, data);

/**
 * Xóa section (cascade xóa toàn bộ lessons bên trong)
 * DELETE /api/v1/courses/{courseId}/sections/{sectionId}
 * Quyền: INSTRUCTOR (chỉ course mình), STAFF, SUPER_ADMIN
 * @param {number} courseId
 * @param {number} sectionId
 */
export const deleteSectionApi = (courseId, sectionId) =>
  api.delete(`/courses/${courseId}/sections/${sectionId}`);
