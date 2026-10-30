import type { AccountSummary, Order, Position, RiskSettings, Signal, SystemStatus } from './types';
import * as mock from './mockData';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${res.status} ${res.statusText}${text ? `: ${text}` : ''}`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const api = USE_MOCK
  ? mock.api
  : {
      getStatus: () => request<SystemStatus>('/status'),
      getAccount: () => request<AccountSummary>('/account'),
      getPositions: () => request<Position[]>('/positions'),
      getSignals: (limit = 50) => request<Signal[]>(`/signals?limit=${limit}`),
      getOrders: (limit = 50) => request<Order[]>(`/orders?limit=${limit}`),
      getSettings: () => request<RiskSettings>('/settings'),
      saveSettings: (s: RiskSettings) =>
        request<RiskSettings>('/settings', { method: 'PUT', body: JSON.stringify(s) }),
      // Kill switch
      stopTrading: () => request<void>('/trading/stop', { method: 'POST' }),
      startTrading: () => request<void>('/trading/start', { method: 'POST' }),
    };

export const isMock = USE_MOCK;
