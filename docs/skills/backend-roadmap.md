# Backend roadmap

What the backend must do, and in which order to build it. Tick items off (`[x]`) as they are done and keep this file current.

## Features

### AI
- [ ] **Prompt building** — send the AI market data plus context: current positions, cash, risk limits, recent signals. Same prompt template for every provider ([ai-providers.md](../rules/ai-providers.md)).
- [ ] **AI provider adapters + `AiRouter`** — priority order, automatic switch to the next model on quota/technical errors, quota reset time stored in DB, usage/budget tracking. Includes the startup sync of `app.ai.providers`: refuse a changed `type` on an existing alias, and clear availability on a model change ([ai-providers.md](../rules/ai-providers.md)).
- [ ] **Strict response validation** — parse into the signal schema; anything malformed or ambiguous = HOLD.
- [ ] **Persist AI communication** — every call in `ai_calls` (provider, model, tokens, cost, latency, outcome), raw response + parsed signal in `signals`.

### Broker
- [ ] **Broker adapters** (IBKR, eToro) behind `BrokerClient` ([brokers.md](../rules/brokers.md)), paper/demo first.
- [ ] **Account summary and positions** — fetch from the active account; store `account_snapshots` + `position_snapshots` for history (schedule + retention in [brokers.md](../rules/brokers.md)).
- [ ] **History reconciliation/backfill** — per adapter, fill gaps in orders/fills/daily equity from the broker's history source (IBKR Flex Web Service, eToro trade history).
- [ ] **Order lifecycle** — track each order SUBMITTED → PARTIALLY_FILLED → FILLED / CANCELLED / REJECTED; record fills, including late ones.
- [ ] **External activity** — store orders we didn't send (`source = EXTERNAL`: broker's own app, stop-outs, liquidations) and money movements in `cash_transactions` (deposits, dividends, fees).
- [ ] **Instrument mapping** — symbol → broker's own ID and trading rules, cached in `instrument_mappings`.
- [ ] **Reconnect + resync** — reconnect with backoff, reload state from the broker; unknown order outcome = "maybe placed", reconcile first.
- [x] **Active account switching** — `ActiveAccountService`, `PUT /api/accounts/active`, rules in [brokers.md](../rules/brokers.md).
- [ ] **Retiring accounts** — done: an account removed from the config or with empty `.env` values is disabled, keeps its history, and an alias can't change broker or trading mode. Not done: startup warning + audit entry when a disabled account still has open orders in our DB ([brokers.md](../rules/brokers.md)).

### Trading
- [ ] **Market data** — prices/history from the broker or a free source behind `MarketDataProvider`.
- [ ] **Risk engine** — max position value, max order value, max daily loss, max trades/day, allowed symbols, per account. Rejected signals stored with reason.
- [ ] **Daily P&L / loss tracking** — needed for the max-daily-loss limit.
- [ ] **Order execution** — signal → `OrderRequest`, units ↔ amount conversion and netting vs. separate positions via `BrokerCapabilities`, idempotent (own `clientOrderId`, check open orders/positions first).
- [ ] **Automated trading** — scheduler runs analysis → risk check → execution.
- [ ] **Manual trading** — endpoint to place/cancel orders by hand (needs adding to [api-contract.md](api-contract.md)). Manual orders also pass the risk engine.
- [ ] **Scheduler + market hours** — `@Scheduled` runs, market hours and holidays per instrument; no orders while the market is closed.
- [x] **Kill switch** — `POST /api/trading/stop|start`, persists across restarts and account switches. Execution must check it right before every order once execution is built.

### Platform
- [ ] **Database schema + Flyway migrations** — `V1__initial_schema.sql` and Flyway wiring done, with Hibernate validating entities at startup ([database.md](database.md)). JPA entities exist for `app_settings`, `broker_accounts`, `active_account_changes`, `risk_limits`, `signals`, `orders` and `audit_log`; the rest are added with the features that use them.
- [ ] **Config + secrets** — everything from the root `.env` (template: `.env.example`), bound via `@ConfigurationProperties`; accounts enabled by which `.env` values are filled in ([configuration.md](../rules/configuration.md)). Done for the database and broker accounts. Not done: AI provider keys.
- [ ] **Startup safety checks** — refuse to start if LIVE/PAPER doesn't match the credentials or port. Done so far: no LIVE account active without `ALLOW_LIVE_TRADING=true`, and an alias can't change broker or trading mode. Checking credentials and ports needs the adapters.
- [ ] **REST API for the frontend** — exactly the shapes in [api-contract.md](api-contract.md) / `frontend/src/types.ts`. Done: status (broker fields fixed to false until adapters exist), accounts, active account, kill switch, settings, signals, orders. Still 501 placeholders: account summary and positions (need adapters), account history, cash transactions, trades, order events and fills, AI providers.
- [ ] **Settings storage** — risk limits and AI provider order/enabled in DB, changeable at runtime. Risk limits done; new accounts start with zero limits and no allowed symbols, so nothing can trade until they are set.
- [x] **Local-only access (no login)** — single-user app, no login/logout. Bind the backend to `127.0.0.1`, CORS only for the local frontend ([configuration.md](../rules/configuration.md)).
- [ ] **Alerts** — notify (e.g. email or Telegram) on broker disconnect, all AI providers down, daily loss limit hit, order rejected.
- [ ] **Audit log + health check** — every decision logged with UTC timestamps; health endpoint for broker, AI and DB status.
- [ ] **AI performance tracking** — compare signals with what the price did afterwards, per provider/model.
- [ ] **Backups and data retention** for the database.
- [ ] **Tests** — `FakeBrokerClient`, `FakeAiProvider`, contract tests; required for risk, execution and adapter code.

## Build order

1. Database schema, `.env` config loading, local-only access.
2. Broker adapters on paper/demo accounts only: account summary, positions, orders, reconnect.
3. REST API, so the frontend can switch from mock data (`VITE_USE_MOCK=false`) to the real backend.
4. AI providers + router, strict validation, persistence. Signals are **stored only — no orders yet**.
5. Risk engine → execution → order lifecycle, with tests.
6. Scheduler, market hours, kill switch, alerts.
7. Manual trading.
8. AI performance tracking.

Run steps 1–4 for a while before enabling automated orders, so the AI's decisions can be reviewed before it can place trades (even on a paper account).
