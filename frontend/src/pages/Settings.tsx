import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import type { PageProps } from '../accounts';
import type { RiskLimits, RiskSettings } from '../types';
import { usePolling } from '../usePolling';

const decimal = /^\d+(\.\d{1,2})?$/;

const parseSymbols = (text: string) =>
  text.split(/[\s,]+/).map((x) => x.trim().toUpperCase()).filter(Boolean);

export function Settings({ account }: PageProps) {
  const { data, error } = usePolling(api.getSettings);
  const [form, setForm] = useState<RiskSettings | null>(null);
  const [symbols, setSymbols] = useState('');
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (data) {
      setForm(data);
      const limits = data.accounts.find((a) => a.accountId === account.accountId)?.limits;
      setSymbols(limits?.allowedSymbols.join(', ') ?? '');
    }
  }, [data, account.accountId]);

  if (error) return <div className="banner error">Backend error: {error}</div>;
  if (!form) return <p className="muted">Loading…</p>;

  // Only the active account's limits are shown; other accounts' limits are kept untouched for when you switch back.
  const limits = form.accounts.find((a) => a.accountId === account.accountId)?.limits;
  if (!limits) {
    return <div className="banner error">No risk limits configured for {account.displayName}. Nothing will be traded.</div>;
  }

  const set = <K extends keyof RiskLimits>(key: K, value: RiskLimits[K]) =>
    setForm({
      ...form,
      accounts: form.accounts.map((a) =>
        a.accountId === account.accountId ? { ...a, limits: { ...a.limits, [key]: value } } : a),
    });

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!form || !limits) return;
    for (const key of ['maxPositionValue', 'maxOrderValue', 'maxDailyLoss'] as const) {
      if (!decimal.test(limits[key])) {
        setMessage({ ok: false, text: `${key} must be a positive number` });
        return;
      }
    }
    const toSave: RiskSettings = {
      ...form,
      accounts: form.accounts.map((a) =>
        a.accountId === account.accountId
          ? { ...a, limits: { ...a.limits, allowedSymbols: parseSymbols(symbols) } }
          : a),
    };
    setSaving(true);
    try {
      await api.saveSettings(toSave);
      setMessage({ ok: true, text: 'Saved' });
    } catch (err) {
      setMessage({ ok: false, text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }

  const cur = account.baseCurrency;

  return (
    <>
      <h2>Risk settings — {account.displayName} ({account.broker})</h2>
      <p className="muted">
        Enforced by the backend before every order. Each broker account keeps its own limits, so they are
        still there when you switch back. Paper/live mode is set in backend config only, not here.
      </p>
      <form className="settings" onSubmit={save}>
        <label>
          Max position value ({cur})
          <input inputMode="decimal" value={limits.maxPositionValue} onChange={(e) => set('maxPositionValue', e.target.value)} />
        </label>
        <label>
          Max single order value ({cur})
          <input inputMode="decimal" value={limits.maxOrderValue} onChange={(e) => set('maxOrderValue', e.target.value)} />
        </label>
        <label>
          Max daily loss ({cur})
          <input inputMode="decimal" value={limits.maxDailyLoss} onChange={(e) => set('maxDailyLoss', e.target.value)} />
        </label>
        <label>
          Max trades per day
          <input type="number" min={0} value={limits.maxTradesPerDay} onChange={(e) => set('maxTradesPerDay', Number(e.target.value))} />
        </label>
        <label>
          AI analysis interval (minutes)
          <input type="number" min={1} value={form.analysisIntervalMinutes}
            onChange={(e) => setForm({ ...form, analysisIntervalMinutes: Number(e.target.value) })} />
        </label>
        <label className="wide">
          Allowed symbols (comma separated)
          <input value={symbols} onChange={(e) => setSymbols(e.target.value)} />
        </label>
        <div className="row wide">
          <button className="primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          {message && <span className={message.ok ? 'pos' : 'neg'}>{message.text}</span>}
        </div>
      </form>
    </>
  );
}
