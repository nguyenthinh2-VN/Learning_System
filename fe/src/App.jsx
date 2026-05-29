import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import { AuthProvider } from '@/context/AuthContext';
import AppLayout from '@/components/layout/AppLayout';
import AdminLayout from '@/components/layout/AdminLayout';
import HomePage from '@/pages/HomePage';
import CourseDetailPage from '@/pages/CourseDetailPage';
import PurchaseConfirmPage from '@/pages/PurchaseConfirmPage';
import WalletPage from '@/pages/WalletPage';
import MyCoursesPage from '@/pages/MyCoursesPage';
import EnrolledCourseDetailPage from '@/pages/EnrolledCourseDetailPage';
import ProfilePage from '@/pages/ProfilePage';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import DashboardGate from '@/pages/dashboard/DashboardGate';
import AdminLoginPage from '@/pages/admin/AdminLoginPage';
import AdminOverviewPage from '@/pages/admin/AdminOverviewPage';
import AdminUsersPage from '@/pages/admin/AdminUsersPage';
import AdminCoursesPage from '@/pages/admin/AdminCoursesPage';
import AdminCourseContentPage from '@/pages/admin/AdminCourseContentPage';
import AdminCoursePreviewPage from '@/pages/admin/AdminCoursePreviewPage';
import AdminPendingCoursesPage from '@/pages/admin/AdminPendingCoursesPage';
import AdminVouchersPage from '@/pages/admin/AdminVouchersPage';
import AdminTopUpPage from '@/pages/admin/AdminTopUpPage';

function InstructorPlaceholder() {
  return (
    <div className="min-h-screen flex items-center justify-center flex-col gap-4">
      <h1 className="text-2xl font-bold">Instructor Dashboard</h1>
      <p className="text-muted-foreground text-sm">Đang phát triển...</p>
      <a href="/" className="text-sm underline underline-offset-4">Trang chủ</a>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <Toaster position="top-right" richColors closeButton />
      <BrowserRouter>
        <Routes>
          {/* Public pages with AppSidebar */}
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/courses/:id" element={<CourseDetailPage />} />
            <Route path="/courses/:id/purchase" element={<PurchaseConfirmPage />} />
            <Route path="/wallet" element={<WalletPage />} />
            <Route path="/my-courses" element={<MyCoursesPage />} />
            <Route path="/my-courses/:courseId" element={<EnrolledCourseDetailPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>

          {/* Auth pages (public login/register) */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Dashboard gate — redirect theo role */}
          <Route path="/dashboard" element={<DashboardGate />} />
          <Route path="/dashboard/*" element={<DashboardGate />} />

          {/* Admin portal — Login riêng, Layout riêng, Token riêng */}
          <Route path="/admin/login" element={<AdminLoginPage />} />
          <Route element={<AdminLayout />}>
            <Route path="/admin" element={<AdminOverviewPage />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/courses" element={<AdminCoursesPage />} />
            <Route path="/admin/courses/:id/content" element={<AdminCourseContentPage />} />
            <Route path="/admin/courses/pending" element={<AdminPendingCoursesPage />} />
            <Route path="/admin/courses/:id/preview" element={<AdminCoursePreviewPage />} />
            <Route path="/admin/vouchers" element={<AdminVouchersPage />} />
            <Route path="/admin/wallet" element={<AdminTopUpPage />} />
          </Route>

          {/* Instructor portal (sẽ build sau) */}
          <Route path="/instructor" element={<InstructorPlaceholder />} />
          <Route path="/instructor/*" element={<InstructorPlaceholder />} />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
