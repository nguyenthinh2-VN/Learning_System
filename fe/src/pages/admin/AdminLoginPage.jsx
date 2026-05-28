import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminLoginApi } from '@/api/adminApi';
import { useAuth } from '@/context/AuthContext';
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
import { ShieldCheck, Loader2, Eye, EyeOff } from 'lucide-react';

const ADMIN_ROLES = ['INSTRUCTOR', 'STAFF', 'ADMIN_USER', 'SUPER_ADMIN'];

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const { adminLogin, isAdminAuthenticated } = useAuth();

  const [form, setForm] = useState({ identifier: '', password: '' });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  if (isAdminAuthenticated) {
    navigate('/admin', { replace: true });
    return null;
  }

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
      const res = await adminLoginApi({ identifier: form.identifier, password: form.password });
      const data = res.data.data;

      if (!ADMIN_ROLES.includes(data.role)) {
        setApiError('Tài khoản này không có quyền truy cập hệ thống quản trị.');
        return;
      }

      adminLogin(
        {
          id: data.id,
          username: data.username,
          email: data.email,
          name: data.name,
          role: data.role,
          isInternal: data.isInternal,
        },
        data.accessToken
      );

      navigate('/admin', { replace: true });
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
    <div className="dark">
      <div className="h-screen flex items-center justify-center bg-background px-4">
        <div className="w-full max-w-sm">
          {/* Brand */}
          <div className="mb-8 text-center">
            <div className="flex items-center justify-center mb-3">
              <div className="size-12 rounded-xl bg-primary text-primary-foreground flex items-center justify-center">
                <ShieldCheck className="size-6" />
              </div>
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">LearnSpace</h1>
            <Badge variant="outline" className="mt-2 text-xs tracking-widest font-mono">
              ADMIN PORTAL
            </Badge>
          </div>

          <Card>
            <CardHeader className="space-y-1 pb-4">
              <CardTitle className="text-lg font-semibold">Đăng nhập quản trị</CardTitle>
              <CardDescription>
                Dành cho giảng viên, nhân viên và quản trị viên
              </CardDescription>
            </CardHeader>

            <Separator />

            <form onSubmit={handleSubmit} noValidate>
              <CardContent className="space-y-4 pt-4">
                {apiError && (
                  <div
                    id="admin-login-error"
                    role="alert"
                    className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2"
                  >
                    {apiError}
                  </div>
                )}

                <div className="space-y-1.5">
                  <Label htmlFor="admin-identifier">Email hoặc tên đăng nhập</Label>
                  <Input
                    id="admin-identifier"
                    name="identifier"
                    type="text"
                    autoComplete="username"
                    placeholder="admin@learnspace.vn"
                    value={form.identifier}
                    onChange={handleChange}
                    aria-invalid={!!errors.identifier}
                    disabled={loading}
                  />
                  {errors.identifier && (
                    <p className="text-xs text-destructive">{errors.identifier}</p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="admin-password">Mật khẩu</Label>
                  <div className="relative">
                    <Input
                      id="admin-password"
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="current-password"
                      placeholder="••••••••"
                      value={form.password}
                      onChange={handleChange}
                      aria-invalid={!!errors.password}
                      disabled={loading}
                      className="pr-10"
                    />
                    <button
                      type="button"
                      className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-muted-foreground hover:text-foreground"
                      onClick={() => setShowPassword((v) => !v)}
                      tabIndex={-1}
                    >
                      {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    </button>
                  </div>
                  {errors.password && (
                    <p className="text-xs text-destructive">{errors.password}</p>
                  )}
                </div>
              </CardContent>

              <CardFooter className="flex flex-col gap-3 pt-2">
                <Button
                  id="admin-login-submit"
                  type="submit"
                  className="w-full"
                  disabled={loading}
                >
                  {loading && <Loader2 className="size-4 animate-spin mr-2" />}
                  {loading ? 'Đang xác minh…' : 'Đăng nhập'}
                </Button>
              </CardFooter>
            </form>
          </Card>

          <p className="text-center text-xs text-muted-foreground mt-6">
            Truy cập trái phép sẽ bị ghi nhận và xử lý.
          </p>
        </div>
      </div>
    </div>
  );
}
