// Display-only formatting. Never use these results for calculations.
import type { OrderSize } from './types';

// Values below 1 keep their decimals: a crypto price like 0.000012 must not show as 0.00.
const smallValueDigits = 12;

export function money(value: string | null | undefined, currency = 'USD'): string {
  if (value == null) return '—';
  const n = Number(value);
  const maximumFractionDigits = Math.abs(n) < 1 ? smallValueDigits : 2;
  // Intl only accepts 3-letter codes; crypto codes like USDT are written after the number.
  if (!/^[A-Za-z]{3}$/.test(currency)) {
    return `${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits }).format(n)} ${currency}`;
  }
  return new Intl.NumberFormat('en-US', { style: 'currency', currency, maximumFractionDigits }).format(n);
}

export function num(value: string | null | undefined): string {
  if (value == null) return '—';
  const n = Number(value);
  const maximumFractionDigits = Math.abs(n) < 1 ? smallValueDigits : 4;
  return new Intl.NumberFormat('en-US', { maximumFractionDigits }).format(n);
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
