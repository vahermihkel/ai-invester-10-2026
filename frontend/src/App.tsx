import { useState } from 'react';
import { api, isMock } from './api';
import { usePolling } from './usePolling';
import { Dashboard } from './pages/Dashboard';
import { Signals } from './pages/Signals';
import { Orders } from './pages/Orders';
import { Settings } from './pages/Settings';

const tabs = {
  dashboard: { label: 'Dashboard', Component: Dashboard },
  signals: { label: 'AI signals', Component: Signals },
  orders: { label: 'Orders', Component: Orders },
  settings: { label: 'Settings', Component: Settings },
};
type Tab = keyof typeof tabs;

export function App() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const { data: status } = usePolling(api.getStatus, 10_000);
  const { Component } = tabs[tab];

  return (
    <div className="app">
      <header>
        <h1>AI Investor</h1>
        <nav>
          {(Object.keys(tabs) as Tab[]).map((t) => (
            <button key={t} className={t === tab ? 'active' : ''} onClick={() => setTab(t)}>
              {tabs[t].label}
            </button>
          ))}
        </nav>
        <div className="badges">
          {isMock && <span className="badge mock">MOCK DATA</span>}
          {status && (
            <span className={`badge ${status.tradingMode === 'LIVE' ? 'live' : 'paper'}`}>
              {status.tradingMode === 'LIVE' ? 'LIVE ACCOUNT' : 'PAPER'}
            </span>
          )}
        </div>
      </header>
      <main>
        <Component />
      </main>
    </div>
  );
}
