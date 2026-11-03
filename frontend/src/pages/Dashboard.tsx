import { useCallback, useState } from 'react';
import { api } from '../api';
import type { PageProps } from '../accounts';
import { money, num, pnlClass, time } from '../format';
import { usePolling } from '../usePolling';

export function Dashboard({ account }: PageProps) {
  const status = usePolling(api.getStatus, 10_000);
  const loadSummary = useCallback(() => api.getAccount(account.accountId), [account.accountId]);
  const summary = usePolling(loadSummary, 10_000);
  const loadPositions = useCallback(() => api.getPositions(account.accountId), [account.accountId]);
  const positions = usePolling(loadPositions, 10_000);

  const s = status.data;
  const a = summary.data;
  const error = status.error ?? summary.error ?? positions.error;

  return (
    <>
      {error && <div className="banner error">Backend error: {error}</div>}

      {s && <KillSwitch enabled={s.tradingEnabled} onChanged={status.reload} />}

      <section className="cards">
        <Card label="Equity" value={money(a?.equity, a?.currency)} />
        <Card label="Cash" value={money(a?.cash, a?.currency)} />
        <Card label="Today's P&L" value={money(a?.dailyPnl, a?.currency)} className={pnlClass(a?.dailyPnl)} />
        <Card label="Unrealized P&L" value={money(a?.unrealizedPnl, a?.currency)} className={pnlClass(a?.unrealizedPnl)} />
      </section>

      {s && (
        <p className="muted">
          {account.displayName} ({account.broker}): {s.brokerConnected ? 'connected' : 'disconnected'} ·
          Market: {s.marketOpen ? 'open' : 'closed'} · Last AI run: {time(s.lastRunAt)}
        </p>
      )}

      <h2>Positions</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Symbol</th><th>Side</th><th className="r">Qty</th><th className="r">Avg price</th>
              <th className="r">Price</th><th className="r">Value</th><th className="r">Unrealized P&L</th>
            </tr>
          </thead>
          <tbody>
            {positions.data?.map((p) => (
              <tr key={`${p.symbol}-${p.exchange}-${p.side}-${p.brokerPositionId ?? ''}`}>
                <td>
                  <b>{p.symbol}</b>
                  <div className="muted small">
                    {p.assetClass}{p.exchange ? ` · ${p.exchange}` : ''}{p.leverage ? ` · x${p.leverage}` : ''}
                  </div>
                </td>
                <td><span className={`tag ${p.side === 'LONG' ? 'buy' : 'sell'}`}>{p.side}</span></td>
                <td className="r">{num(p.quantity)}</td>
                <td className="r">{money(p.avgPrice, p.currency)}</td>
                <td className="r">{money(p.marketPrice, p.currency)}</td>
                <td className="r">{money(p.marketValue, p.currency)}</td>
                <td className={`r ${pnlClass(p.unrealizedPnl)}`}>{money(p.unrealizedPnl, p.currency)}</td>
              </tr>
            ))}
            {positions.data?.length === 0 && (
              <tr><td colSpan={7} className="muted">No open positions</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function Card({ label, value, className = '' }: { label: string; value: string; className?: string }) {
  return (
    <div className="card">
      <div className="card-label">{label}</div>
      <div className={`card-value ${className}`}>{value}</div>
    </div>
  );
}

function KillSwitch({ enabled, onChanged }: { enabled: boolean; onChanged: () => void }) {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    try {
      await action();
      onChanged();
    } finally {
      setBusy(false);
      setConfirming(false);
    }
  }

  return (
    <section className={`killswitch ${enabled ? 'on' : 'off'}`}>
      <div>
        <b>Automated trading is {enabled ? 'ON' : 'STOPPED'}</b>
        <div className="muted">
          {enabled ? 'The AI may place orders within your risk limits.' : 'No new orders will be placed.'}
        </div>
      </div>
      {enabled ? (
        confirming ? (
          <div className="row">
            <button className="danger" disabled={busy} onClick={() => run(api.stopTrading)}>Yes, stop now</button>
            <button disabled={busy} onClick={() => setConfirming(false)}>Cancel</button>
          </div>
        ) : (
          <button className="danger" onClick={() => setConfirming(true)}>Stop trading</button>
        )
      ) : (
        <button className="primary" disabled={busy} onClick={() => run(api.startTrading)}>Resume trading</button>
      )}
    </section>
  );
}
