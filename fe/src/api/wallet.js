import api from './auth';

// ═══════════════════════════════════════════
// WALLET API
// ═══════════════════════════════════════════

/**
 * Nạp tiền vào ví
 * POST /api/v1/users/me/top-up
 * Quyền: Đăng nhập
 * @param {number} amount
 */
export const topUpApi = (amount) =>
  api.post('/users/me/top-up', { amount });

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
