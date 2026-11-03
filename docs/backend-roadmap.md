# Backend roadmap

What the backend must do, and in which order to build it. Tick items off (`[x]`) as they are done and keep this file current.

## Features

### AI
- [ ] **Prompt building** — send the AI market data plus context: current positions, cash, risk limits, recent signals. Same prompt template for every provider ([ai-providers.md](ai-providers.md)).
- [ ] **AI provider adapters + `AiRouter`** — priority order, automatic switch to the next model on quota/technical errors, quota reset time stored in DB, usage/budget tracking.
- [ ] **Strict response validation** — parse into the signal schema; anything malformed or ambiguous = HOLD.
- [ ] **Persist AI communication** — every call in `ai_calls` (provider, model, tokens, cost, latency, outcome), raw response + parsed signal in `signals`.

### Broker
- [ ] **Broker adapters** (IBKR, eToro) behind `BrokerClient` ([brokers.md](brokers.md)), paper/demo first.
- [ ] **Account summary and positions** — fetch from the active account; store `account_snapshots` + `position_snapshots` for history (schedule + retention in [brokers.md](brokers.md)).
- [ ] **History reconciliation/backfill** — per adapter, fill gaps in orders/fills/daily equity from the broker's history source (IBKR Flex Web Service, eToro trade history).
- [ ] **Order lifecycle** — track each order SUBMITTED → PARTIALLY_FILLED → FILLED / CANCELLED / REJECTED; record fills, including late ones.
- [ ] **Instrument mapping** — symbol → broker's own ID, cached in `instrument_mappings`.
- [ ] **Reconnect + resync** — reconnect with backoff, reload state from the broker; unknown order outcome = "maybe placed", reconcile first.
- [ ] **Active account switching** — `ActiveAccountService`, `PUT /api/accounts/active`, rules in [brokers.md](brokers.md).

### Trading
- [ ] **Market data** — prices/history from the broker or a free source behind `MarketDataProvider`.
- [ ] **Risk engine** — max position value, max order value, max daily loss, max trades/day, allowed symbols, per account. Rejected signals stored with reason.
- [ ] **Daily P&L / loss tracking** — needed for the max-daily-loss limit.
- [ ] **Order execution** — signal → `OrderRequest`, units ↔ amount conversion via `BrokerCapabilities`, idempotent (own `clientOrderId`, check open orders/positions first).
- [ ] **Automated trading** — scheduler runs analysis → risk check → execution.
- [ ] **Manual trading** — endpoint to place/cancel orders by hand (needs adding to [api-contract.md](api-contract.md)). Manual orders also pass the risk engine.
- [ ] **Scheduler + market hours** — `@Scheduled` runs, market hours and holidays per instrument; no orders while the market is closed.
- [ ] **Kill switch** — `POST /api/trading/stop|start`, persists across restarts and account switches.

### Platform
- [ ] **Database schema + Flyway migrations** — `V1__initial_schema.sql` written and tested on PostgreSQL 18 ([database.md](database.md)); still needs the Maven project + Flyway wiring and JPA entities.
- [ ] **Config + secrets** — everything from the root `.env` (template: `.env.example`), bound via `@ConfigurationProperties`; accounts enabled by which `.env` values are filled in ([configuration.md](configuration.md)).
- [ ] **Startup safety checks** — refuse to start if LIVE/PAPER doesn't match the credentials or port.
- [ ] **REST API for the frontend** — exactly the shapes in [api-contract.md](api-contract.md) / `frontend/src/types.ts`.
- [ ] **Settings storage** — risk limits and AI provider order/enabled in DB, changeable at runtime.
- [ ] **Local-only access (no login)** — single-user app, no login/logout. Bind the backend to `127.0.0.1`, CORS only for the local frontend ([configuration.md](configuration.md)).
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
