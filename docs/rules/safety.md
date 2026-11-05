# Safety rules — always follow

This app moves real money. When writing or changing code:

- **Paper/demo trading by default, for every broker.**
  - IBKR: paper account (TWS port 7497 / Gateway 4002). Live = 7496 / 4001.
  - eToro: demo/virtual portfolio environment and demo keys. Real = real-environment keys/endpoints.
  - Any future broker: its sandbox/paper/demo environment.
  - Live trading only when explicitly enabled per account by config, never as a default. Making a LIVE account active (from `.env` or the UI) is refused unless `ALLOW_LIVE_TRADING=true` in `.env`. The backend must refuse to start an account in LIVE if the environment and the credentials/port don't match (e.g. LIVE flag with paper port, or demo keys flagged as real).
- **When in doubt, no order.** If limits, the kill switch, the active account, broker state or the AI response can't be read or validated, nothing is sent. A failure must never lead to trading.
- **Never trust AI output blindly.** Validate and parse it strictly (JSON schema). Malformed or ambiguous output = HOLD, no order.
  - Every provider gets the same checks, including a fallback model. A valid answer still goes through the risk engine.
  - The AI only returns a signal: it gets no tools and no way to call the broker, choose the account or change settings.
  - Outside text in a prompt (news, instrument descriptions, broker messages) is data and can contain instructions aimed at the AI. Risk limits are what contain a manipulated answer, so never loosen them because of something the AI says.
- **Risk limits enforced in backend code, not in the prompt:** max position size, max order value, max daily loss, max trades per day, allowed symbols list. These must be configurable per account (each broker account has its own limits, in its base currency); the active account's limits apply.
  - Missing or zero limits and unlisted symbols mean rejection. A new alias starts with no limits and inherits nothing, so it can't trade until they are set.
  - Switching accounts doesn't reset an account's daily loss or trade counters.
- **Kill switch:** a setting/endpoint that immediately stops all automated trading. It stays in effect across account switches (switching never re-enables trading).
  - Trading starts stopped on a new database and stays as set across restarts. Only an explicit start turns trading on, never a config change, a restart or re-enabling an account.
  - Execution checks it right before every order. It stops new orders; it doesn't cancel open orders or close positions at the broker.
- **No duplicate orders:** check open orders and current positions on the target account before placing; make execution idempotent (our own client order ID per signal + account, stored before sending; on retry/reconnect check the broker for that order before resending).
- **Log every decision** — signal, target account, risk check result, order, fill and any error — with timestamps (UTC).
- **Handle disconnects:** any broker connection/API can drop or rate-limit; reconnect safely with backoff and resync state from the broker, don't assume. Treat an unknown order outcome (timeout) as "maybe placed" and reconcile before doing anything else.
- **Changing or retiring accounts and providers:** follow the rules in [brokers.md](brokers.md) and [ai-providers.md](ai-providers.md). A disabled account can't be reconciled and nothing watches its positions, so clear its open orders and decide about its positions first.
- **Market hours:** don't send orders when the market is closed unless explicitly intended. Market hours come from the adapter/instrument, not a hardcoded exchange.
- Any change to order execution, risk logic, a broker adapter, the AI router, the signal parser or local-only access needs tests.

## Secrets & access

All credentials come from the single `.env` file, and the app has no login but is reachable from the local machine only — see [configuration.md](configuration.md).

- **What goes to AI providers:** prompts contain positions and cash and leave this machine for every provider in the chain, including free tiers that may keep or train on them. Never put credentials or real broker account numbers in a prompt, and enable only providers you accept that for.
- **Where API keys go:** a key is sent only to its own provider's `baseUrl`, over HTTPS unless the provider runs on this machine.
- **What gets stored:** strip credentials and real account numbers before saving prompts, AI responses, broker payloads (`raw`, `details`), error messages and audit entries.
- **Exposed credentials:** keys are added by editing `.env` directly, not pasted into chats or AI tools. If a key was exposed or committed, regenerate it at the provider or broker; deleting it from the file or git history isn't enough.
