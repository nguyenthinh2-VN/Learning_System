import axios from 'axios';

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
