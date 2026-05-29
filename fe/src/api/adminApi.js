import axios from 'axios';

/**
 * Axios instance riêng cho Admin Portal.
 * Dùng adminToken (tách biệt khỏi publicToken của website học viên).
 */
const adminApi = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

adminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Auth ───────────────────────────────────────────────
export const adminLoginApi = (credentials) =>
  adminApi.post('/auth/login', credentials);

// ─── Users ──────────────────────────────────────────────
export const createUserApi = (data) =>
  adminApi.post('/admin/users', data);

export const getUsersApi = (params = {}) =>
  adminApi.get('/admin/users', { params });

// ─── Courses ────────────────────────────────────────────
export const getAdminCoursesApi = (params = {}) =>
  adminApi.get('/admin/courses', { params });

export const getInstructorCoursesApi = (params = {}) =>
  adminApi.get('/instructor/courses', { params });

export const getPendingCoursesApi = (params = {}) =>
  adminApi.get('/admin/courses/pending', { params });

export const publishCourseApi = (id) =>
  adminApi.post(`/admin/courses/${id}/publish`);

export const unpublishCourseApi = (id) =>
  adminApi.post(`/admin/courses/${id}/unpublish`);

export const updateCoursePriceApi = (id, price) =>
  adminApi.put(`/admin/courses/${id}/price`, { price });

// Course CRUD (dùng adminApi để gửi admin token)
export const adminGetCourseDetailApi = (id) =>
  adminApi.get(`/courses/${id}`);

export const adminCreateCourseApi = (data) =>
  adminApi.post('/courses', data);

export const adminUpdateCourseApi = (id, data) =>
  adminApi.put(`/courses/${id}`, data);

export const adminDeleteCourseApi = (id) =>
  adminApi.delete(`/courses/${id}`);

// Sections (dùng adminApi)
export const adminGetSectionsApi = (courseId) =>
  adminApi.get(`/courses/${courseId}/sections`);

export const adminCreateSectionApi = (courseId, data) =>
  adminApi.post(`/courses/${courseId}/sections`, data);

export const adminUpdateSectionApi = (courseId, sectionId, data) =>
  adminApi.put(`/courses/${courseId}/sections/${sectionId}`, data);

export const adminDeleteSectionApi = (courseId, sectionId) =>
  adminApi.delete(`/courses/${courseId}/sections/${sectionId}`);

// Lessons (dùng adminApi)
export const adminCreateLessonApi = (courseId, sectionId, data) =>
  adminApi.post(`/courses/${courseId}/sections/${sectionId}/lessons`, data);

export const adminUpdateLessonApi = (courseId, sectionId, lessonId, data) =>
  adminApi.put(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`, data);

export const adminDeleteLessonApi = (courseId, sectionId, lessonId) =>
  adminApi.delete(`/courses/${courseId}/sections/${sectionId}/lessons/${lessonId}`);

// ─── Vouchers ───────────────────────────────────────────
export const getVouchersApi = (params = {}) =>
  adminApi.get('/admin/vouchers', { params });

export const createVoucherApi = (data) =>
  adminApi.post('/admin/vouchers', data);

export const updateVoucherApi = (id, data) =>
  adminApi.put(`/admin/vouchers/${id}`, data);

export const deleteVoucherApi = (id) =>
  adminApi.delete(`/admin/vouchers/${id}`);

// ─── Admin Top-up ───────────────────────────────────────
export const adminTopUpApi = (userId, data) =>
  adminApi.post(`/admin/users/${userId}/top-up`, data);

// Cộng tiền theo username HOẶC email (ô nhập 1 dòng)
export const adminTopUpByIdentifierApi = (data) =>
  adminApi.post('/admin/users/top-up', data); // { identifier, amount, note }

export default adminApi;
