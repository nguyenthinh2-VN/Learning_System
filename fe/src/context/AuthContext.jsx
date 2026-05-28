import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

const PUBLIC_TOKEN_KEY = 'publicToken';
const PUBLIC_USER_KEY = 'publicUser';

export function AuthProvider({ children }) {
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
  }, []);

  const isPublicAuthenticated = !!publicToken;

  return (
    <AuthContext.Provider
      value={{
        publicUser,
        publicToken,
        publicLogin,
        publicLogout,
        isPublicAuthenticated,
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
