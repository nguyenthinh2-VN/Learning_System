import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginApi } from '@/api/auth';
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

export default function LoginPage() {
  const navigate = useNavigate();
  const { publicLogin, fetchProfile } = useAuth();

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

      // Lưu thông tin cơ bản + token vào AuthContext
      publicLogin(
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

      // Gọi profile API để lấy balance ngay sau khi có token
      await fetchProfile();

      // Redirect theo role
      const role = data.role;
      if (role === 'INSTRUCTOR') {
        navigate('/instructor');
      } else if (['STAFF', 'ADMIN_USER', 'SUPER_ADMIN'].includes(role)) {
        navigate('/admin');
      } else {
        navigate('/');
      }
    } catch (err) {
      const msg = err?.response?.data?.message;
      if (err?.response?.status === 401) {
        setApiError('Email/tên đăng nhập hoặc mật khẩu không đúng.');
      } else {
        setApiError(msg || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">LearnSpace</h1>
          <p className="text-sm text-muted-foreground mt-1">Nền tảng học trực tuyến</p>
        </div>

        <Card className="shadow-sm">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-lg font-semibold">Đăng nhập</CardTitle>
            <CardDescription className="text-sm">
              Nhập thông tin để truy cập tài khoản của bạn
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleSubmit} noValidate>
            <CardContent className="space-y-4">
              {/* API Error */}
              {apiError && (
                <div
                  id="login-error"
                  role="alert"
                  className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2"
                >
                  {apiError}
                </div>
              )}

              {/* Identifier */}
              <div className="space-y-1.5">
                <Label htmlFor="identifier">Email hoặc tên đăng nhập</Label>
                <Input
                  id="identifier"
                  name="identifier"
                  type="text"
                  autoComplete="username"
                  placeholder="email@example.com"
                  value={form.identifier}
                  onChange={handleChange}
                  aria-invalid={!!errors.identifier}
                  aria-describedby={errors.identifier ? 'identifier-error' : undefined}
                  disabled={loading}
                />
                {errors.identifier && (
                  <p id="identifier-error" className="text-xs text-destructive">
                    {errors.identifier}
                  </p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <Label htmlFor="password">Mật khẩu</Label>
                <Input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={handleChange}
                  aria-invalid={!!errors.password}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                  disabled={loading}
                />
                {errors.password && (
                  <p id="password-error" className="text-xs text-destructive">
                    {errors.password}
                  </p>
                )}
              </div>
            </CardContent>

            <CardFooter className="flex flex-col gap-3 pt-2">
              <Button
                id="login-submit"
                type="submit"
                className="w-full"
                disabled={loading}
              >
                {loading ? 'Đang đăng nhập…' : 'Đăng nhập'}
              </Button>

              <p className="text-center text-sm text-muted-foreground">
                Chưa có tài khoản?{' '}
                <Link
                  to="/register"
                  className="font-medium text-foreground underline-offset-4 hover:underline"
                >
                  Đăng ký ngay
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
}
