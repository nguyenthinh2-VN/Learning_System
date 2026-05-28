import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

const ROLE_LABELS = {
  MEMBER: 'Học viên',
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý tài khoản',
  SUPER_ADMIN: 'Quản trị viên',
};

export default function DashboardContent({ user, onLogout }) {
  return (
    <div className="min-h-screen bg-foreground text-background">
      {/* Top Bar */}
      <header className="border-b border-background/10 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-sm font-mono tracking-widest text-background/40 uppercase">
            LearnSpace
          </span>
          <Separator orientation="vertical" className="h-4 bg-background/15" />
          <Badge
            variant="outline"
            className="border-background/20 text-background/60 text-xs tracking-widest font-mono"
          >
            DASHBOARD
          </Badge>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <p className="text-sm text-background font-medium">{user.name}</p>
            <p className="text-xs text-background/40">{user.email}</p>
          </div>
          <Button
            id="dashboard-logout"
            size="sm"
            onClick={onLogout}
            className="text-xs cursor-pointer"
            style={{
              backgroundColor: 'transparent',
              color: 'rgba(255,255,255,0.7)',
              border: '1px solid rgba(255,255,255,0.2)',
            }}
          >
            Đăng xuất
          </Button>
        </div>
      </header>

      {/* Main */}
      <main className="max-w-4xl mx-auto px-6 py-12">
        {/* Welcome */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-background">
            Xin chào, {user.name}
          </h2>
          <p className="text-sm text-background/50 mt-1">
            Đây là khu vực quản trị nội bộ của LearnSpace.
          </p>
        </div>

        {/* Info Card */}
        <div className="bg-background/5 border border-background/10 rounded-lg p-6 mb-6">
          <h3 className="text-sm font-medium text-background/70 uppercase tracking-wider mb-4">
            Thông tin tài khoản
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <InfoRow label="Tên" value={user.name} />
            <InfoRow label="Username" value={user.username} />
            <InfoRow label="Email" value={user.email} />
            <InfoRow
              label="Vai trò"
              value={
                <Badge
                  variant="outline"
                  className="border-background/20 text-background/80 font-mono text-xs"
                >
                  {ROLE_LABELS[user.role] || user.role}
                </Badge>
              }
            />
            <InfoRow
              label="Loại tài khoản"
              value={
                <Badge
                  variant="outline"
                  className="border-background/20 text-background/80 font-mono text-xs"
                >
                  {user.isInternal ? 'Nội bộ' : 'Bên ngoài'}
                </Badge>
              }
            />
          </div>
        </div>

        {/* Placeholder notice */}
        <div className="bg-background/5 border border-dashed border-background/15 rounded-lg p-6 text-center">
          <p className="text-sm text-background/40">
            Dashboard đang được phát triển. Các module quản lý sẽ xuất hiện tại đây.
          </p>
        </div>
      </main>
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div>
      <p className="text-xs text-background/40 mb-1">{label}</p>
      {typeof value === 'string' ? (
        <p className="text-sm text-background font-medium">{value}</p>
      ) : (
        value
      )}
    </div>
  );
}
