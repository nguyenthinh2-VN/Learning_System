import axios from 'axios';
import i18n from '../i18n';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Tự động đính kèm JWT token — CHỈ dùng publicToken cho public API
// (internalToken dành riêng cho dashboard, không được lẫn vào đây)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('publicToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Tự động xử lý token hết hạn / không hợp lệ (401).
// Nếu đang có phiên đăng nhập (publicToken tồn tại) mà server trả 401,
// nghĩa là token đã hết hạn → xóa phiên và chuyển về trang đăng nhập.
// Bỏ qua chính endpoint /auth/** để lỗi sai mật khẩu vẫn hiển thị trên form.
api.interceptors.response.use(
  (response) => {
    // Tự động dịch success message nếu có
    if (response.data && response.data.code) {
      const translated = i18n.t(`api.${response.data.code}`);
      if (translated && translated !== `api.${response.data.code}`) {
        response.data.message = translated;
      }
    }
    return response;
  },
  (error) => {
    // Tự động dịch error message nếu có
    if (error.response && error.response.data && error.response.data.code) {
      const translated = i18n.t(`api.${error.response.data.code}`);
      if (translated && translated !== `api.${error.response.data.code}`) {
        error.response.data.message = translated;
      }
    }
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    const isAuthCall = url.includes('/auth/');
    const hasSession = !!localStorage.getItem('publicToken');

    if (status === 401 && hasSession && !isAuthCall) {
      localStorage.removeItem('publicToken');
      localStorage.removeItem('publicUser');
      // Tránh vòng lặp nếu đang ở trang login
      if (!window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/login?expired=1&redirect=${redirect}`;
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Public login — dùng cho học viên
 * @param {{ identifier: string, password: string }} credentials
 */
export const loginApi = (credentials) =>
  api.post('/auth/login', credentials);

/**
 * Đăng ký tài khoản mới
 * @param {{ email: string, password: string, name: string }} data
 */
export const registerApi = (data) =>
  api.post('/auth/register', data);

export default api;
