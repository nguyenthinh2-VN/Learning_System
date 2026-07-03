import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import { useWalletWebSocket } from '@/hooks/useWalletWebSocket';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from './LanguageSwitcher';
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
  const { t } = useTranslation();

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
                <span className="truncate font-bold text-lg">LearnSpace</span>
                <span className="truncate text-xs text-muted-foreground">
                  {t('ui.sidebar.platform_name')}
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
          <SidebarGroupLabel>{t('ui.sidebar.main_menu')}</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {/* Trang chủ */}
              <SidebarMenuItem>
                <SidebarMenuButton
                  isActive={isActive('/')}
                  tooltip={t('ui.sidebar.home')}
                  render={<Link to="/" />}
                >
                  <Home />
                  <span>{t('ui.sidebar.home')}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>

              {/* Tiến độ (Yêu cầu đăng nhập) */}
              {isPublicAuthenticated && (
                <SidebarMenuItem>
                  <SidebarMenuButton
                    isActive={isActive('/progress')}
                    tooltip={t('ui.sidebar.progress')}
                    render={<Link to="/progress" />}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="lucide lucide-trending-up"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17" /><polyline points="16 7 22 7 22 13" /></svg>
                    <span>{t('ui.sidebar.progress')}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}

              {/* Khóa học — Toggle dropdown bằng useState */}
              <SidebarMenuItem>
                <SidebarMenuButton
                  tooltip={t('ui.sidebar.courses')}
                  onClick={() => setCoursesOpen((v) => !v)}
                >
                  <BookOpen />
                  <span>{t('ui.sidebar.courses')}</span>
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
                        <span>{t('ui.sidebar.all_courses')}</span>
                      </SidebarMenuSubButton>
                    </SidebarMenuSubItem>

                    {isPublicAuthenticated && (
                      <>
                        <SidebarMenuSubItem>
                          <SidebarMenuSubButton render={<Link to="/my-courses" />}>
                            <span>{t('ui.sidebar.purchased_courses')}</span>
                          </SidebarMenuSubButton>
                        </SidebarMenuSubItem>
                        <SidebarMenuSubItem>
                          <SidebarMenuSubButton render={<Link to="/" />}>
                            <span>{t('ui.sidebar.favorites')}</span>
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
                    tooltip={t('ui.sidebar.wallet')}
                    render={<Link to="/wallet" />}
                  >
                    <Wallet />
                    <span>{t('ui.sidebar.wallet')}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        {/* Tài khoản — cần login */}
        {isPublicAuthenticated && (
          <SidebarGroup>
            <SidebarGroupLabel>{t('ui.sidebar.account')}</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton tooltip={t('ui.sidebar.my_profile')} render={<Link to="/profile" />}>
                    <User />
                    <span>{t('ui.sidebar.my_profile')}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton tooltip={t('ui.sidebar.change_pw')} render={<Link to="/" />}>
                    <Lock />
                    <span>{t('ui.sidebar.change_pw')}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip={t('ui.sidebar.logout')}
                    onClick={() => {
                      publicLogout();
                      window.location.href = '/login';
                    }}
                  >
                    <LogOut />
                    <span>{t('ui.sidebar.logout')}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>

                {/* Chọn ngôn ngữ */}
                <SidebarMenuItem>
                  <LanguageSwitcher />
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
      </SidebarContent>

      {/* Footer — User Profile & Settings */}
      <SidebarFooter>
        <SidebarMenu>
          {!isPublicAuthenticated && (
            <SidebarMenuItem>
              <LanguageSwitcher />
            </SidebarMenuItem>
          )}
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
                tooltip={t('ui.sidebar.login')}
                render={<Link to="/login" />}
              >
                <div className="flex aspect-square size-8 items-center justify-center rounded-full bg-foreground text-background shrink-0">
                  <LogIn className="size-4" />
                </div>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold">{t('ui.sidebar.login')}</span>
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
