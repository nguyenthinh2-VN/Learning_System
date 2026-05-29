import '@/styles/brand.css';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { getProfileApi, updateProfileApi, changePasswordApi, uploadAvatarApi } from '@/api/wallet';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  User, Mail, Shield, Wallet, BookOpen,
  LogIn, RefreshCw, CheckCircle2, AlertCircle,
  Pencil, X, Save, Lock, Camera,
} from 'lucide-react';

function formatBalance(amount) {
  if (amount === null || amount === undefined) return '---';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(amount);
}

const ROLE_LABELS = {
  MEMBER: 'Học viên',
  INSTRUCTOR: 'Giảng viên',
  STAFF: 'Nhân viên',
  ADMIN_USER: 'Quản lý',
  SUPER_ADMIN: 'Quản trị viên',
};

const ROLE_COLORS = {
  MEMBER:      { bg: '#EFF6FF', text: '#2563EB', border: '#BFDBFE' },
  INSTRUCTOR:  { bg: '#ECFDF5', text: '#059669', border: '#A7F3D0' },
  STAFF:       { bg: '#FFFBEB', text: '#D97706', border: '#FDE68A' },
  ADMIN_USER:  { bg: '#F5F3FF', text: '#7C3AED', border: '#DDD6FE' },
  SUPER_ADMIN: { bg: '#FEF2F2', text: '#DC2626', border: '#FECACA' },
};

// ─── Edit Name Modal ──────────────────────────────────────
function EditNameModal({ currentName, onClose, onSaved }) {
  const [name, setName] = useState(currentName || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) { setError('Tên không được để trống.'); return; }
    setLoading(true);
    try {
      const res = await updateProfileApi({ name: name.trim() });
      onSaved(res.data.data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Cập nhật thất bại.');
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl border shadow-xl w-full max-w-sm mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold" style={{ color: 'var(--brand-text-primary)' }}>
            Chỉnh sửa họ tên
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100">
            <X className="size-4" />
          </button>
        </div>
        {error && (
          <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-red-50 text-red-600 border border-red-200">
            <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="edit-name">Họ tên mới</Label>
            <Input
              id="edit-name"
              value={name}
              onChange={(e) => { setName(e.target.value); setError(''); }}
              placeholder="Nguyễn Văn A"
              autoFocus
            />
          </div>
          <div className="flex gap-2 pt-1">
            <Button type="button" variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
            <Button type="submit" disabled={loading} className="flex-1">
              {loading ? <RefreshCw className="size-4 animate-spin mr-2" /> : <Save className="size-4 mr-2" />}
              Lưu
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Change Password Modal ────────────────────────────────
function ChangePasswordModal({ onClose }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setForm((p) => ({ ...p, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.currentPassword || !form.newPassword) { setError('Vui lòng nhập đầy đủ.'); return; }
    if (form.newPassword.length < 6) { setError('Mật khẩu mới tối thiểu 6 ký tự.'); return; }
    if (form.newPassword !== form.confirmPassword) { setError('Mật khẩu xác nhận không khớp.'); return; }
    setLoading(true);
    try {
      await changePasswordApi({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      setSuccess(true);
      setTimeout(onClose, 1500);
    } catch (err) {
      setError(err?.response?.data?.message || 'Đổi mật khẩu thất bại.');
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl border shadow-xl w-full max-w-sm mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold" style={{ color: 'var(--brand-text-primary)' }}>
            Đổi mật khẩu
          </h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100">
            <X className="size-4" />
          </button>
        </div>

        {success ? (
          <div className="flex items-center gap-2 text-sm px-3 py-3 rounded-lg bg-green-50 text-green-600 border border-green-200">
            <CheckCircle2 className="size-4" />Đổi mật khẩu thành công!
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3">
            {error && (
              <div className="flex items-start gap-2 text-sm px-3 py-2.5 rounded-lg bg-red-50 text-red-600 border border-red-200">
                <AlertCircle className="size-4 shrink-0 mt-0.5" /><span>{error}</span>
              </div>
            )}
            <div className="space-y-1.5">
              <Label htmlFor="cur-pw">Mật khẩu hiện tại</Label>
              <Input id="cur-pw" name="currentPassword" type="password" value={form.currentPassword} onChange={handleChange} placeholder="••••••••" autoFocus />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="new-pw">Mật khẩu mới</Label>
              <Input id="new-pw" name="newPassword" type="password" value={form.newPassword} onChange={handleChange} placeholder="Tối thiểu 6 ký tự" />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="confirm-pw">Xác nhận mật khẩu mới</Label>
              <Input id="confirm-pw" name="confirmPassword" type="password" value={form.confirmPassword} onChange={handleChange} placeholder="Nhập lại mật khẩu mới" />
            </div>
            <div className="flex gap-2 pt-1">
              <Button type="button" variant="outline" onClick={onClose} className="flex-1">Hủy</Button>
              <Button type="submit" disabled={loading} className="flex-1">
                {loading ? <RefreshCw className="size-4 animate-spin mr-2" /> : <Lock className="size-4 mr-2" />}
                Đổi mật khẩu
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

// ─── Info Row ─────────────────────────────────────────────
function InfoRow({ icon: Icon, label, value, mono = false, action }) {
  return (
    <div className="flex items-center gap-4 py-4 border-b last:border-0" style={{ borderColor: 'var(--brand-border)' }}>
      <div
        className="size-9 rounded-lg flex items-center justify-center shrink-0"
        style={{ background: 'var(--brand-primary-light)' }}
      >
        <Icon className="size-4" style={{ color: 'var(--brand-primary)' }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs mb-0.5" style={{ color: 'var(--brand-text-secondary)' }}>{label}</p>
        <p className={`text-sm font-medium truncate ${mono ? 'font-mono' : ''}`} style={{ color: 'var(--brand-text-primary)' }}>
          {value}
        </p>
      </div>
      {action && (
        <button
          onClick={action.onClick}
          className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors hover:opacity-80"
          style={{
            borderColor: 'var(--brand-primary-mid)',
            color: 'var(--brand-primary)',
            background: 'var(--brand-primary-light)',
          }}
        >
          <Pencil className="size-3" />
          {action.label}
        </button>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────
export default function ProfilePage() {
  const navigate = useNavigate();
  const { isPublicAuthenticated, fetchProfile } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [showEditName, setShowEditName] = useState(false);
  const [showChangePw, setShowChangePw] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarError, setAvatarError] = useState('');
  const fileInputRef = useRef(null);

  const loadProfile = async (showRefresh = false) => {
    if (showRefresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      const res = await getProfileApi();
      setProfile(res.data.data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải thông tin cá nhân.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
  const MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

  const handleAvatarSelected = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // reset để chọn lại cùng file vẫn trigger
    if (!file) return;

    setAvatarError('');
    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      setAvatarError('Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.');
      return;
    }
    if (file.size > MAX_AVATAR_SIZE) {
      setAvatarError('Ảnh vượt quá 2MB. Vui lòng chọn ảnh nhỏ hơn.');
      return;
    }

    setUploadingAvatar(true);
    try {
      const res = await uploadAvatarApi(file);
      setProfile(res.data.data);
      // Đồng bộ context để sidebar/header tự cập nhật avatar ngay (không cần F5)
      fetchProfile();
    } catch (err) {
      setAvatarError(err?.response?.data?.message || 'Tải ảnh lên thất bại.');
    } finally {
      setUploadingAvatar(false);
    }
  };

  useEffect(() => {
    if (isPublicAuthenticated) loadProfile();
    else setLoading(false);
  }, [isPublicAuthenticated]);

  if (!isPublicAuthenticated) {
    return (
      <div className="max-w-xl mx-auto px-6 py-24 text-center space-y-4">
        <LogIn className="size-12 mx-auto" style={{ color: 'var(--brand-text-tertiary)' }} />
        <h2 className="text-xl font-bold" style={{ color: 'var(--brand-text-primary)' }}>
          Đăng nhập để xem thông tin
        </h2>
        <p className="text-sm" style={{ color: 'var(--brand-text-secondary)' }}>
          Bạn cần đăng nhập để xem thông tin cá nhân.
        </p>
        <Button onClick={() => navigate('/login')}>Đăng nhập ngay</Button>
      </div>
    );
  }

  const roleColor = profile ? (ROLE_COLORS[profile.role] || { bg: '#F1F5F9', text: '#64748B', border: '#E2E8F0' }) : null;

  return (
    <div className="max-w-2xl mx-auto px-6 py-10 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--brand-text-primary)' }}>
            Thông tin cá nhân
          </h1>
          <p className="text-sm mt-1" style={{ color: 'var(--brand-text-secondary)' }}>
            Quản lý thông tin tài khoản của bạn
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => loadProfile(true)} disabled={refreshing}>
          <RefreshCw className={`size-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      {error && (
        <div className="flex items-center gap-2 text-sm px-4 py-3 rounded-lg bg-red-50 text-red-600 border border-red-200">
          <AlertCircle className="size-4 shrink-0" /><span>{error}</span>
        </div>
      )}

      {/* Avatar + Name card */}
      {loading ? (
        <div className="rounded-2xl border p-6 space-y-4" style={{ borderColor: 'var(--brand-border)' }}>
          <div className="flex items-center gap-4">
            <Skeleton className="size-20 rounded-full" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-5 w-40" />
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-3 w-32" />
            </div>
          </div>
        </div>
      ) : profile && (
        <div
          className="rounded-2xl p-6"
          style={{
            background: 'linear-gradient(135deg, var(--brand-primary-light) 0%, #F5F3FF 100%)',
            border: '1px solid var(--brand-primary-mid)',
          }}
        >
          <div className="flex items-center gap-5">
            {/* Avatar */}
            <div className="relative shrink-0">
              {profile.avatarUrl ? (
                <img
                  src={profile.avatarUrl}
                  alt={profile.name}
                  className="size-20 rounded-full object-cover shadow-lg bg-white"
                  onError={(e) => { e.currentTarget.style.display = 'none'; e.currentTarget.nextSibling.style.display = 'flex'; }}
                />
              ) : null}
              <div
                className="size-20 rounded-full items-center justify-center text-3xl font-extrabold text-white shadow-lg"
                style={{
                  background: 'var(--brand-gradient-btn)',
                  display: profile.avatarUrl ? 'none' : 'flex',
                }}
              >
                {profile.name?.charAt(0)?.toUpperCase() || 'U'}
              </div>

              {/* Upload overlay button */}
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingAvatar}
                title="Đổi ảnh đại diện"
                className="absolute -bottom-1 -right-1 size-8 rounded-full flex items-center justify-center shadow-md border-2 border-white transition-transform hover:scale-105 disabled:opacity-70"
                style={{ background: 'var(--brand-primary)' }}
              >
                {uploadingAvatar
                  ? <RefreshCw className="size-4 text-white animate-spin" />
                  : <Camera className="size-4 text-white" />}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={handleAvatarSelected}
              />
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-xl font-bold truncate" style={{ color: 'var(--brand-text-primary)' }}>
                  {profile.name}
                </h2>
                <button
                  onClick={() => setShowEditName(true)}
                  className="flex items-center gap-1 text-xs px-2 py-1 rounded-lg border transition-colors"
                  style={{
                    borderColor: 'var(--brand-primary-mid)',
                    color: 'var(--brand-primary)',
                    background: 'white',
                  }}
                >
                  <Pencil className="size-3" />Sửa
                </button>
              </div>

              <p className="text-sm mt-0.5" style={{ color: 'var(--brand-text-secondary)' }}>
                @{profile.username}
              </p>

              <div className="flex items-center gap-2 mt-2 flex-wrap">
                {roleColor && (
                  <span
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold border leading-none"
                    style={{ background: roleColor.bg, color: roleColor.text, borderColor: roleColor.border }}
                  >
                    {ROLE_LABELS[profile.role] || profile.role}
                  </span>
                )}
                {profile.isInternal && (
                  <span
                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold border leading-none"
                    style={{ background: '#ECFDF5', color: '#FF0000', borderColor: '#A7F3D0' }}
                  >
                    <CheckCircle2 className="size-3" />Nội bộ
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Avatar upload error / hint */}
      {avatarError && (
        <div className="flex items-center gap-2 text-sm px-4 py-2.5 rounded-lg bg-red-50 text-red-600 border border-red-200">
          <AlertCircle className="size-4 shrink-0" /><span>{avatarError}</span>
        </div>
      )}

      {/* Info details */}
      {loading ? (
        <div className="rounded-2xl border p-6 space-y-2" style={{ borderColor: 'var(--brand-border)' }}>
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="flex items-center gap-4 py-3">
              <Skeleton className="size-9 rounded-lg" />
              <div className="space-y-1.5 flex-1">
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-4 w-48" />
              </div>
            </div>
          ))}
        </div>
      ) : profile && (
        <div
          className="rounded-2xl px-6 bg-white"
          style={{ border: '1px solid var(--brand-border)' }}
        >
          <InfoRow
            icon={User}
            label="Họ tên"
            value={profile.name}
            action={{ label: 'Chỉnh sửa', onClick: () => setShowEditName(true) }}
          />
          <InfoRow icon={Mail} label="Email" value={profile.email} />
          <InfoRow icon={Shield} label="Tên đăng nhập" value={profile.username} mono />
          <InfoRow
            icon={Wallet}
            label="Số dư ví"
            value={formatBalance(profile.balance)}
          />
        </div>
      )}

      {/* Action buttons */}
      {!loading && profile && (
        <div className="space-y-3">
          {/* Change password */}
          <button
            onClick={() => setShowChangePw(true)}
            className="w-full flex items-center gap-3 p-4 rounded-xl border transition-colors text-left group"
            style={{ borderColor: 'var(--brand-border)', background: 'white' }}
          >
            <div
              className="size-10 rounded-lg flex items-center justify-center shrink-0"
              style={{ background: 'var(--brand-primary-light)' }}
            >
              <Lock className="size-5" style={{ color: 'var(--brand-primary)' }} />
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium" style={{ color: 'var(--brand-text-primary)' }}>
                Đổi mật khẩu
              </p>
              <p className="text-xs" style={{ color: 'var(--brand-text-secondary)' }}>
                Cập nhật mật khẩu để bảo mật tài khoản
              </p>
            </div>
            <Pencil className="size-4 opacity-40 group-hover:opacity-100 transition-opacity" style={{ color: 'var(--brand-primary)' }} />
          </button>

          {/* Quick links */}
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => navigate('/wallet')}
              className="group flex items-center gap-3 p-4 rounded-xl border transition-colors text-left"
              style={{ borderColor: 'var(--brand-border)', background: 'white' }}
            >
              <div
                className="size-10 rounded-lg flex items-center justify-center shrink-0"
                style={{ background: 'var(--brand-primary-light)' }}
              >
                <Wallet className="size-5" style={{ color: 'var(--brand-primary)' }} />
              </div>
              <div>
                <p className="text-sm font-medium" style={{ color: 'var(--brand-text-primary)' }}>Ví tiền</p>
                <p className="text-xs" style={{ color: 'var(--brand-success)' }}>
                  {formatBalance(profile.balance)}
                </p>
              </div>
            </button>

            <button
              onClick={() => navigate('/my-courses')}
              className="group flex items-center gap-3 p-4 rounded-xl border transition-colors text-left"
              style={{ borderColor: 'var(--brand-border)', background: 'white' }}
            >
              <div
                className="size-10 rounded-lg flex items-center justify-center shrink-0"
                style={{ background: 'var(--brand-primary-light)' }}
              >
                <BookOpen className="size-5" style={{ color: 'var(--brand-primary)' }} />
              </div>
              <div>
                <p className="text-sm font-medium" style={{ color: 'var(--brand-text-primary)' }}>Khóa học</p>
                <p className="text-xs" style={{ color: 'var(--brand-text-secondary)' }}>Đã đăng ký</p>
              </div>
            </button>
          </div>
        </div>
      )}

      {/* Modals */}
      {showEditName && (
        <EditNameModal
          currentName={profile?.name}
          onClose={() => setShowEditName(false)}
          onSaved={(updatedProfile) => {
            setProfile((p) => ({ ...p, ...updatedProfile }));
            // Đồng bộ context để sidebar/header cập nhật tên ngay
            fetchProfile();
            setShowEditName(false);
          }}
        />
      )}
      {showChangePw && <ChangePasswordModal onClose={() => setShowChangePw(false)} />}
    </div>
  );
}
