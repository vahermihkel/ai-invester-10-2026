# Safety rules — always follow

This app moves real money. When writing or changing code:

- **Paper/demo trading by default, for every broker.**
  - IBKR: paper account (TWS port 7497 / Gateway 4002). Live = 7496 / 4001.
  - eToro: demo/virtual portfolio environment and demo keys. Real = real-environment keys/endpoints.
  - Any future broker: its sandbox/paper/demo environment.
  - Live trading only when explicitly enabled per account by config, never as a default. Making a LIVE account active (from `.env` or the UI) is refused unless `ALLOW_LIVE_TRADING=true` in `.env`. The backend must refuse to start an account in LIVE if the environment and the credentials/port don't match (e.g. LIVE flag with paper port, or demo keys flagged as real).
- **Never trust AI output blindly.** Validate and parse it strictly (JSON schema). Malformed or ambiguous output = HOLD, no order.
- **Risk limits enforced in backend code, not in the prompt:** max position size, max order value, max daily loss, max trades per day, allowed symbols list. These must be configurable per account (each broker account has its own limits, in its base currency); the active account's limits apply.
- **Kill switch:** a setting/endpoint that immediately stops all automated trading. It stays in effect across account switches (switching never re-enables trading).
- **No duplicate orders:** check open orders and current positions on the target account before placing; make execution idempotent (our own client order ID per signal + account, stored before sending; on retry/reconnect check the broker for that order before resending).
- **Log every decision** — signal, target account, risk check result, order, fill and any error — with timestamps (UTC).
- **Handle disconnects:** any broker connection/API can drop or rate-limit; reconnect safely with backoff and resync state from the broker, don't assume. Treat an unknown order outcome (timeout) as "maybe placed" and reconcile before doing anything else.
- **Market hours:** don't send orders when the market is closed unless explicitly intended. Market hours come from the adapter/instrument, not a hardcoded exchange.
- Any change to order execution, risk logic or a broker adapter needs tests.

## Secrets & access

All credentials come from the single `.env` file, and the app has no login but is reachable from the local machine only — see [configuration.md](configuration.md).
