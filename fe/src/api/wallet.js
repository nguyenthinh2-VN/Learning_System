import api from './auth';

// ═══════════════════════════════════════════
// USER PROFILE API (Section 16)
// ═══════════════════════════════════════════

/**
 * Lấy thông tin cá nhân + số dư ví
 * GET /api/v1/users/me/profile
 * Quyền: Đăng nhập (mọi role)
 * @returns {{ id, username, email, name, role, isInternal, balance, avatarUrl }}
 */
export const getProfileApi = () => api.get('/users/me/profile');

/**
 * Cập nhật thông tin cá nhân (name, avatarUrl)
 * PUT /api/v1/users/me/profile
 * Quyền: Đăng nhập — chỉ sửa của chính mình
 * @param {{ name: string, avatarUrl?: string }} data
 *   avatarUrl: null/undefined = giữ nguyên, "" = xóa avatar
 */
export const updateProfileApi = (data) =>
  api.put('/users/me/profile', data);

/**
 * Đổi mật khẩu
 * PUT /api/v1/users/me/password
 * Quyền: Đăng nhập
 * @param {{ currentPassword: string, newPassword: string }} data
 */
export const changePasswordApi = (data) =>
  api.put('/users/me/password', data);

/**
 * Upload ảnh đại diện (multipart). Chỉ JPEG/PNG/WebP, tối đa 2MB.
 * POST /api/v1/users/me/avatar
 * Quyền: Đăng nhập
 * @param {File} file
 * @returns profile mới (kèm avatarUrl)
 */
export const uploadAvatarApi = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// ═══════════════════════════════════════════
// WALLET TOP-UP API (Section 14)
// ═══════════════════════════════════════════

/**
 * Khởi tạo nạp tiền — nhận referenceCode + displayType/displayData
 * POST /api/v1/wallet/top-up/init
 * Quyền: Đăng nhập
 * @param {number} amount - Tối thiểu 10,000đ
 * @returns {{ referenceCode, amount, displayType, displayData, expiredAt }}
 */
export const initTopUpApi = (amount) =>
  api.post('/wallet/top-up/init', { amount });

/**
 * Giả lập thanh toán thành công (chỉ dùng khi payment.provider=mock)
 * POST /api/v1/webhook/mock?ref={referenceCode}
 * Quyền: Không cần (dev only)
 * @param {string} referenceCode
 */
export const mockWebhookApi = (referenceCode) =>
  api.post(`/webhook/mock?ref=${referenceCode}`);

/**
 * Lịch sử giao dịch ví của chính mình (phân trang, mới nhất trước)
 * GET /api/v1/users/me/transactions?page&size
 * Quyền: Đăng nhập
 * @param {number} page - mặc định 0
 * @param {number} size - mặc định 20, [1, 100]
 * @returns PageResult<{ id, referenceCode, amount, direction, status, source, note, createdAt, completedAt }>
 */
export const getTransactionsApi = (page = 0, size = 20) =>
  api.get('/users/me/transactions', { params: { page, size } });

// ═══════════════════════════════════════════
// PURCHASE API
// ═══════════════════════════════════════════

/**
 * Preview giá (có voucher hoặc không)
 * POST /api/v1/courses/{courseId}/quote
 * Quyền: MEMBER (read-only, không tiêu thụ voucher)
 * @param {number} courseId
 * @param {string} [voucherCode]
 */
export const quoteApi = (courseId, voucherCode) =>
  api.post(`/courses/${courseId}/quote`, voucherCode ? { voucherCode } : {});

/**
 * Mua khóa học (checkout)
 * POST /api/v1/courses/{id}/purchase
 * Quyền: Đăng nhập
 * @param {number} courseId
 * @param {string} [voucherCode]
 */
export const purchaseApi = (courseId, voucherCode) =>
  api.post(`/courses/${courseId}/purchase`, voucherCode ? { voucherCode } : {});
