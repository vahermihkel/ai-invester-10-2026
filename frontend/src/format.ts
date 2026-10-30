// Display-only formatting. Never use these results for calculations.

export function money(value: string | null | undefined, currency = 'USD'): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(Number(value));
}

export function num(value: string | null | undefined): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 }).format(Number(value));
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
