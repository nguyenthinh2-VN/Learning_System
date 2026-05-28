import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerApi } from '@/api/auth';
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

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = 'Vui lòng nhập họ tên.';
    if (!form.email.trim()) {
      errs.email = 'Vui lòng nhập email.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errs.email = 'Email không đúng định dạng.';
    }
    if (!form.password) {
      errs.password = 'Vui lòng nhập mật khẩu.';
    } else if (form.password.length < 6) {
      errs.password = 'Mật khẩu phải có ít nhất 6 ký tự.';
    }
    if (!form.confirmPassword) {
      errs.confirmPassword = 'Vui lòng xác nhận mật khẩu.';
    } else if (form.password !== form.confirmPassword) {
      errs.confirmPassword = 'Mật khẩu xác nhận không khớp.';
    }
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
      await registerApi({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      setSuccessMsg('Đăng ký thành công! Đang chuyển đến trang đăng nhập…');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      const code = err?.response?.data?.code;
      const msg = err?.response?.data?.message;
      if (code === 'EMAIL_ALREADY_EXISTS') {
        setErrors((prev) => ({ ...prev, email: 'Email này đã được đăng ký.' }));
      } else if (code === 'VALIDATION_ERROR') {
        setApiError(msg || 'Dữ liệu không hợp lệ.');
      } else {
        setApiError(msg || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4 py-8">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">LearnSpace</h1>
          <p className="text-sm text-muted-foreground mt-1">Nền tảng học trực tuyến</p>
        </div>

        <Card className="shadow-sm">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-lg font-semibold">Tạo tài khoản</CardTitle>
            <CardDescription className="text-sm">
              Điền thông tin để bắt đầu học ngay hôm nay
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleSubmit} noValidate>
            <CardContent className="space-y-4">
              {/* Success */}
              {successMsg && (
                <div
                  id="register-success"
                  role="status"
                  className="text-sm text-foreground bg-muted border border-border rounded-md px-3 py-2"
                >
                  {successMsg}
                </div>
              )}

              {/* API Error */}
              {apiError && (
                <div
                  id="register-error"
                  role="alert"
                  className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2"
                >
                  {apiError}
                </div>
              )}

              {/* Name */}
              <div className="space-y-1.5">
                <Label htmlFor="name">Họ và tên</Label>
                <Input
                  id="name"
                  name="name"
                  type="text"
                  autoComplete="name"
                  placeholder="Nguyễn Văn A"
                  value={form.name}
                  onChange={handleChange}
                  aria-invalid={!!errors.name}
                  disabled={loading}
                />
                {errors.name && (
                  <p className="text-xs text-destructive">{errors.name}</p>
                )}
              </div>

              {/* Email */}
              <div className="space-y-1.5">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder="email@example.com"
                  value={form.email}
                  onChange={handleChange}
                  aria-invalid={!!errors.email}
                  disabled={loading}
                />
                {errors.email && (
                  <p className="text-xs text-destructive">{errors.email}</p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <Label htmlFor="reg-password">Mật khẩu</Label>
                <Input
                  id="reg-password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Tối thiểu 6 ký tự"
                  value={form.password}
                  onChange={handleChange}
                  aria-invalid={!!errors.password}
                  disabled={loading}
                />
                {errors.password && (
                  <p className="text-xs text-destructive">{errors.password}</p>
                )}
              </div>

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
                <Input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Nhập lại mật khẩu"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  aria-invalid={!!errors.confirmPassword}
                  disabled={loading}
                />
                {errors.confirmPassword && (
                  <p className="text-xs text-destructive">{errors.confirmPassword}</p>
                )}
              </div>
            </CardContent>

            <CardFooter className="flex flex-col gap-3 pt-2">
              <Button
                id="register-submit"
                type="submit"
                className="w-full"
                disabled={loading || !!successMsg}
              >
                {loading ? 'Đang đăng ký…' : 'Đăng ký'}
              </Button>

              <p className="text-center text-sm text-muted-foreground">
                Đã có tài khoản?{' '}
                <Link
                  to="/login"
                  className="font-medium text-foreground underline-offset-4 hover:underline"
                >
                  Đăng nhập
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  );
}
