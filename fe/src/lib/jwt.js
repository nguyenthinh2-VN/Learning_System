/**
 * Tiện ích JWT phía client — CHỈ để đọc thời hạn (exp), KHÔNG xác thực chữ ký.
 * Việc xác thực thật vẫn do server làm. Ở client ta chỉ cần biết token đã
 * hết hạn hay chưa để chủ động đăng xuất, tránh để UI ở trạng thái "đã đăng nhập" giả.
 */

/** Giải mã payload của JWT (base64url). Trả về object hoặc null nếu lỗi. */
export function decodeJwt(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/**
 * Token đã hết hạn chưa.
 * - Token không hợp lệ / không decode được → coi như hết hạn (true) để buộc đăng nhập lại.
 * - Token không có `exp` → coi như KHÔNG hết hạn (false), để server quyết định.
 * @param {string} token
 * @param {number} skewSeconds Dung sai lệch giờ (mặc định 0)
 */
export function isJwtExpired(token, skewSeconds = 0) {
  const payload = decodeJwt(token);
  if (!payload) return true;
  if (typeof payload.exp !== 'number') return false;
  const nowSec = Math.floor(Date.now() / 1000);
  return payload.exp <= nowSec - skewSeconds;
}
