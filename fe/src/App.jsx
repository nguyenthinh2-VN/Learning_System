import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import AppLayout from '@/components/layout/AppLayout';
import HomePage from '@/pages/HomePage';
import CourseDetailPage from '@/pages/CourseDetailPage';
import PurchaseConfirmPage from '@/pages/PurchaseConfirmPage';
import WalletPage from '@/pages/WalletPage';
import MyCoursesPage from '@/pages/MyCoursesPage';
import EnrolledCourseDetailPage from '@/pages/EnrolledCourseDetailPage';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import DashboardGate from '@/pages/dashboard/DashboardGate';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public pages with Sidebar layout */}
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/courses/:id" element={<CourseDetailPage />} />
            <Route path="/courses/:id/purchase" element={<PurchaseConfirmPage />} />
            <Route path="/wallet" element={<WalletPage />} />
            <Route path="/my-courses" element={<MyCoursesPage />} />
            <Route path="/my-courses/:courseId" element={<EnrolledCourseDetailPage />} />
          </Route>

          {/* Auth pages — không có sidebar */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Internal portal — tách biệt hoàn toàn */}
          <Route path="/dashboard" element={<DashboardGate />} />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
