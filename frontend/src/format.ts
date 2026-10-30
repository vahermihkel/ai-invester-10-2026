// Display-only formatting. Never use these results for calculations.
import type { OrderSize } from './types';

export function money(value: string | null | undefined, currency = 'USD'): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(Number(value));
}

export function num(value: string | null | undefined): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 }).format(Number(value));
}

/** Order/signal size: units (e.g. IBKR shares) or a cash amount (e.g. eToro). */
export function size(value: OrderSize | null | undefined, currency = 'USD'): string {
  if (value == null) return '—';
  return value.type === 'AMOUNT' ? money(value.value, currency) : `${num(value.value)} units`;
}

export function pnlClass(value: string | null | undefined): string {
  if (value == null) return '';
  const n = Number(value);
  return n > 0 ? 'pos' : n < 0 ? 'neg' : '';
}

export function time(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}
