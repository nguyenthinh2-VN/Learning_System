import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { quoteApi, purchaseApi } from '@/api/wallet';
import useCourseDetailStore from '@/store/useCourseDetailStore';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  BookOpen,
  Tag,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Wallet,
  ShoppingCart,
  Loader2,
} from 'lucide-react';

function formatMoney(amount) {
  if (amount === 0) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

// Số dư ví: null/undefined → '---' (tránh hiển thị "NaN đ")
function formatBalance(amount) {
  if (amount == null) return '---';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

export default function PurchaseConfirmPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { publicUser, balance, fetchProfile } = useAuth();
  const { currentCourse, fetchCourseDetail, cache } = useCourseDetailStore();

  const [voucherInput, setVoucherInput] = useState('');
  const [quote, setQuote] = useState(null);        // preview giá từ server
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quoteError, setQuoteError] = useState('');

  const [purchasing, setPurchasing] = useState(false);
  const [purchaseSuccess, setPurchaseSuccess] = useState(false);
  const [purchaseError, setPurchaseError] = useState('');

  // Lấy course từ cache hoặc fetch
  useEffect(() => {
    if (id) {
      const courseId = Number(id);
      if (!cache[courseId]) {
        fetchCourseDetail(courseId);
      }
    }
  }, [id, cache, fetchCourseDetail]);

  // Quote mặc định không voucher khi vào trang
  useEffect(() => {
    if (id && currentCourse?.id === Number(id)) {
      handleQuote('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentCourse?.id]);

  // Nạp số dư ví nếu chưa có (vd: mở tab trực tiếp / refresh)
  useEffect(() => {
    if (balance == null) {
      fetchProfile();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleQuote = async (code) => {
    setQuoteLoading(true);
    setQuoteError('');
    try {
      const res = await quoteApi(Number(id), code);
      setQuote(res.data.data);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể kiểm tra voucher.';
      setQuoteError(msg);
      setQuote(null);
    } finally {
      setQuoteLoading(false);
    }
  };

  const handleApplyVoucher = () => {
    if (!voucherInput.trim()) return;
    handleQuote(voucherInput.trim().toUpperCase());
  };

  const handleClearVoucher = () => {
    setVoucherInput('');
    setQuoteError('');
    handleQuote('');
  };

  const handlePurchase = async () => {
    setPurchasing(true);
    setPurchaseError('');
    try {
      const res = await purchaseApi(
        Number(id),
        quote?.voucherApplied ? voucherInput.trim().toUpperCase() : undefined
      );
      void res;
      await fetchProfile(); // đồng bộ lại số dư từ BE sau khi mua
      setPurchaseSuccess(true);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Mua khóa học thất bại. Vui lòng thử lại.';
      setPurchaseError(msg);
    } finally {
      setPurchasing(false);
    }
  };

  const course = currentCourse?.id === Number(id) ? currentCourse : null;
  const finalPrice = quote?.finalPrice ?? course?.price ?? 0;
  const originalPrice = quote?.originalPrice ?? course?.price ?? 0;
  const discountAmount = quote?.discountAmount ?? 0;
  const hasDiscount = discountAmount > 0;
  const insufficientBalance = balance != null && balance < finalPrice;

  // ── Success screen ────────────────────────────────────────────────────────
  if (purchaseSuccess) {
    return (
      <div className="max-w-md mx-auto px-6 py-20 text-center space-y-4">
        <div className="size-16 rounded-full bg-emerald-50 flex items-center justify-center mx-auto">
          <CheckCircle2 className="size-8 text-emerald-600" />
        </div>
        <h2 className="text-xl font-bold">Đăng ký thành công!</h2>
        <p className="text-sm text-muted-foreground">
          Bạn đã đăng ký <span className="font-medium text-foreground">{course?.title}</span> thành công.
        </p>
        <div className="flex flex-col gap-2 pt-2">
          <Button onClick={() => navigate('/')} className="w-full">
            Về trang chủ
          </Button>
          <Button variant="outline" onClick={() => navigate(`/courses/${id}`)} className="w-full">
            Xem khóa học
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-xl mx-auto px-6 py-10">
      {/* Back */}
      <button
        onClick={() => navigate(`/courses/${id}`)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors mb-6"
      >
        <ArrowLeft className="size-4" />
        Quay lại
      </button>

      <h1 className="text-xl font-bold mb-6">Xác nhận đăng ký</h1>

      {/* ── Course Info ─────────────────────────────────────────────── */}
      <div className="border rounded-xl p-4 mb-4 flex gap-4">
        <div className="size-16 rounded-lg bg-muted flex items-center justify-center shrink-0">
          <BookOpen className="size-7 text-muted-foreground/30" />
        </div>
        <div className="flex-1 min-w-0">
          {!course ? (
            <>
              <Skeleton className="h-4 w-3/4 mb-2" />
              <Skeleton className="h-3 w-full" />
            </>
          ) : (
            <>
              <p className="font-semibold text-sm leading-snug">{course.title}</p>
              {course.description && (
                <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
                  {course.description}
                </p>
              )}
            </>
          )}
        </div>
      </div>

      {/* ── Voucher Input ───────────────────────────────────────────── */}
      <div className="border rounded-xl p-4 mb-4 space-y-3">
        <div className="flex items-center gap-2">
          <Tag className="size-4" />
          <p className="text-sm font-medium">Mã voucher</p>
        </div>

        <div className="flex gap-2">
          <Input
            id="voucher-input"
            placeholder="Nhập mã voucher (VD: WELCOME50)"
            value={voucherInput}
            onChange={(e) => setVoucherInput(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === 'Enter' && handleApplyVoucher()}
            className="uppercase font-mono tracking-widest text-sm"
          />
          <Button
            id="voucher-apply"
            variant="outline"
            onClick={handleApplyVoucher}
            disabled={!voucherInput.trim() || quoteLoading}
          >
            {quoteLoading ? <Loader2 className="size-4 animate-spin" /> : 'Xác nhận'}
          </Button>
        </div>

        {/* Voucher applied badge */}
        {quote?.voucherApplied && (
          <div className="flex items-center justify-between">
            <Badge className="bg-emerald-100 text-emerald-800 border-emerald-200 hover:bg-emerald-100">
              <CheckCircle2 className="size-3 mr-1" />
              {quote.voucherCode} — giảm {formatMoney(discountAmount)}
            </Badge>
            <button
              onClick={handleClearVoucher}
              className="text-xs text-muted-foreground hover:text-foreground"
            >
              Xóa
            </button>
          </div>
        )}

        {/* Voucher error */}
        {quoteError && (
          <div className="flex items-center gap-2 text-xs text-red-600">
            <AlertCircle className="size-3.5 shrink-0" />
            {quoteError}
          </div>
        )}
      </div>

      {/* ── Price Summary ───────────────────────────────────────────── */}
      <div className="border rounded-xl p-4 mb-4 space-y-3">
        <p className="text-sm font-medium mb-2">Chi tiết thanh toán</p>

        <div className="flex justify-between text-sm">
          <span className="text-muted-foreground">Giá gốc</span>
          <span className={hasDiscount ? 'line-through text-muted-foreground' : 'font-semibold'}>
            {formatMoney(originalPrice)}
          </span>
        </div>

        {hasDiscount && (
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Giảm giá</span>
            <span className="text-emerald-600 font-medium">−{formatMoney(discountAmount)}</span>
          </div>
        )}

        <div className="border-t pt-3 flex justify-between items-center">
          <span className="font-semibold">Thành tiền</span>
          <span
            className={`text-xl font-bold ${
              hasDiscount ? 'text-emerald-600' : 'text-foreground'
            }`}
          >
            {quoteLoading ? (
              <Skeleton className="h-6 w-24 inline-block" />
            ) : (
              formatMoney(finalPrice)
            )}
          </span>
        </div>
      </div>

      {/* ── Wallet Balance ──────────────────────────────────────────── */}
      <div className={`border rounded-xl p-4 mb-6 flex items-center justify-between ${
        insufficientBalance ? 'border-red-200 bg-red-50' : ''
      }`}>
        <div className="flex items-center gap-2">
          <Wallet className={`size-4 ${insufficientBalance ? 'text-red-500' : 'text-muted-foreground'}`} />
          <span className="text-sm">Số dư ví</span>
        </div>
        <div className="text-right">
          <p className={`font-semibold text-sm ${insufficientBalance ? 'text-red-600' : ''}`}>
            {formatBalance(balance)}
          </p>
          {insufficientBalance && (
            <p className="text-xs text-red-500 mt-0.5">
              Thiếu {formatMoney(finalPrice - balance)} —{' '}
              <button
                onClick={() => navigate('/wallet')}
                className="underline underline-offset-2"
              >
                Nạp tiền
              </button>
            </p>
          )}
        </div>
      </div>

      {/* ── Purchase Error ──────────────────────────────────────────── */}
      {purchaseError && (
        <div className="flex items-start gap-2 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3 mb-4">
          <AlertCircle className="size-4 shrink-0 mt-0.5" />
          {purchaseError}
        </div>
      )}

      {/* ── Purchase Button ─────────────────────────────────────────── */}
      <Button
        id="purchase-confirm-btn"
        className="w-full"
        size="lg"
        disabled={purchasing || insufficientBalance || !course}
        onClick={handlePurchase}
      >
        {purchasing ? (
          <>
            <Loader2 className="size-4 mr-2 animate-spin" />
            Đang xử lý...
          </>
        ) : (
          <>
            <ShoppingCart className="size-4 mr-2" />
            Xác nhận thanh toán {!quoteLoading && formatMoney(finalPrice)}
          </>
        )}
      </Button>

      <p className="text-xs text-muted-foreground text-center mt-3">
        Bằng cách nhấn xác nhận, bạn đồng ý với điều khoản dịch vụ của chúng tôi.
      </p>
    </div>
  );
}
