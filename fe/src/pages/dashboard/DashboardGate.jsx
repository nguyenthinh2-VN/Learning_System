import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

/**
 * DashboardGate — cổng phân luồng sau khi login.
 * Đọc role từ AuthContext (publicToken) và redirect đến đúng dashboard.
 *
 * - Chưa login → /login
 * - MEMBER → /dashboard/home  (TODO: sẽ build sau)
 * - INSTRUCTOR → /instructor  (TODO: sẽ build sau)
 * - STAFF / ADMIN_USER / SUPER_ADMIN → /admin  (TODO: sẽ build sau)
 *
 * Tạm thời: render trang placeholder cho từng role khi chưa có trang riêng.
 */
export default function DashboardGate() {
  const { publicUser, isPublicAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isPublicAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }

    const role = publicUser?.role;
    if (role === 'INSTRUCTOR') {
      navigate('/instructor', { replace: true });
    } else if (['STAFF', 'ADMIN_USER', 'SUPER_ADMIN'].includes(role)) {
      navigate('/admin', { replace: true });
    }
    // MEMBER → ở lại /dashboard (trang member dashboard — sẽ build sau)
  }, [isPublicAuthenticated, publicUser, navigate]);

  // Render placeholder trong lúc chờ redirect hoặc cho MEMBER
  if (!isPublicAuthenticated) return null;

  const role = publicUser?.role;

  // Nếu là role đang chờ redirect (INSTRUCTOR/ADMIN), không render gì
  if (role === 'INSTRUCTOR' || ['STAFF', 'ADMIN_USER', 'SUPER_ADMIN'].includes(role)) {
    return null;
  }

  // MEMBER — trang placeholder cho đến khi có Member Dashboard thật
  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center text-center px-4">
      <div className="max-w-md space-y-4">
        <div className="size-16 rounded-2xl bg-foreground text-background flex items-center justify-center mx-auto text-2xl font-bold">
          {publicUser?.name?.charAt(0)?.toUpperCase() || 'U'}
        </div>
        <h1 className="text-2xl font-bold">Xin chào, {publicUser?.name}!</h1>
        <p className="text-muted-foreground text-sm">
          Dashboard học viên đang được phát triển. Bạn có thể dùng menu bên trái để truy cập các tính năng.
        </p>
        <a
          href="/"
          className="inline-block mt-4 text-sm font-medium underline underline-offset-4"
        >
          ← Về trang chủ
        </a>
      </div>
    </div>
  );
}

