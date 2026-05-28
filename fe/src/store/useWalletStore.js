import { create } from 'zustand';

// Balance mock (localStorage để persist qua refresh)
const BALANCE_KEY = 'mock_wallet_balance';
const getStoredBalance = () => {
  const v = localStorage.getItem(BALANCE_KEY);
  return v ? parseFloat(v) : 1000000; // mặc định 1,000,000đ
};
const saveBalance = (b) => localStorage.setItem(BALANCE_KEY, String(b));

const useWalletStore = create((set, get) => ({
  balance: getStoredBalance(),
  transactions: [], // lịch sử giao dịch mock

  /**
   * Mock nạp tiền — cộng thẳng vào balance local
   * (khi API topUp thật sẵn sàng thì thay bằng topUpApi)
   */
  topUp: (amount) => {
    const newBalance = get().balance + amount;
    saveBalance(newBalance);
    set((state) => ({
      balance: newBalance,
      transactions: [
        {
          id: Date.now(),
          type: 'TOP_UP',
          amount,
          description: 'Nạp tiền vào ví',
          createdAt: new Date().toISOString(),
        },
        ...state.transactions,
      ],
    }));
    return newBalance;
  },

  /**
   * Trừ tiền sau khi mua thành công
   */
  deduct: (amount) => {
    const newBalance = Math.max(0, get().balance - amount);
    saveBalance(newBalance);
    set((state) => ({
      balance: newBalance,
      transactions: [
        {
          id: Date.now(),
          type: 'PURCHASE',
          amount: -amount,
          description: 'Thanh toán khóa học',
          createdAt: new Date().toISOString(),
        },
        ...state.transactions,
      ],
    }));
  },

  refreshBalance: () => {
    set({ balance: getStoredBalance() });
  },
}));

export default useWalletStore;
