import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { getProfileApi } from '@/api/wallet';

const AuthContext = createContext(null);

const PUBLIC_TOKEN_KEY = 'publicToken';
const PUBLIC_USER_KEY = 'publicUser';
const ADMIN_TOKEN_KEY = 'adminToken';
const ADMIN_USER_KEY = 'adminUser';

export function AuthProvider({ children }) {
  // ─── Public (Website học viên) ──────────────────────────
  const [publicUser, setPublicUser] = useState(() => {
    try {
      const stored = localStorage.getItem(PUBLIC_USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [publicToken, setPublicToken] = useState(() =>
    localStorage.getItem(PUBLIC_TOKEN_KEY) || null
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

  // Tự động gọi fetchProfile khi app load nếu đã có token (F5, mở tab mới)
  useEffect(() => {
    if (publicToken) {
      fetchProfile();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Chỉ chạy 1 lần khi mount

  const isPublicAuthenticated = !!publicToken;

  // ─── Admin (Portal quản trị — token tách biệt) ─────────
  const [adminUser, setAdminUser] = useState(() => {
    try {
      const stored = localStorage.getItem(ADMIN_USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [adminToken, setAdminToken] = useState(() =>
    localStorage.getItem(ADMIN_TOKEN_KEY) || null
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
