# AI Investor

Automated trading app. A free AI model analyses market data and produces buy/sell signals; the backend turns those signals into orders on one or more of the user's brokerage accounts. Supported brokers: **Interactive Brokers (IBKR)** and **eToro**. More brokers will be added later, so the code must be broker-agnostic (see "Multi-broker architecture"). Every signal, order and fill is stored in a database so history can be reviewed in the frontend.

## Project layout

```
ai-investor/
├── backend/    # API server, broker adapters (IBKR, eToro, …), AI signal engine, trade execution, scheduler
├── frontend/   # Web UI: dashboard, positions, trade history, AI decisions, settings
└── CLAUDE.md
```

The frontend is set up (see Commands); the backend folder is still empty. Update this file when the stack is chosen and code is added (commands, structure, env vars).

## Tech stack (planned — update when decided)

- **Backend:** Java 21 + Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation)
- **Broker connections:** each broker is an adapter behind our own `BrokerClient` interface (see below)
  - **IBKR:** official IBKR TWS API Java client (`EClient`/`EWrapper`) talking to TWS or IB Gateway; the adapter turns callbacks into `CompletableFuture`s and handles reconnects
  - **eToro:** eToro public REST API (API key + user key, separate demo/virtual and real environments) called via `RestClient`. Check the current eToro API docs for endpoints, auth headers, rate limits and order semantics before implementing — don't guess them
  - **....** any broker can be added later without any problems / major work
- **AI model:** a free model (e.g. a local model via Ollama, or a free-tier hosted API) called via Spring AI or `RestClient`. Keep it behind one interface so the model can be swapped
- **Database:** PostgreSQL; Flyway migrations; Testcontainers (or H2) for tests
- **Frontend:** React + Vite + TypeScript
- **Scheduling:** Spring `@Scheduled` for periodic analysis runs during market hours
- **Build:** Maven

## Architecture / data flow

1. **Market data** — fetch prices/history (from a broker's market data via its adapter, or another free source behind a `MarketDataProvider` interface).
2. **AI analysis** — send data to the AI model, get back a structured, broker-neutral signal: `symbol`, `action` (BUY/SELL/HOLD), `quantity` or size hint, `confidence`, `reasoning`. The AI never knows or chooses which broker is used.
3. **Risk check** — validate the signal against risk rules (see below), for the specific target account. Rejected signals are still stored, with the rejection reason.
4. **Execution** — the execution service maps the signal to a broker-neutral `OrderRequest` and sends it to the target account's `BrokerClient` adapter.
5. **Persistence** — store signals, orders, fills, positions snapshots and errors, each tagged with `broker` and `account_id` (internal alias).
6. **Frontend** — reads everything from the backend REST API; never talks to a broker or the AI model directly.

Suggested core tables: `broker_accounts`, `signals`, `orders`, `fills`, `positions_snapshots`, `market_data`, `settings`, `logs`, `instrument_mappings`.

## Multi-broker architecture — always follow

Code must work for IBKR, eToro **and any broker added later** without changing core logic. Adding a new broker = adding a new adapter module + config, nothing else.

- **One interface, many adapters.** Core code (scheduler, AI engine, risk engine, execution service, controllers) depends only on the `BrokerClient` interface and broker-neutral domain types. Never import IBKR (`com.ib.*`) or eToro classes/DTOs outside their own adapter package (e.g. `broker/ibkr`, `broker/etoro`).
- **`BrokerClient` covers at least:** `connect`/`disconnect`/`isConnected`, `getAccountSummary`, `getPositions`, `getOpenOrders`, `placeOrder(OrderRequest)`, `cancelOrder`, `getOrderStatus`, `getFills`, `resolveInstrument(symbol)`, `isMarketOpen(instrument)`, `capabilities()`. All async methods return `CompletableFuture`; all results are our own domain records.
- **Broker-neutral domain model.** `Instrument`, `OrderRequest`, `Order`, `Fill`, `Position`, `AccountSummary`, `OrderStatus` are our own records. Each adapter maps to/from the broker's native format (IBKR `Contract`/`Order`, eToro instrument IDs / amount-based positions, etc.) and maps broker statuses and errors to our enums/exceptions.
- **Capabilities, not `if (broker == X)`.** Each adapter declares a `BrokerCapabilities` record (e.g. supported order types, fractional quantities, size by amount vs. by units, short selling, leverage/CFD, supported asset classes, min order size, rate limits). Core code checks capabilities and rejects (stores the rejection reason) instead of branching on broker name. Example: eToro opens positions by cash amount, IBKR by share quantity — the execution service converts via capabilities and current price, using `BigDecimal`.
- **Instrument mapping.** Our canonical key is the symbol (+ exchange/currency where needed). Each adapter resolves it to the broker's own ID (IBKR `conId`, eToro `instrumentId`, …) and caches it in `instrument_mappings`. If a symbol can't be resolved unambiguously → no order.
- **Accounts are configuration.** Brokers/accounts are defined in config (`app.brokers.accounts[]`: alias, broker type, environment PAPER/LIVE, enabled, credentials via env vars). Every signal and order targets an explicit account alias; never a "default broker" hidden in code. Adapters are discovered as Spring beans keyed by broker type (e.g. `Map<BrokerType, BrokerClientFactory>`).
- **Per-account isolation.** Risk limits, kill switch, daily loss/trade counters, reconnect state and idempotency checks are tracked per account (plus the global kill switch). A failure on one broker must not stop or affect another.
- **Same safety rules for every broker.** Every adapter must implement paper/live separation, reconnect + resync, idempotent ordering and full logging (below). A new adapter isn't done until it has these.
- **Contract tests.** One shared abstract test suite for the `BrokerClient` contract that every adapter (and the in-memory `FakeBrokerClient` used in tests) must pass. Adapter-specific mapping tests use recorded/mocked broker responses, never a real account.

## Safety rules — always follow

This app moves real money. When writing or changing code:

- **Paper/demo trading by default, for every broker.**
  - IBKR: paper account (TWS port 7497 / Gateway 4002). Live = 7496 / 4001.
  - eToro: demo/virtual portfolio environment and demo keys. Real = real-environment keys/endpoints.
  - Any future broker: its sandbox/paper/demo environment.
  - Live trading only when explicitly enabled per account by config, never as a default. The backend must refuse to start an account in LIVE if the environment and the credentials/port don't match (e.g. LIVE flag with paper port, or demo keys flagged as real).
- **Never trust AI output blindly.** Validate and parse it strictly (JSON schema). Malformed or ambiguous output = HOLD, no order.
- **Risk limits enforced in backend code, not in the prompt:** max position size, max order value, max daily loss, max trades per day, allowed symbols list. These must be configurable, per account, with global caps on top. Values are converted to the account's base currency before checks.
- **Kill switch:** a setting/endpoint that immediately stops all automated trading (global), plus per-account stop.
- **No duplicate orders:** check open orders and current positions on the target account before placing; make execution idempotent (our own client order ID per signal + account, stored before sending; on retry/reconnect check the broker for that order before resending).
- **Log every decision** — signal, target account, risk check result, order, fill and any error — with timestamps (UTC).
- **Handle disconnects:** any broker connection/API can drop or rate-limit; reconnect safely with backoff and resync state from the broker, don't assume. Treat an unknown order outcome (timeout) as "maybe placed" and reconcile before doing anything else.
- **Market hours:** don't send orders when the market is closed unless explicitly intended. Market hours come from the adapter/instrument, not a hardcoded exchange.
- Any change to order execution, risk logic or a broker adapter needs tests.

## Secrets & config

- Credentials, account IDs, API keys go in `.env` (never committed), one set per broker account (e.g. `IBKR_PAPER_HOST`, `IBKR_PAPER_PORT`, `ETORO_DEMO_API_KEY`, `ETORO_DEMO_USER_KEY`). Keep a `.env.example` with placeholder values for every broker.
- `.gitignore` must include `.env`, database files, logs, `node_modules/`, `target/`, `build/`, `.gradle/`, `.idea/`.
- Never hardcode account numbers, API keys or user keys in code or print them in logs. The app refers to accounts by internal alias only; the real broker account ID stays in config and is masked anywhere it is displayed.

## Commands

Frontend (run in `frontend/`):
- `npm install` — install dependencies
- `npm run dev` — dev server on http://localhost:5173 (proxies `/api` to the backend on :8080)
- `npm run build` — type-check + production build

`frontend/.env.development` sets `VITE_USE_MOCK=true`, so the UI uses in-memory mock data (`src/mockData.ts`). Set it to `false` once the backend is running.

Backend: _to be filled in once the Maven project is set up._

## REST API contract (frontend ↔ backend)

Types live in `frontend/src/types.ts`; the backend DTOs must match. Money/price/quantity fields are JSON **strings** (serialized `BigDecimal`), timestamps ISO-8601 UTC. `broker` is a string (e.g. `IBKR`, `ETORO`) — the frontend must not assume a fixed list. `accountId` is always the internal alias, never the real broker account number.

| Method | Path | Returns |
|---|---|---|
| GET | `/api/status` | `SystemStatus` — tradingEnabled (global), lastRunAt, and per account: accountId, broker, tradingMode (PAPER/LIVE), brokerConnected, tradingEnabled, marketOpen |
| GET | `/api/accounts` | `BrokerAccount[]` — accountId, broker, displayName, tradingMode, enabled |
| GET | `/api/account?accountId=X` | `AccountSummary` |
| GET | `/api/positions?accountId=X` | `Position[]` (all accounts if omitted) |
| GET | `/api/signals?limit=N&accountId=X` | `Signal[]` (newest first) |
| GET | `/api/orders?limit=N&accountId=X` | `Order[]` (newest first) |
| GET / PUT | `/api/settings` | `RiskSettings` (global caps + per-account limits) |
| POST | `/api/trading/stop` | kill switch: stop all automated trading (`?accountId=X` stops one account) |
| POST | `/api/trading/start` | resume automated trading (`?accountId=X` for one account) |

Every `Position`, `Signal`, `Order` includes `broker` and `accountId`. Paper/live mode and which brokers/accounts exist are backend config only; the frontend can display them but never change them.

## Conventions

- Backend: Java records + Bean Validation for API DTOs and AI-signal schemas (parse AI output with Jackson, reject unknown/invalid fields); business logic in services, not controllers.
- Config via `@ConfigurationProperties` and Spring profiles; `paper` is the default profile, `live` must be enabled explicitly (and per account).
- Money/prices/quantities: use `BigDecimal` (`NUMERIC` in PostgreSQL), never `double`/`float`, in the DB and order logic. Always store the currency with money values.
- Times stored in UTC.
- Keep the AI provider, each broker adapter and the risk engine as separate modules so each can be tested/mocked independently.
- Broker-specific code, DTOs and quirks stay inside that broker's adapter package; document each adapter's quirks in a short `README.md` in its package.
