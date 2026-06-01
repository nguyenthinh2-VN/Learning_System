/**
 * Helper kiểm tra permission ở FE (chỉ để ẩn/hiện UI — enforcement thật luôn ở BE).
 *
 * `user.permissions` được nạp từ login response (LoginResponse.permissions).
 * SUPER_ADMIN luôn full quyền nên bỏ qua kiểm tra danh sách.
 */
export function hasPermission(user, permission) {
  if (!user) return false;
  if (user.role === 'SUPER_ADMIN') return true;
  if (!permission) return true;
  const perms = user.permissions || [];
  return perms.includes(permission);
}

/** Có ít nhất một trong các permission. */
export function hasAnyPermission(user, permissions = []) {
  if (!user) return false;
  if (user.role === 'SUPER_ADMIN') return true;
  if (!permissions.length) return true;
  return permissions.some((p) => hasPermission(user, p));
}

// Nhãn tiếng Việt cho từng permission (FE hiển thị trong bảng ma trận)
export const PERMISSION_LABELS = {
  VIEW_COURSE: 'Xem khóa học',
  ENROLL_COURSE: 'Đăng ký khóa học',
  CREATE_COURSE: 'Tạo khóa học',
  EDIT_COURSE: 'Sửa khóa học',
  DELETE_COURSE: 'Xóa khóa học',
  CREATE_SECTION: 'Tạo chương học',
  EDIT_SECTION: 'Sửa/Xóa chương học',
  CREATE_LESSON: 'Tạo bài giảng',
  EDIT_LESSON: 'Sửa/Xóa bài giảng',
  VIEW_USER: 'Xem người dùng',
  CREATE_USER: 'Tạo tài khoản',
  EDIT_USER: 'Sửa người dùng',
  DELETE_USER: 'Xóa người dùng',
  MANAGE_ROLE: 'Quản lý phân quyền',
  VIEW_REPORT: 'Xem báo cáo',
  PUBLISH_COURSE: 'Duyệt & publish khóa học',
  LOCK_COURSE_PRICE: 'Khóa/sửa giá khóa học',
  MANAGE_VOUCHER: 'Quản lý voucher',
  USE_VOUCHER: 'Dùng voucher khi mua',
  VIEW_LESSON: 'Xem nội dung bài giảng',
  TRACK_PROGRESS: 'Theo dõi tiến độ học',
  VIEW_TRANSACTION: 'Xem giao dịch hệ thống',
  VIEW_REVENUE: 'Xem báo cáo doanh thu',
  MANAGE_WALLET: 'Cộng tiền vào ví',
};

// Nhóm permission theo module (FE chia nhóm cho dễ nhìn trong bảng ma trận)
export const PERMISSION_GROUPS = [
  { label: 'Khóa học', permissions: ['VIEW_COURSE', 'CREATE_COURSE', 'EDIT_COURSE', 'DELETE_COURSE', 'PUBLISH_COURSE', 'LOCK_COURSE_PRICE'] },
  { label: 'Nội dung', permissions: ['CREATE_SECTION', 'EDIT_SECTION', 'CREATE_LESSON', 'EDIT_LESSON', 'VIEW_LESSON', 'TRACK_PROGRESS'] },
  { label: 'Học viên', permissions: ['ENROLL_COURSE', 'USE_VOUCHER'] },
  { label: 'Người dùng & Vai trò', permissions: ['VIEW_USER', 'CREATE_USER', 'EDIT_USER', 'DELETE_USER', 'MANAGE_ROLE'] },
  { label: 'Tài chính & Báo cáo', permissions: ['MANAGE_VOUCHER', 'VIEW_REPORT', 'VIEW_TRANSACTION', 'VIEW_REVENUE', 'MANAGE_WALLET'] },
];

// Nhãn tiếng Việt cho từng role
export const ROLE_LABELS_PERM = {
  MEMBER: 'Học viên',
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý tài khoản',
  SUPER_ADMIN: 'Quản trị tối cao',
};
