import { Outlet, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { SidebarProvider, SidebarInset } from '@/components/ui/sidebar';
import { TooltipProvider } from '@/components/ui/tooltip';
import AdminSidebar from './AdminSidebar';
import { useAuth } from '@/context/AuthContext';

// Tất cả role được phép vào admin portal
const ADMIN_ROLES = ['INSTRUCTOR', 'STAFF', 'ADMIN_USER', 'SUPER_ADMIN'];

export default function AdminLayout() {
  const { isAdminAuthenticated, adminUser, refreshAdminPermissions } = useAuth();

  // Đồng bộ permissions động mỗi khi vào portal (quyền có thể đã đổi sau lần login trước).
  useEffect(() => {
    if (isAdminAuthenticated) {
      refreshAdminPermissions();
    }
  }, [isAdminAuthenticated, refreshAdminPermissions]);

  if (!isAdminAuthenticated) {
    return <Navigate to="/admin/login" replace />;
  }

  if (!ADMIN_ROLES.includes(adminUser?.role)) {
    return <Navigate to="/admin/login" replace />;
  }

  return (
    <div className="bg-slate-50 text-foreground">
      <TooltipProvider>
        <SidebarProvider>
          <AdminSidebar />
          <SidebarInset className="bg-slate-50">
            <Outlet />
          </SidebarInset>
        </SidebarProvider>
      </TooltipProvider>
    </div>
  );
}
