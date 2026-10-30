import { useCallback } from 'react';
import { api } from '../api';
import type { PageProps } from '../accounts';
import { size, time } from '../format';
import { usePolling } from '../usePolling';

export function Signals({ account }: PageProps) {
  const loadSignals = useCallback(() => api.getSignals(account.accountId, 100), [account.accountId]);
  const { data, error } = usePolling(loadSignals, 15_000);

  return (
    <>
      <h2>AI signals</h2>
      {error && <div className="banner error">Backend error: {error}</div>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Time</th><th>Symbol</th><th>Action</th><th className="r">Size</th>
              <th className="r">Confidence</th><th>Status</th><th>Reasoning</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((s) => (
              <tr key={s.id}>
                <td className="nowrap">{time(s.createdAt)}</td>
                <td><b>{s.symbol}</b></td>
                <td><span className={`tag ${s.action.toLowerCase()}`}>{s.action}</span></td>
                <td className="r nowrap">{size(s.size, account.baseCurrency)}</td>
                <td className="r">{Math.round(s.confidence * 100)}%</td>
                <td>
                  <span className={`tag ${s.status.toLowerCase()}`}>{s.status}</span>
                  {s.rejectionReason && <div className="muted small">{s.rejectionReason}</div>}
                </td>
                <td className="reasoning">{s.reasoning}</td>
              </tr>
            ))}
            {data?.length === 0 && <tr><td colSpan={7} className="muted">No signals yet</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}
