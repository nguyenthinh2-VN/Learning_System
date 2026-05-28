import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:8080/ws';

function formatMoney(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

function getToastMessage(data) {
  const amount = formatMoney(data.addedAmount ?? 0);
  switch (data.source) {
    case 'MOCK': return `[Dev] Nap tien thanh cong: +${amount}`;
    case 'VIETQR': return `Nap tien thanh cong: +${amount}`;
    case 'ADMIN': return `Tai khoan duoc cong tien: +${amount}`;
    default: return `So du vi da duoc cap nhat: +${amount}`;
  }
}

/**
 * Hook ket noi WebSocket realtime cho vi tien.
 * Chi connect/disconnect khi `token` thay doi.
 * Callbacks duoc luu qua ref de khong gay re-connect khi re-render.
 */
export function useWalletWebSocket({ token, onBalanceUpdate, onToast }) {
  const clientRef = useRef(null);
  const onBalanceUpdateRef = useRef(onBalanceUpdate);
  const onToastRef = useRef(onToast);

  onBalanceUpdateRef.current = onBalanceUpdate;
  onToastRef.current = onToast;

  useEffect(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('[WalletWS] Connected');
        client.subscribe('/user/queue/wallet', (message) => {
          try {
            const data = JSON.parse(message.body);
            if (data.event === 'WALLET_UPDATED') {
              console.log('[WalletWS] WALLET_UPDATED:', data);
              onBalanceUpdateRef.current?.(data.newBalance, data);
              onToastRef.current?.(getToastMessage(data), 'success');
            }
          } catch (err) {
            console.error('[WalletWS] Parse error:', err);
          }
        });
      },
      onDisconnect: () => console.log('[WalletWS] Disconnected'),
      onStompError: (frame) => console.error('[WalletWS] STOMP error:', frame.headers?.message),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [token]);
}
