/**
 * Nguồn sự thật DUY NHẤT cho nhãn + màu của từng Role.
 * Dùng chung cho mọi nơi hiển thị role (bảng admin, profile, badge...).
 * Sau này thêm role mới chỉ cần khai báo ở đây.
 *
 * Mỗi role có một màu đặc trưng, thống nhất toàn hệ thống:
 *   MEMBER       → emerald (xanh lá)
 *   INSTRUCTOR   → indigo  (xanh tím)
 *   STAFF        → amber   (vàng/cam)
 *   ADMIN_USER   → violet  (tím)
 *   SUPER_ADMIN  → rose    (đỏ)
 */

export const ROLE_LABELS = {
  MEMBER: 'Học viên',
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý',
  SUPER_ADMIN: 'Quản trị viên',
};

// Hex tokens — dùng cho inline style (vd ProfilePage badge gradient)
export const ROLE_COLORS = {
  MEMBER:      { bg: '#ECFDF5', text: '#059669', border: '#A7F3D0' },
  INSTRUCTOR:  { bg: '#EEF2FF', text: '#4F46E5', border: '#C7D2FE' },
  STAFF:       { bg: '#FFFBEB', text: '#D97706', border: '#FDE68A' },
  ADMIN_USER:  { bg: '#F5F3FF', text: '#7C3AED', border: '#DDD6FE' },
  SUPER_ADMIN: { bg: '#FFF1F2', text: '#E11D48', border: '#FECDD3' },
};

export const ROLE_COLOR_FALLBACK = { bg: '#F1F5F9', text: '#64748B', border: '#E2E8F0' };

// Tailwind class tokens — dùng cho Badge/span trong bảng
export const ROLE_BADGE_CLASS = {
  MEMBER: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  INSTRUCTOR: 'bg-indigo-50 text-indigo-700 border border-indigo-200',
  STAFF: 'bg-amber-50 text-amber-700 border border-amber-200',
  ADMIN_USER: 'bg-violet-50 text-violet-700 border border-violet-200',
  SUPER_ADMIN: 'bg-rose-50 text-rose-700 border border-rose-200',
};

export const ROLE_BADGE_CLASS_FALLBACK = 'bg-slate-100 text-slate-600 border border-slate-200';

// Text-only color tokens — dùng cho nhãn role trên sidebar / header (chỉ đổi màu chữ, không bg)
export const ROLE_TEXT_CLASS = {
  MEMBER: 'text-emerald-600',
  INSTRUCTOR: 'text-indigo-600',
  STAFF: 'text-amber-600',
  ADMIN_USER: 'text-violet-600',
  SUPER_ADMIN: 'text-rose-600',
};

export const ROLE_TEXT_CLASS_FALLBACK = 'text-slate-600';

export function getRoleLabel(role) {
  return ROLE_LABELS[role] || role;
}

export function getRoleColor(role) {
  return ROLE_COLORS[role] || ROLE_COLOR_FALLBACK;
}

export function getRoleBadgeClass(role) {
  return ROLE_BADGE_CLASS[role] || ROLE_BADGE_CLASS_FALLBACK;
}

export function getRoleTextClass(role) {
  return ROLE_TEXT_CLASS[role] || ROLE_TEXT_CLASS_FALLBACK;
}
