import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarRail,
} from '@/components/ui/sidebar';
import {
  Home,
  BookOpen,
  Wallet,
  User,
  Lock,
  LogOut,
  LogIn,
  ChevronRight,
  GraduationCap,
} from 'lucide-react';

export default function AppSidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { publicUser, isPublicAuthenticated, publicLogout } = useAuth();
  const [coursesOpen, setCoursesOpen] = useState(true);

  const isActive = (path) => location.pathname === path;

  const scrollToCourses = () => {
    if (location.pathname !== '/') {
      navigate('/');
      setTimeout(() => {
        document.getElementById('course-list-section')?.scrollIntoView({ behavior: 'smooth' });
      }, 300);
    } else {
      document.getElementById('course-list-section')?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <Sidebar collapsible="icon">
      {/* Header — Logo */}
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" render={<Link to="/" />}>
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-foreground text-background">
                <GraduationCap className="size-4" />
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-semibold">LearnSpace</span>
                <span className="truncate text-xs text-muted-foreground">
                  Nền tảng học trực tuyến
                </span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      {/* Content — Menu Groups */}
      <SidebarContent>
        {/* Menu chính */}
        <SidebarGroup>
          <SidebarGroupLabel>Menu chính</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {/* Trang chủ */}
              <SidebarMenuItem>
                <SidebarMenuButton
                  isActive={isActive('/')}
                  tooltip="Trang chủ"
                  render={<Link to="/" />}
                >
                  <Home />
                  <span>Trang chủ</span>
                </SidebarMenuButton>
              </SidebarMenuItem>

              {/* Khóa học — Toggle dropdown bằng useState */}
              <SidebarMenuItem>
                <SidebarMenuButton
                  tooltip="Khóa học"
                  onClick={() => setCoursesOpen((v) => !v)}
                >
                  <BookOpen />
                  <span>Khóa học</span>
                  <ChevronRight
                    className="ml-auto transition-transform duration-200"
                    style={{ transform: coursesOpen ? 'rotate(90deg)' : 'rotate(0deg)' }}
                  />
                </SidebarMenuButton>

                {/* Sub-menu */}
                {coursesOpen && (
                  <SidebarMenuSub>
                    <SidebarMenuSubItem>
                      <SidebarMenuSubButton onClick={scrollToCourses}>
                        <span>Tất cả khóa học</span>
                      </SidebarMenuSubButton>
                    </SidebarMenuSubItem>

                    {isPublicAuthenticated && (
                      <>
                        <SidebarMenuSubItem>
                          <SidebarMenuSubButton render={<Link to="/my-courses" />}>
                            <span>Đang học</span>
                          </SidebarMenuSubButton>
                        </SidebarMenuSubItem>
                        <SidebarMenuSubItem>
                          <SidebarMenuSubButton render={<Link to="/" />}>
                            <span>Yêu thích</span>
                          </SidebarMenuSubButton>
                        </SidebarMenuSubItem>
                      </>
                    )}
                  </SidebarMenuSub>
                )}
              </SidebarMenuItem>

              {/* Ví tiền — cần login */}
              {isPublicAuthenticated && (
                <SidebarMenuItem>
                  <SidebarMenuButton
                    isActive={isActive('/wallet')}
                    tooltip="Ví tiền"
                    render={<Link to="/wallet" />}
                  >
                    <Wallet />
                    <span>Ví tiền</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        {/* Tài khoản — cần login */}
        {isPublicAuthenticated && (
          <SidebarGroup>
            <SidebarGroupLabel>Tài khoản</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton tooltip="Thông tin cá nhân" render={<Link to="/" />}>
                    <User />
                    <span>Thông tin cá nhân</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton tooltip="Đổi mật khẩu" render={<Link to="/" />}>
                    <Lock />
                    <span>Đổi mật khẩu</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip="Đăng xuất"
                    onClick={() => {
                      publicLogout();
                      window.location.href = '/login';
                    }}
                  >
                    <LogOut />
                    <span>Đăng xuất</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
      </SidebarContent>

      {/* Footer — User info hoặc Đăng nhập */}
      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            {isPublicAuthenticated ? (
              <SidebarMenuButton size="lg" tooltip={publicUser?.email}>
                <div className="flex aspect-square size-8 items-center justify-center rounded-full bg-muted text-foreground font-semibold text-xs">
                  {publicUser?.name?.charAt(0)?.toUpperCase() || 'U'}
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">{publicUser?.name}</span>
                  <span className="truncate text-xs text-muted-foreground">
                    {publicUser?.email}
                  </span>
                </div>
              </SidebarMenuButton>
            ) : (
              <SidebarMenuButton
                size="lg"
                tooltip="Đăng nhập"
                render={<Link to="/login" />}
              >
                <div className="flex aspect-square size-8 items-center justify-center rounded-full bg-foreground text-background">
                  <LogIn className="size-4" />
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">Đăng nhập</span>
                  <span className="truncate text-xs text-muted-foreground">
                    Truy cập tài khoản
                  </span>
                </div>
              </SidebarMenuButton>
            )}
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  );
}
