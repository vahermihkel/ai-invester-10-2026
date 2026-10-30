import { useState } from 'react';
import { api } from '../api';
import { money, num, pnlClass, time } from '../format';
import { usePolling } from '../usePolling';

export function Dashboard() {
  const status = usePolling(api.getStatus, 10_000);
  const account = usePolling(api.getAccount, 10_000);
  const positions = usePolling(api.getPositions, 10_000);

  const s = status.data;
  const a = account.data;
  const error = status.error ?? account.error ?? positions.error;

  return (
    <>
      {error && <div className="banner error">Backend error: {error}</div>}

      {s && <KillSwitch enabled={s.tradingEnabled} onChanged={status.reload} />}

      <section className="cards">
        <Card label="Net liquidation" value={money(a?.netLiquidation, a?.currency)} />
        <Card label="Cash" value={money(a?.cash, a?.currency)} />
        <Card label="Today's P&L" value={money(a?.dailyPnl, a?.currency)} className={pnlClass(a?.dailyPnl)} />
        <Card label="Unrealized P&L" value={money(a?.unrealizedPnl, a?.currency)} className={pnlClass(a?.unrealizedPnl)} />
      </section>

      {s && (
        <p className="muted">
          IBKR: {s.brokerConnected ? 'connected' : 'disconnected'} · Market: {s.marketOpen ? 'open' : 'closed'} ·
          Last AI run: {time(s.lastRunAt)}
        </p>
      )}

      <h2>Positions</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Symbol</th><th className="r">Qty</th><th className="r">Avg cost</th>
              <th className="r">Price</th><th className="r">Value</th><th className="r">Unrealized P&L</th>
            </tr>
          </thead>
          <tbody>
            {positions.data?.map((p) => (
              <tr key={p.symbol}>
                <td><b>{p.symbol}</b></td>
                <td className="r">{num(p.quantity)}</td>
                <td className="r">{money(p.avgCost)}</td>
                <td className="r">{money(p.marketPrice)}</td>
                <td className="r">{money(p.marketValue)}</td>
                <td className={`r ${pnlClass(p.unrealizedPnl)}`}>{money(p.unrealizedPnl)}</td>
              </tr>
            ))}
            {positions.data?.length === 0 && (
              <tr><td colSpan={6} className="muted">No open positions</td></tr>
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
