// Shared account helpers. Everything here works off BrokerAccount data from the backend,
// so a new broker shows up without frontend changes.
import type { BrokerAccount, TradingMode } from './types';

/** Props every page gets: the single active broker account. */
export interface PageProps {
  account: BrokerAccount;
}

export function ModeBadge({ mode }: { mode: TradingMode }) {
  return <span className={`badge ${mode === 'LIVE' ? 'live' : 'paper'}`}>{mode === 'LIVE' ? 'LIVE ACCOUNT' : 'PAPER'}</span>;
}
