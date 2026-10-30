import { useCallback, useEffect, useState } from 'react';

/** Loads data with `fn` and reloads it every `intervalMs` (0 = load once). */
export function usePolling<T>(fn: () => Promise<T>, intervalMs = 0) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setData(await fn());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [fn]);

  useEffect(() => {
    reload();
    if (!intervalMs) return;
    const id = setInterval(reload, intervalMs);
    return () => clearInterval(id);
  }, [reload, intervalMs]);

  return { data, error, reload };
}
