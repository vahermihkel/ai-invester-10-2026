import { useCallback } from 'react';
import { api } from '../api';
import type { PageProps } from '../accounts';
import { money, num, size, time } from '../format';
import { usePolling } from '../usePolling';

export function Orders({ account }: PageProps) {
  const loadOrders = useCallback(() => api.getOrders(account.accountId, 100), [account.accountId]);
  const { data, error } = usePolling(loadOrders, 15_000);

  return (
    <>
      <h2>Orders</h2>
      {error && <div className="banner error">Backend error: {error}</div>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Time</th><th>Symbol</th><th>Side</th><th className="r">Size</th><th>Type</th>
              <th className="r">Limit</th><th>Status</th><th className="r">Filled qty</th>
              <th className="r">Avg fill</th><th className="r">Signal</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((o) => (
              <tr key={o.id}>
                <td className="nowrap">{time(o.createdAt)}</td>
                <td>
                  <b>{o.symbol}</b>
                  {o.brokerOrderId && <div className="muted small">#{o.brokerOrderId}</div>}
                </td>
                <td><span className={`tag ${o.side.toLowerCase()}`}>{o.side}</span></td>
                <td className="r nowrap">{size(o.size, o.currency)}</td>
                <td>{o.orderType}</td>
                <td className="r">{money(o.limitPrice, o.currency)}</td>
                <td><span className={`tag ${o.status.toLowerCase()}`}>{o.status}</span></td>
                <td className="r">{num(o.filledQuantity)}</td>
                <td className="r">{money(o.avgFillPrice, o.currency)}</td>
                <td className="r muted">{o.signalId ? `#${o.signalId}` : o.source.toLowerCase()}</td>
              </tr>
            ))}
            {data?.length === 0 && <tr><td colSpan={10} className="muted">No orders yet</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
