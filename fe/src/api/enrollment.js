import api from './auth';

/**
 * Lấy danh sách khóa học đã mua của user hiện tại
 * GET /api/v1/users/me/enrollments
 * Quyền: Đăng nhập (mọi role)
 * Response trả { enrollmentId, courseId, paidPrice, enrolledAt }
 * (Không có title/thumbnail — FE phải join thêm courses API)
 */
export const getMyEnrollmentsApi = ({ page = 0, size = 100 } = {}) =>
  api.get('/users/me/enrollments', { params: { page, size } });
