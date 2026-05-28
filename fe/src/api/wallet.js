import api from './auth';

// ═══════════════════════════════════════════
// USER PROFILE API (Section 16)
// ═══════════════════════════════════════════

/**
 * Lấy thông tin cá nhân + số dư ví
 * GET /api/v1/users/me/profile
 * Quyền: Đăng nhập (mọi role)
 * @returns {{ id, username, email, name, role, isInternal, balance }}
 */
export const getProfileApi = () => api.get('/users/me/profile');

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
