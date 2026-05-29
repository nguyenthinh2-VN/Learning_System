import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import { useWalletWebSocket } from '@/hooks/useWalletWebSocket';
import '@/styles/brand.css';

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

function formatBalance(amount) {
  if (amount === null || amount === undefined) return '---';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

export default function AppSidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { publicUser, publicToken, balance, isPublicAuthenticated, publicLogout, updateBalance } = useAuth();
  const [coursesOpen, setCoursesOpen] = useState(true);

  // Kết nối WebSocket — chỉ reconnect khi token thay đổi
  useWalletWebSocket({
    token: publicToken,
    onBalanceUpdate: (newBalance) => updateBalance(newBalance),
    onToast: (message, type) => {
      if (type === 'success') toast.success(message);
      else toast.info(message);
    },
  });

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
              <div
                className="flex aspect-square size-8 items-center justify-center rounded-lg shrink-0"
                style={{ background: 'var(--brand-gradient-btn)' }}
              >
                <GraduationCap className="size-4 text-white" />
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
                            <span>Khóa học đã mua</span>
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
                  <SidebarMenuButton tooltip="Thông tin cá nhân" render={<Link to="/profile" />}>
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
              <SidebarMenuButton
                size="lg"
                tooltip={`${publicUser?.email} · ${formatBalance(balance)}`}
                className="h-auto py-2"
                render={<Link to="/profile" />}
              >
                <div className="flex aspect-square size-8 items-center justify-center rounded-full bg-muted text-foreground font-semibold text-xs shrink-0 overflow-hidden">
                  {publicUser?.avatarUrl ? (
                    <img
                      src={publicUser.avatarUrl}
                      alt={publicUser?.name || 'avatar'}
                      className="size-full object-cover"
                      onError={(e) => { e.currentTarget.style.display = 'none'; e.currentTarget.nextSibling.style.display = 'flex'; }}
                    />
                  ) : null}
                  <span
                    className="size-full items-center justify-center"
                    style={{ display: publicUser?.avatarUrl ? 'none' : 'flex' }}
                  >
                    {publicUser?.name?.charAt(0)?.toUpperCase() || 'U'}
                  </span>
                </div>
                <div className="flex flex-col flex-1 text-left text-sm leading-tight min-w-0">
                  <span className="truncate font-semibold">{publicUser?.name}</span>
                  <span className="truncate text-xs text-muted-foreground">{publicUser?.email}</span>
                  <span className="truncate text-[11px] mt-0.5 text-emerald-500 font-medium">
                    {balance !== null ? formatBalance(balance) : '---'}
                    {/* {publicUser?.role && (
                      <span className="text-muted-foreground font-normal ml-1">· {publicUser.role}</span>
                    )} */}
                  </span>
                </div>
              </SidebarMenuButton>
            ) : (
              <SidebarMenuButton
                size="lg"
                tooltip="Đăng nhập"
                render={<Link to="/login" />}
              >
                <div className="flex aspect-square size-8 items-center justify-center rounded-full bg-foreground text-background shrink-0">
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
