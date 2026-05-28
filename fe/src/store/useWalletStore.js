import { create } from 'zustand';
import { initTopUpApi, mockWebhookApi } from '@/api/wallet';

/**
 * Wallet Store — chỉ dùng cho trạng thái UI của luồng nạp tiền.
 * Số dư (balance) được quản lý trong AuthContext và cập nhật qua WebSocket.
 */
const useWalletStore = create((set) => ({
  // Trạng thái nạp tiền
  topUpLoading: false,
  topUpError: null,
  topUpResult: null, // { referenceCode, amount, displayType, displayData, expiredAt }

  // Trạng thái mock webhook (dev only)
  mockLoading: false,
  mockError: null,

  /**
   * Khởi tạo nạp tiền — gọi POST /api/v1/wallet/top-up/init
   * Sau khi thành công, FE đọc displayType/displayData để render QR hoặc hướng dẫn.
   * Số dư sẽ tự cập nhật qua WebSocket (WALLET_UPDATED) sau khi thanh toán xong.
   * @param {number} amount - Tối thiểu 10,000đ
   */
  initTopUp: async (amount) => {
    set({ topUpLoading: true, topUpError: null, topUpResult: null });
    try {
      const res = await initTopUpApi(amount);
      const result = res.data.data;
      set({ topUpResult: result, topUpLoading: false });
      return result;
    } catch (err) {
      const msg = err?.response?.data?.message || 'Khởi tạo nạp tiền thất bại.';
      set({ topUpError: msg, topUpLoading: false });
      throw err;
    }
  },

  /**
   * Giả lập thanh toán thành công (chỉ dùng khi payment.provider=mock)
   * Gọi POST /api/v1/webhook/mock?ref={referenceCode}
   * Sau khi gọi, BE sẽ push WebSocket WALLET_UPDATED → AuthContext tự cập nhật balance.
   * @param {string} referenceCode
   */
  triggerMockWebhook: async (referenceCode) => {
    set({ mockLoading: true, mockError: null });
    try {
      const res = await mockWebhookApi(referenceCode);
      set({ mockLoading: false });
      return res.data;
    } catch (err) {
      const msg = err?.response?.data?.message || 'Mock webhook thất bại.';
      set({ mockError: msg, mockLoading: false });
      throw err;
    }
  },

  /** Reset trạng thái sau khi đóng form nạp tiền */
  resetTopUp: () => set({ topUpLoading: false, topUpError: null, topUpResult: null }),
}));

export default useWalletStore;

