import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2, Wallet } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { completeVnPayReturnApi, getTopUpStatusApi } from '@/api/wallet';

/**
 * Trang VNPay redirect về sau khi user thanh toán.
 *
 * Luồng:
 *  1. Đọc toàn bộ vnp_* từ query.
 *  2. Gọi POST /wallet/top-up/vnpay-return → BE verify chữ ký + đối soát + (RETURN mode) cộng tiền.
 *  3. Nếu chưa COMPLETED (IPN mode: BE chỉ ghi nhận, chờ IPN cộng) → poll GET status vài lần.
 *  4. Hiển thị xanh (thành công) / đỏ (thất bại). Số dư ở tab ví tự cập nhật qua WebSocket.
 */

const STATE = { LOADING: 'loading', SUCCESS: 'success', FAILED: 'failed' };
const POLL_MAX = 5;
const POLL_INTERVAL_MS = 2000;

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount ?? 0);
}

export default function WalletVnpayReturnPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [state, setState] = useState(STATE.LOADING);
  const [message, setMessage] = useState('Đang xác nhận kết quả thanh toán…');
  const [amount, setAmount] = useState(null);
  const ranRef = useRef(false); // tránh chạy 2 lần do StrictMode

  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;

    // Gom toàn bộ vnp_* từ query string
    const params = {};
    searchParams.forEach((value, key) => {
      if (key.startsWith('vnp_')) params[key] = value;
    });

    const ref = params.vnp_TxnRef;
    if (!ref) {
      setState(STATE.FAILED);
      setMessage('Thiếu thông tin giao dịch trong đường dẫn trả về.');
      return;
    }

    const finishSuccess = (amt) => {
      setAmount(amt);
      setState(STATE.SUCCESS);
      setMessage('Nạp tiền thành công');
    };

    const pollStatus = async (attempt) => {
      try {
        const res = await getTopUpStatusApi(ref);
        const data = res.data.data;
        if (data.status === 'COMPLETED') {
          finishSuccess(data.amount);
          return;
        }
        if (data.status === 'EXPIRED' || data.status === 'FAILED') {
          setState(STATE.FAILED);
          setMessage('Giao dịch không thành công hoặc đã hết hạn.');
          return;
        }
        if (attempt < POLL_MAX) {
          setTimeout(() => pollStatus(attempt + 1), POLL_INTERVAL_MS);
        } else {
          // Vẫn PENDING sau khi poll — có thể IPN tới trễ.
          setState(STATE.FAILED);
          setMessage('Chưa nhận được xác nhận thanh toán. Vui lòng kiểm tra lại lịch sử giao dịch sau ít phút.');
        }
      } catch {
        setState(STATE.FAILED);
        setMessage('Không kiểm tra được trạng thái giao dịch.');
      }
    };

    (async () => {
      // vnp_ResponseCode != "00" → user huỷ/thất bại, không cần gọi BE complete.
      if (params.vnp_ResponseCode && params.vnp_ResponseCode !== '00') {
        setState(STATE.FAILED);
        setMessage('Bạn đã huỷ hoặc thanh toán không thành công.');
        return;
      }
      try {
        const res = await completeVnPayReturnApi(params);
        const data = res.data.data;
        if (data?.status === 'COMPLETED') {
          finishSuccess(data.amount);
        } else {
          // IPN mode: BE mới ghi nhận, chờ IPN cộng → poll.
          pollStatus(1);
        }
      } catch (err) {
        setState(STATE.FAILED);
        setMessage(err?.response?.data?.message || 'Xác nhận thanh toán thất bại.');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="max-w-lg mx-auto px-6 py-16">
      <div className="border rounded-2xl p-8 flex flex-col items-center text-center gap-4">
        {state === STATE.LOADING && (
          <>
            <Loader2 className="size-14 animate-spin text-muted-foreground" />
            <h1 className="text-xl font-semibold">Đang xử lý…</h1>
            <p className="text-sm text-muted-foreground">{message}</p>
          </>
        )}

        {state === STATE.SUCCESS && (
          <>
            <CheckCircle2 className="size-16 text-emerald-500" />
            <h1 className="text-xl font-semibold text-emerald-700">{message}</h1>
            {amount != null && (
              <p className="text-2xl font-bold tracking-tight">{formatMoney(amount)}</p>
            )}
            <p className="text-sm text-muted-foreground">
              Số dư ví của bạn đã được cập nhật.
            </p>
          </>
        )}

        {state === STATE.FAILED && (
          <>
            <XCircle className="size-16 text-red-500" />
            <h1 className="text-xl font-semibold text-red-700">Nạp tiền không thành công</h1>
            <p className="text-sm text-muted-foreground">{message}</p>
          </>
        )}

        <Button className="mt-2" onClick={() => navigate('/wallet')}>
          <Wallet className="size-4 mr-2" />
          Quay lại ví
        </Button>
      </div>
    </div>
  );
}
