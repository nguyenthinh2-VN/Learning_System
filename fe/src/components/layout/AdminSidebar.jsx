import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  SidebarRail,
  SidebarSeparator,
} from '@/components/ui/sidebar';
import {
  LayoutDashboard,
  Users,
  BookOpen,
  Ticket,
  Wallet,
  Receipt,
  LogOut,
  ShieldCheck,
  ClipboardCheck,
  GraduationCap,
  KeyRound,
} from 'lucide-react';
import LanguageSwitcher from '@/components/layout/LanguageSwitcher';
import { ROLE_LABELS, getRoleTextClass } from '@/lib/roleColors';
import { hasPermission } from '@/lib/permissions';

/**
 * Ma trận phân quyền theo permission-matrix.md
 * INSTRUCTOR: CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION, CREATE_LESSON, EDIT_LESSON
 * STAFF: + PUBLISH_COURSE, LOCK_COURSE_PRICE, MANAGE_VOUCHER
 * ADMIN_USER: + VIEW_USER, CREATE_USER, EDIT_USER
 * SUPER_ADMIN: tất cả
 */
export default function AdminSidebar() {
  const { t } = useTranslation();

  const NAV_GROUPS = [
  {
    label: t('ui.admin.overview'),
    items: [
      { title: t('ui.admin.dashboard'), url: '/admin', icon: LayoutDashboard },
    ],
  },
  {
    label: t('ui.admin.content'),
    items: [
      // INSTRUCTOR chỉ thấy "Khóa học của tôi" — tất cả role có CREATE_COURSE đều thấy
      { title: t('ui.admin.courses'), url: '/admin/courses', icon: BookOpen },
      // Chờ duyệt: ai có PUBLISH_COURSE
      { title: t('ui.admin.pending_approval'), url: '/admin/courses/pending', icon: ClipboardCheck, permission: 'PUBLISH_COURSE' },
    ],
  },
  {
    label: t('ui.admin.management'),
    items: [
      // VIEW_USER: ADMIN_USER, SUPER_ADMIN
      { title: t('ui.admin.users'), url: '/admin/users', icon: Users, roles: ['ADMIN_USER', 'SUPER_ADMIN'] },
      // MANAGE_ROLE: phân quyền động (mặc định chỉ SUPER_ADMIN)
      { title: t('ui.admin.roles_permissions'), url: '/admin/permissions', icon: KeyRound, permission: 'MANAGE_ROLE' },
    ],
  },
  {
    label: t('ui.admin.finance'),
    items: [
      // MANAGE_VOUCHER: ai có MANAGE_VOUCHER
      { title: t('ui.admin.vouchers'), url: '/admin/vouchers', icon: Ticket, permission: 'MANAGE_VOUCHER' },
      // Giao dịch toàn hệ thống: ai có VIEW_TRANSACTION
      { title: t('ui.admin.transactions'), url: '/admin/transactions', icon: Receipt, permission: 'VIEW_TRANSACTION' },
      // Cộng tiền: ai có MANAGE_WALLET
      { title: t('ui.admin.add_funds'), url: '/admin/wallet', icon: Wallet, permission: 'MANAGE_WALLET' },
    ],
  },
];

  const location = useLocation();
  const navigate = useNavigate();
  const { adminUser, adminLogout } = useAuth();

  const role = adminUser?.role;
  const isInstructor = role === 'INSTRUCTOR';
  const canAccess = (item) => {
    if (item.permission) return hasPermission(adminUser, item.permission);
    return !item.roles || item.roles.includes(role);
  };

  // Tập hợp tất cả url nav mà role hiện tại thấy được
  const allUrls = NAV_GROUPS
    .flatMap((g) => g.items)
    .filter(canAccess)
    .map((i) => i.url);

  // Chọn url khớp cụ thể nhất (dài nhất) với pathname hiện tại
  const matchedUrl = (path) =>
    allUrls
      .filter((url) => path === url || path.startsWith(url + '/'))
      .sort((a, b) => b.length - a.length)[0] ?? null;

  const isActive = (url) => matchedUrl(location.pathname) === url;

  const handleLogout = () => {
    adminLogout();
    navigate('/admin/login', { replace: true });
  };

  return (
    <Sidebar collapsible="icon">
      {/* Header — Brand */}
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" tooltip="Admin Portal" render={<Link to="/admin" />}>
              <div className={`flex aspect-square size-8 items-center justify-center rounded-lg shrink-0 ${
                isInstructor
                  ? 'bg-gradient-to-br from-emerald-500 to-teal-600 text-white'
                  : 'bg-gradient-to-br from-indigo-500 to-purple-600 text-white'
              }`}>
                {isInstructor ? <GraduationCap className="size-4" /> : <ShieldCheck className="size-4" />}
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-semibold">LearnSpace</span>
                <span className="truncate text-xs text-muted-foreground">
                  {isInstructor ? 'Instructor Portal' : 'Admin Portal'}
                </span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      {/* Content — Nav groups */}
      <SidebarContent>
        {NAV_GROUPS.map((group) => {
          const visibleItems = group.items.filter(canAccess);
          if (visibleItems.length === 0) return null;
          return (
            <SidebarGroup key={group.label}>
              <SidebarGroupLabel>{group.label}</SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  {visibleItems.map((item) => (
                    <SidebarMenuItem key={item.url}>
                      <SidebarMenuButton
                        tooltip={item.title}
                        isActive={isActive(item.url)}
                        render={<Link to={item.url} />}
                      >
                        <item.icon />
                        <span>{item.title}</span>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))}
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
          );
        })}
      </SidebarContent>

      {/* Footer — User card + Logout */}
      <SidebarFooter>
        <SidebarSeparator className="mb-1" />
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              size="lg"
              tooltip={`${adminUser?.name} · ${adminUser?.email}`}
              className="cursor-default hover:bg-transparent h-auto py-2"
            >
              <div className={`flex aspect-square size-8 items-center justify-center rounded-full font-semibold text-xs border shrink-0 ${
                isInstructor
                  ? 'bg-gradient-to-br from-emerald-500/15 to-teal-500/15 text-emerald-700 border-emerald-500/30'
                  : 'bg-gradient-to-br from-indigo-500/15 to-purple-500/15 text-indigo-700 border-indigo-500/30'
              }`}>
                {adminUser?.name?.charAt(0)?.toUpperCase() || 'A'}
              </div>
              <div className="flex flex-col flex-1 text-left text-sm leading-tight min-w-0">
                <span className="truncate font-semibold">{adminUser?.name}</span>
                <span className="truncate text-xs text-muted-foreground">{adminUser?.email}</span>
                <span className={`truncate text-[11px] mt-0.5 font-medium ${getRoleTextClass(role)}`}>
                  {ROLE_LABELS[role] || role}
                </span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
          <SidebarMenuItem>
            <LanguageSwitcher />
          </SidebarMenuItem>
          <SidebarMenuItem>
            <SidebarMenuButton
              tooltip={t('ui.admin.logout')}
              onClick={handleLogout}
              className="text-muted-foreground hover:text-foreground"
            >
              <LogOut />
              <span>{t('ui.admin.logout')}</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  );
}
