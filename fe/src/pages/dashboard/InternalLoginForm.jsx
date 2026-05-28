import { useState } from 'react';
import { loginApi } from '@/api/auth';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';

const INTERNAL_ROLES = ['STAFF', 'ADMIN_USER', 'SUPER_ADMIN', 'INSTRUCTOR'];

/**
 * Kiểm tra xem user có quyền nội bộ không
 */
function isInternalUser(data) {
  return data.isInternal === true || INTERNAL_ROLES.includes(data.role);
}

export default function InternalLoginForm({ onSuccess }) {
  const [form, setForm] = useState({ identifier: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errs = {};
    if (!form.identifier.trim()) errs.identifier = 'Vui lòng nhập email hoặc tên đăng nhập.';
    if (!form.password) errs.password = 'Vui lòng nhập mật khẩu.';
    return errs;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: '' }));
    setApiError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }

    setLoading(true);
    try {
      const res = await loginApi({ identifier: form.identifier, password: form.password });
      const data = res.data.data;

      if (!isInternalUser(data)) {
        setApiError('Tài khoản này không có quyền truy cập vào hệ thống nội bộ.');
        return;
      }

      // Lưu internal token riêng biệt
      localStorage.setItem('internalToken', data.accessToken);
      localStorage.setItem(
        'internalUser',
        JSON.stringify({
          id: data.id,
          username: data.username,
          email: data.email,
          name: data.name,
          role: data.role,
          isInternal: data.isInternal,
        })
      );

      onSuccess({
        id: data.id,
        username: data.username,
        email: data.email,
        name: data.name,
        role: data.role,
        isInternal: data.isInternal,
      });
    } catch (err) {
      if (err?.response?.status === 401) {
        setApiError('Email/tên đăng nhập hoặc mật khẩu không đúng.');
      } else {
        setApiError(err?.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-screen flex items-center justify-center px-4" style={{ backgroundColor: '#0a0a0a' }}>
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="mb-8 text-center">
          <p className="text-xs font-mono tracking-widest text-background/40 uppercase mb-3">
            LearnSpace
          </p>
          <Badge
            id="internal-badge"
            variant="outline"
            className="border-background/20 text-background/60 text-xs tracking-widest font-mono px-3 py-1"
          >
            INTERNAL ACCESS
          </Badge>
        </div>

        <Card className="shadow-none" style={{ backgroundColor: '#0a0a0a', borderColor: 'rgba(255,255,255,0.08)' }}>
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-lg font-semibold text-background">
              Đăng nhập nội bộ
            </CardTitle>
            <CardDescription className="text-sm text-background/50">
              Chỉ dành cho nhân viên, giảng viên và quản trị viên
            </CardDescription>
          </CardHeader>

          <Separator className="bg-background/10 mb-4" />

          <form onSubmit={handleSubmit} noValidate>
            <CardContent className="space-y-4">
              {/* API Error */}
              {apiError && (
                <div
                  id="internal-login-error"
                  role="alert"
                  className="text-sm text-red-400 bg-red-400/10 border border-red-400/20 rounded-md px-3 py-2"
                >
                  {apiError}
                </div>
              )}

              {/* Identifier */}
              <div className="space-y-1.5">
                <Label htmlFor="internal-identifier" className="text-background/70 text-sm">
                  Email hoặc tên đăng nhập
                </Label>
                <Input
                  id="internal-identifier"
                  name="identifier"
                  type="text"
                  autoComplete="username"
                  placeholder="email@company.com"
                  value={form.identifier}
                  onChange={handleChange}
                  aria-invalid={!!errors.identifier}
                  disabled={loading}
                  className="bg-background/5 border-background/15 text-background placeholder:text-background/30 focus-visible:ring-background/30"
                />
                {errors.identifier && (
                  <p className="text-xs text-red-400">{errors.identifier}</p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <Label htmlFor="internal-password" className="text-background/70 text-sm">
                  Mật khẩu
                </Label>
                <Input
                  id="internal-password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={handleChange}
                  aria-invalid={!!errors.password}
                  disabled={loading}
                  className="bg-background/5 border-background/15 text-background placeholder:text-background/30 focus-visible:ring-background/30"
                />
                {errors.password && (
                  <p className="text-xs text-red-400">{errors.password}</p>
                )}
              </div>
            </CardContent>

            <CardFooter className="pt-2">
              <Button
                id="internal-login-submit"
                type="submit"
                className="w-full"
                disabled={loading}
                style={{ backgroundColor: '#ffffff', color: '#0a0a0a', borderColor: 'transparent' }}
              >
                {loading ? 'Đang xác minh…' : 'Đăng nhập'}
              </Button>
            </CardFooter>
          </form>
        </Card>

        <p className="text-center text-xs text-background/30 mt-6">
          Truy cập trái phép sẽ bị ghi nhận và xử lý.
        </p>
      </div>
    </div>
  );
}
