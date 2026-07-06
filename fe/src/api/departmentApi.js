import adminApi from './adminApi';

export const getDepartmentsApi = () =>
  adminApi.get('/admin/departments');

export const createDepartmentApi = (data) =>
  adminApi.post('/admin/departments', data);

export const updateDepartmentApi = (id, data) =>
  adminApi.put(`/admin/departments/${id}`, data);

export const deleteDepartmentApi = (id) =>
  adminApi.delete(`/admin/departments/${id}`);
