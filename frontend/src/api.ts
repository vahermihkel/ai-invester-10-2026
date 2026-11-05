import type {
  AccountSummary, BrokerAccount, Order, Position, RiskSettings, Signal, SystemStatus,
} from './types';
import * as mock from './mockData';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  if (!res.ok) throw new Error(await errorMessage(res));
  return res.status === 204 ? (undefined as T) : res.json();
}

/** The backend explains errors in a JSON "detail" field; anything else is shown as it came. */
async function errorMessage(res: Response): Promise<string> {
  const text = await res.text().catch(() => '');
  try {
    const detail = JSON.parse(text).detail;
    if (detail) return detail;
  } catch {
    // Not JSON, e.g. a proxy error page when the backend isn't running.
  }
  return `${res.status} ${res.statusText}${text ? `: ${text}` : ''}`;
}

const q = (accountId: string) => encodeURIComponent(accountId);

// Only one account is active at a time; pages pass the active accountId explicitly.
export const api = USE_MOCK
  ? mock.api
  : {
      getAccounts: () => request<BrokerAccount[]>('/accounts'),
      getStatus: () => request<SystemStatus>('/status'),
      // Switch which broker account is traded and shown.
      setActiveAccount: (accountId: string) =>
        request<SystemStatus>('/accounts/active', { method: 'PUT', body: JSON.stringify({ accountId }) }),
      getAccount: (accountId: string) => request<AccountSummary>(`/account?accountId=${q(accountId)}`),
      getPositions: (accountId: string) => request<Position[]>(`/positions?accountId=${q(accountId)}`),
      getSignals: (accountId: string, limit = 50) =>
        request<Signal[]>(`/signals?accountId=${q(accountId)}&limit=${limit}`),
      getOrders: (accountId: string, limit = 50) =>
        request<Order[]>(`/orders?accountId=${q(accountId)}&limit=${limit}`),
      getSettings: () => request<RiskSettings>('/settings'),
      saveSettings: (s: RiskSettings) =>
        request<RiskSettings>('/settings', { method: 'PUT', body: JSON.stringify(s) }),
      // Kill switch
      stopTrading: () => request<void>('/trading/stop', { method: 'POST' }),
      startTrading: () => request<void>('/trading/start', { method: 'POST' }),
    };

export const isMock = USE_MOCK;
