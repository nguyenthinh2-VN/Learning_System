import axios from 'axios';

/**
 * Axios instance riêng cho Admin Portal.
 * Dùng adminToken (tách biệt khỏi publicToken của website học viên).
 */
const adminApi = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

adminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Token admin hết hạn / không hợp lệ (401) → xóa phiên admin, về /admin/login.
// Bỏ qua /auth/** để lỗi sai mật khẩu vẫn hiển thị trên form đăng nhập admin.
adminApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    const isAuthCall = url.includes('/auth/');
    const hasSession = !!localStorage.getItem('adminToken');

    if (status === 401 && hasSession && !isAuthCall) {
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminUser');
      if (!window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login?expired=1';
      }
    }
    return Promise.reject(error);
  }
);

// ─── Auth ───────────────────────────────────────────────
export const adminLoginApi = (credentials) =>
  adminApi.post('/auth/login', credentials);

// Lấy thông tin + permissions hiện tại của admin đang đăng nhập (refresh quyền động)
export const getAdminProfileApi = () =>
  adminApi.get('/users/me/profile');

// ─── Users ──────────────────────────────────────────────
export const createUserApi = (data) =>
  adminApi.post('/admin/users', data);

export const getUsersApi = (params = {}) =>
  adminApi.get('/admin/users', { params });

export const adminUpdateUserApi = (id, data) =>
  adminApi.put(`/admin/users/${id}`, data); // { name?, roleName?, isInternal? }

export const adminSetUserStatusApi = (id, enabled) =>
  adminApi.patch(`/admin/users/${id}/status`, { enabled });

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

// ─── Reports & Transactions (SUPER_ADMIN) ───────────────
// Giao dịch toàn hệ thống (lọc + phân trang)
// params: { keyword, source, status, direction, from, to, page, size }
export const getAdminTransactionsApi = (params = {}) =>
  adminApi.get('/admin/transactions', { params });

// Báo cáo doanh thu (số liệu tổng + chuỗi thời gian)
// params: { granularity: 'DAY'|'MONTH', from, to }
export const getRevenueReportApi = (params = {}) =>
  adminApi.get('/admin/reports/revenue', { params });

// ─── Admin Top-up ───────────────────────────────────────
export const adminTopUpApi = (userId, data) =>
  adminApi.post(`/admin/users/${userId}/top-up`, data);

// Cộng tiền theo username HOẶC email (ô nhập 1 dòng)
export const adminTopUpByIdentifierApi = (data) =>
  adminApi.post('/admin/users/top-up', data); // { identifier, amount, note }

// ─── Phân quyền động (Dynamic RBAC — MANAGE_ROLE) ───────
// Danh sách tất cả permission khả dụng
export const getPermissionsApi = () =>
  adminApi.get('/admin/permissions');

// Toàn bộ ma trận role → permission (FE render bảng checkbox)
export const getPermissionMatrixApi = () =>
  adminApi.get('/admin/roles/permissions');

// Permission của một role cụ thể
export const getRolePermissionsApi = (roleName) =>
  adminApi.get(`/admin/roles/${roleName}/permissions`);

// Thay thế toàn bộ permission của một role (replace-all)
export const updateRolePermissionsApi = (roleName, permissions) =>
  adminApi.put(`/admin/roles/${roleName}/permissions`, { permissions });

export default adminApi;
