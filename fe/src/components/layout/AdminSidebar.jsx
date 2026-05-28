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
  SidebarRail,
  SidebarSeparator,
} from '@/components/ui/sidebar';
import {
  LayoutDashboard,
  Users,
  BookOpen,
  Ticket,
  Wallet,
  LogOut,
  ShieldCheck,
  ClipboardCheck,
  GraduationCap,
} from 'lucide-react';

const ROLE_LABELS = {
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý',
  SUPER_ADMIN: 'Quản trị viên',
};

/**
 * Ma trận phân quyền theo permission-matrix.md
 * INSTRUCTOR: CREATE_COURSE, EDIT_COURSE, DELETE_COURSE, CREATE_SECTION, EDIT_SECTION, CREATE_LESSON, EDIT_LESSON
 * STAFF: + PUBLISH_COURSE, LOCK_COURSE_PRICE, MANAGE_VOUCHER
 * ADMIN_USER: + VIEW_USER, CREATE_USER, EDIT_USER
 * SUPER_ADMIN: tất cả
 */
const NAV_GROUPS = [
  {
    label: 'Tổng quan',
    items: [
      { title: 'Dashboard', url: '/admin', icon: LayoutDashboard },
    ],
  },
  {
    label: 'Nội dung',
    items: [
      // INSTRUCTOR chỉ thấy "Khóa học của tôi" — tất cả role có CREATE_COURSE đều thấy
      { title: 'Khóa học', url: '/admin/courses', icon: BookOpen },
      // Chờ duyệt: chỉ STAFF, SUPER_ADMIN (PUBLISH_COURSE)
      { title: 'Chờ duyệt', url: '/admin/courses/pending', icon: ClipboardCheck, roles: ['STAFF', 'SUPER_ADMIN'] },
    ],
  },
  {
    label: 'Quản lý',
    items: [
      // VIEW_USER: ADMIN_USER, SUPER_ADMIN
      { title: 'Người dùng', url: '/admin/users', icon: Users, roles: ['ADMIN_USER', 'SUPER_ADMIN'] },
    ],
  },
  {
    label: 'Tài chính',
    items: [
      // MANAGE_VOUCHER: STAFF, SUPER_ADMIN
      { title: 'Voucher', url: '/admin/vouchers', icon: Ticket, roles: ['STAFF', 'SUPER_ADMIN'] },
      // Cộng tiền: chỉ SUPER_ADMIN
      { title: 'Cộng tiền', url: '/admin/wallet', icon: Wallet, roles: ['SUPER_ADMIN'] },
    ],
  },
];

export default function AdminSidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { adminUser, adminLogout } = useAuth();

  const role = adminUser?.role;
  const isInstructor = role === 'INSTRUCTOR';
  const canAccess = (item) => !item.roles || item.roles.includes(role);

  const isActive = (url) =>
    url === '/admin'
      ? location.pathname === '/admin'
      : location.pathname.startsWith(url);

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
                  ? 'bg-gradient-to-br from-emerald-500/30 to-teal-500/30 text-emerald-200 border-emerald-500/30'
                  : 'bg-gradient-to-br from-indigo-500/30 to-purple-500/30 text-indigo-200 border-indigo-500/30'
              }`}>
                {adminUser?.name?.charAt(0)?.toUpperCase() || 'A'}
              </div>
              <div className="flex flex-col flex-1 text-left text-sm leading-tight min-w-0">
                <span className="truncate font-semibold">{adminUser?.name}</span>
                <span className="truncate text-xs text-muted-foreground">{adminUser?.email}</span>
                <span className={`truncate text-[11px] mt-0.5 font-medium ${
                  isInstructor ? 'text-emerald-400' : 'text-indigo-400'
                }`}>
                  {ROLE_LABELS[role] || role}
                </span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
          <SidebarMenuItem>
            <SidebarMenuButton
              tooltip="Đăng xuất"
              onClick={handleLogout}
              className="text-muted-foreground hover:text-foreground"
            >
              <LogOut />
              <span>Đăng xuất</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  );
}
