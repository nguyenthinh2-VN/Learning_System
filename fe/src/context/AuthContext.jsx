import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { getProfileApi } from '@/api/wallet';
import { getAdminProfileApi } from '@/api/adminApi';
import { isJwtExpired } from '@/lib/jwt';

const AuthContext = createContext(null);

const PUBLIC_TOKEN_KEY = 'publicToken';
const PUBLIC_USER_KEY = 'publicUser';
const ADMIN_TOKEN_KEY = 'adminToken';
const ADMIN_USER_KEY = 'adminUser';

/** Đọc token từ localStorage; nếu đã hết hạn thì xóa luôn và trả về null. */
function readValidToken(tokenKey, userKey) {
  const token = localStorage.getItem(tokenKey);
  if (token && isJwtExpired(token)) {
    localStorage.removeItem(tokenKey);
    localStorage.removeItem(userKey);
    return null;
  }
  return token || null;
}

export function AuthProvider({ children }) {
  // ─── Public (Website học viên) ──────────────────────────
  const [publicUser, setPublicUser] = useState(() => {
    try {
      // Nếu token đã hết hạn → không khôi phục user
      if (!readValidToken(PUBLIC_TOKEN_KEY, PUBLIC_USER_KEY)) return null;
      const stored = localStorage.getItem(PUBLIC_USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [publicToken, setPublicToken] = useState(() =>
    readValidToken(PUBLIC_TOKEN_KEY, PUBLIC_USER_KEY)
  );

  // Số dư ví — nguồn sự thật duy nhất, cập nhật qua fetchProfile hoặc WebSocket
  const [balance, setBalance] = useState(null);

  const publicLogin = useCallback((userData, token) => {
    localStorage.setItem(PUBLIC_TOKEN_KEY, token);
    localStorage.setItem(PUBLIC_USER_KEY, JSON.stringify(userData));
    setPublicToken(token);
    setPublicUser(userData);
  }, []);

  const publicLogout = useCallback(() => {
    localStorage.removeItem(PUBLIC_TOKEN_KEY);
    localStorage.removeItem(PUBLIC_USER_KEY);
    setPublicToken(null);
    setPublicUser(null);
    setBalance(null);
  }, []);

  /**
   * Gọi GET /api/v1/users/me/profile để lấy thông tin user + số dư ví.
   * Gọi ngay sau login thành công, hoặc khi cần refresh balance.
   */
  const fetchProfile = useCallback(async () => {
    try {
      const res = await getProfileApi();
      const data = res.data.data;
      // Cập nhật user info (đề phòng thay đổi từ BE)
      const userData = {
        id: data.id,
        username: data.username,
        email: data.email,
        name: data.name,
        role: data.role,
        isInternal: data.isInternal,
        avatarUrl: data.avatarUrl ?? null,
      };
      localStorage.setItem(PUBLIC_USER_KEY, JSON.stringify(userData));
      setPublicUser(userData);
      setBalance(data.balance ?? 0);
    } catch (err) {
      console.error('[AuthContext] fetchProfile failed:', err);
    }
  }, []);

  /**
   * Cập nhật số dư ví khi nhận sự kiện WALLET_UPDATED từ WebSocket.
   * @param {number} newBalance
   */
  const updateBalance = useCallback((newBalance) => {
    setBalance(newBalance);
  }, []);

  // Khi app load: nếu token public đã hết hạn → đăng xuất + chuyển về login.
  // Nếu còn hạn → fetch profile để lấy balance (F5, mở tab mới).
  useEffect(() => {
    const token = localStorage.getItem(PUBLIC_TOKEN_KEY);
    if (token && isJwtExpired(token)) {
      publicLogout();
      if (!window.location.pathname.startsWith('/login')
          && !window.location.pathname.startsWith('/admin')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/login?expired=1&redirect=${redirect}`;
      }
      return;
    }
    if (token) {
      fetchProfile();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Chỉ chạy 1 lần khi mount

  const isPublicAuthenticated = !!publicToken;

  // ─── Admin (Portal quản trị — token tách biệt) ─────────
  const [adminUser, setAdminUser] = useState(() => {
    try {
      if (!readValidToken(ADMIN_TOKEN_KEY, ADMIN_USER_KEY)) return null;
      const stored = localStorage.getItem(ADMIN_USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [adminToken, setAdminToken] = useState(() =>
    readValidToken(ADMIN_TOKEN_KEY, ADMIN_USER_KEY)
  );

  const adminLogin = useCallback((userData, token) => {
    localStorage.setItem(ADMIN_TOKEN_KEY, token);
    localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(userData));
    setAdminToken(token);
    setAdminUser(userData);
  }, []);

  const adminLogout = useCallback(() => {
    localStorage.removeItem(ADMIN_TOKEN_KEY);
    localStorage.removeItem(ADMIN_USER_KEY);
    setAdminToken(null);
    setAdminUser(null);
  }, []);

  /**
   * Đồng bộ lại permissions của admin đang đăng nhập từ server.
   * Cần thiết cho phân quyền động: khi SUPER_ADMIN đổi ma trận quyền, các tài khoản
   * đang đăng nhập sẽ nhận quyền mới mà không phải đăng nhập lại.
   */
  const refreshAdminPermissions = useCallback(async () => {
    try {
      const res = await getAdminProfileApi();
      const data = res.data?.data;
      if (!data) return;
      setAdminUser((prev) => {
        if (!prev) return prev;
        const updated = {
          ...prev,
          role: data.role ?? prev.role,
          permissions: data.permissions ?? prev.permissions ?? [],
        };
        localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(updated));
        return updated;
      });
    } catch (err) {
      console.error('[AuthContext] refreshAdminPermissions failed:', err);
    }
  }, []);

  // Khi app load trong khu vực /admin: nếu token admin hết hạn → đăng xuất + về /admin/login.
  useEffect(() => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY);
    if (token && isJwtExpired(token)) {
      adminLogout();
      if (window.location.pathname.startsWith('/admin')
          && !window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login?expired=1';
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const isAdminAuthenticated = !!adminToken;

  // ─── Provider ───────────────────────────────────────────
  return (
    <AuthContext.Provider
      value={{
        // Public
        publicUser,
        publicToken,
        balance,
        publicLogin,
        publicLogout,
        fetchProfile,
        updateBalance,
        isPublicAuthenticated,
        // Admin
        adminUser,
        adminToken,
        adminLogin,
        adminLogout,
        refreshAdminPermissions,
        isAdminAuthenticated,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
