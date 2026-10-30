import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import type { RiskSettings } from '../types';
import { usePolling } from '../usePolling';

const decimal = /^\d+(\.\d{1,2})?$/;

export function Settings() {
  const { data, error } = usePolling(api.getSettings);
  const [form, setForm] = useState<RiskSettings | null>(null);
  const [symbols, setSymbols] = useState('');
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (data) {
      setForm(data);
      setSymbols(data.allowedSymbols.join(', '));
    }
  }, [data]);

  if (error) return <div className="banner error">Backend error: {error}</div>;
  if (!form) return <p className="muted">Loading…</p>;

  const set = <K extends keyof RiskSettings>(key: K, value: RiskSettings[K]) =>
    setForm({ ...form, [key]: value });

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    for (const key of ['maxPositionValue', 'maxOrderValue', 'maxDailyLoss'] as const) {
      if (!decimal.test(form[key])) {
        setMessage({ ok: false, text: `${key} must be a positive number` });
        return;
      }
    }
    const allowedSymbols = symbols.split(/[\s,]+/).map((x) => x.trim().toUpperCase()).filter(Boolean);
    setSaving(true);
    try {
      await api.saveSettings({ ...form, allowedSymbols });
      setMessage({ ok: true, text: 'Saved' });
    } catch (err) {
      setMessage({ ok: false, text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <h2>Risk settings</h2>
      <p className="muted">
        Enforced by the backend before every order. Paper/live mode is set in backend config only, not here.
      </p>
      <form className="settings" onSubmit={save}>
        <label>
          Max position value (USD)
          <input inputMode="decimal" value={form.maxPositionValue} onChange={(e) => set('maxPositionValue', e.target.value)} />
        </label>
        <label>
          Max single order value (USD)
          <input inputMode="decimal" value={form.maxOrderValue} onChange={(e) => set('maxOrderValue', e.target.value)} />
        </label>
        <label>
          Max daily loss (USD)
          <input inputMode="decimal" value={form.maxDailyLoss} onChange={(e) => set('maxDailyLoss', e.target.value)} />
        </label>
        <label>
          Max trades per day
          <input type="number" min={0} value={form.maxTradesPerDay} onChange={(e) => set('maxTradesPerDay', Number(e.target.value))} />
        </label>
        <label>
          AI analysis interval (minutes)
          <input type="number" min={1} value={form.analysisIntervalMinutes} onChange={(e) => set('analysisIntervalMinutes', Number(e.target.value))} />
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
