import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useChatbotStore = create(
  persist(
    (set, get) => ({
      isOpen: false,
      sessionId: null,
      messages: [],
      isLoading: false,
      sessionHistory: [], // [{ id, title, updatedAt }]

      setIsOpen: (isOpen) => set({ isOpen }),

      setSessionId: (id) => set((state) => {
        const exists = state.sessionHistory.find(s => s.id === id);
        if (!exists && id) {
          return {
            sessionId: id,
            sessionHistory: [{ id, title: `Phiên tư vấn #${id}`, updatedAt: new Date().toISOString() }, ...state.sessionHistory]
          };
        }
        return { sessionId: id };
      }),

      setMessages: (messages) => set({ messages }),

      addMessage: (message) => set((state) => {
        // Cập nhật thời gian của session hiện tại
        const updatedHistory = state.sessionHistory.map(s =>
          s.id === state.sessionId
            ? { ...s, updatedAt: new Date().toISOString() }
            : s
        );
        return {
          messages: [...state.messages, message],
          sessionHistory: updatedHistory
        };
      }),

      setIsLoading: (isLoading) => set({ isLoading }),

      deleteSession: (id) => set((state) => {
        const newHistory = state.sessionHistory.filter(s => s.id !== id);
        if (state.sessionId === id) {
          return { sessionHistory: newHistory, sessionId: null, messages: [] };
        }
        return { sessionHistory: newHistory };
      }),

      resetSession: () => set({ sessionId: null, messages: [], isLoading: false }),
    }),
    {
      name: 'chatbot-storage',
      partialize: (state) => ({
        sessionId: state.sessionId,
        sessionHistory: state.sessionHistory
      }), // Save both sessionId and history
    }
  )
);
