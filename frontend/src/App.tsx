import { useState } from 'react';
import { api, isMock } from './api';
import { usePolling } from './usePolling';
import { ModeBadge, type PageProps } from './accounts';
import type { BrokerAccount } from './types';
import { Dashboard } from './pages/Dashboard';
import { Signals } from './pages/Signals';
import { Orders } from './pages/Orders';
import { Settings } from './pages/Settings';

const tabs = {
  dashboard: { label: 'Dashboard', Component: Dashboard },
  signals: { label: 'AI signals', Component: Signals },
  orders: { label: 'Orders', Component: Orders },
  settings: { label: 'Settings', Component: Settings },
} satisfies Record<string, { label: string; Component: (props: PageProps) => React.ReactNode }>;
type Tab = keyof typeof tabs;

export function App() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const accounts = usePolling(api.getAccounts);
  const status = usePolling(api.getStatus, 10_000);
  const { Component } = tabs[tab];
  const active = accounts.data?.find((a) => a.accountId === status.data?.activeAccountId);
  const error = accounts.error ?? status.error;

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
          {accounts.data && status.data && (
            <BrokerSwitcher accounts={accounts.data} active={active} onSwitched={status.reload} />
          )}
          {isMock && <span className="badge mock">MOCK DATA</span>}
          {active && <ModeBadge mode={active.tradingMode} />}
        </div>
      </header>
      <main>
        {error && <div className="banner error">Backend error: {error}</div>}
        {status.data && accounts.data && !active && (
          <div className="banner error">No active broker account. Choose one in the header.</div>
        )}
        {/* key: remount pages on switch so nothing from the previous broker stays on screen */}
        {active && <Component key={active.accountId} account={active} />}
      </main>
    </div>
  );
}

/** Dropdown to switch the active broker account. Switching needs a confirmation step. */
function BrokerSwitcher({ accounts, active, onSwitched }: {
  accounts: BrokerAccount[];
  active: BrokerAccount | undefined;
  onSwitched: () => void;
}) {
  const [pending, setPending] = useState<BrokerAccount | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function confirm() {
    if (!pending) return;
    setBusy(true);
    try {
      await api.setActiveAccount(pending.accountId);
      setPending(null);
      setError(null);
      onSwitched();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="switcher">
      <select
        aria-label="Broker account"
        value={pending?.accountId ?? active?.accountId ?? ''}
        onChange={(e) => {
          setError(null);
          const next = accounts.find((a) => a.accountId === e.target.value);
          setPending(next && next.accountId !== active?.accountId ? next : null);
        }}
      >
        {!active && <option value="">Choose broker…</option>}
        {accounts.map((a) => (
          <option key={a.accountId} value={a.accountId} disabled={!a.enabled}>
            {a.displayName} ({a.broker}{a.tradingMode === 'LIVE' ? ', LIVE' : ''})
          </option>
        ))}
      </select>
      {pending && (
        <div className={`switch-confirm ${pending.tradingMode === 'LIVE' ? 'live' : ''}`}>
          <div>
            Switch automated trading{active ? ` from ${active.displayName}` : ''} to <b>{pending.displayName}</b>?
            {pending.tradingMode === 'LIVE' && <div><b>This is a LIVE account with real money.</b></div>}
            <div className="muted small">
              Open positions and orders on the current broker stay as they are.
            </div>
          </div>
          <div className="row">
            <button className={pending.tradingMode === 'LIVE' ? 'danger' : 'primary'} disabled={busy} onClick={confirm}>
              Switch
            </button>
            <button disabled={busy} onClick={() => { setPending(null); setError(null); }}>Cancel</button>
          </div>
          {error && <div className="neg small">{error}</div>}
        </div>
      )}
    </div>
  );
}
