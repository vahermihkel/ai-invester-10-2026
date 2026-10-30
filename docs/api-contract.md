# REST API contract (frontend ↔ backend)

Types live in `frontend/src/types.ts`; the backend DTOs must match. Only one broker account is active at a time; the UI shows only that account and passes its `accountId` explicitly. Money/price/quantity fields are JSON **strings** (serialized `BigDecimal`), timestamps ISO-8601 UTC. `broker` is a string (e.g. `IBKR`, `ETORO`) — the frontend must not assume a fixed list. `accountId` is always the internal alias, never the real broker account number.

| Method | Path | Returns |
|---|---|---|
| GET | `/api/status` | `SystemStatus` — activeAccountId (null = none), tradingEnabled, brokerConnected + marketOpen (of the active account), lastRunAt |
| GET | `/api/accounts` | `BrokerAccount[]` — all configured accounts: accountId, broker, displayName, tradingMode, enabled, baseCurrency, capabilities |
| PUT | `/api/accounts/active` | body `{ accountId }` — switch the active account; returns `SystemStatus`. 400 if unknown/disabled, or LIVE without `ALLOW_LIVE_TRADING=true` |
| GET | `/api/account?accountId=X` | `AccountSummary` |
| GET | `/api/positions?accountId=X` | `Position[]` (active account if omitted) |
| GET | `/api/signals?accountId=X&limit=N` | `Signal[]` (newest first; active account if omitted) |
| GET | `/api/orders?accountId=X&limit=N` | `Order[]` (newest first; active account if omitted) |
| GET / PUT | `/api/settings` | `RiskSettings` (analysis interval + limits per account) |
| POST | `/api/trading/stop` | kill switch: stop automated trading |
| POST | `/api/trading/start` | resume automated trading |
| GET | `/api/ai/providers` | `AiProviderStatus[]` — id, type, model, tier (FREE/PAID), enabled, priority, available, unavailableReason, unavailableUntil (quota reset time, UTC), usage today/this month (tokens, requests, cost) vs. budgets. _Planned — not yet in frontend_ |
| PUT | `/api/ai/providers` | update `enabled` / `priority` / budgets / router strategy (never API keys). _Planned — not yet in frontend_ |

Every `Position`, `Signal`, `Order` includes `broker` and `accountId`. Paper/live mode and which brokers/accounts exist are backend config only; the frontend can display them and choose which configured account is active, but never change their mode or credentials.

## Generic (broker-neutral) API models

The same models are used for IBKR, eToro and any future broker. **Never add broker-specific fields** (no `conId`, `instrumentId`, `netLiquidation`, `MKT`/`LMT` etc.); express broker differences through `BrokerCapabilities`, and map native values to these shapes in the backend adapter. The frontend must never branch on the broker name.

- **`TradingMode`** — `PAPER` (any paper/demo/sandbox environment: IBKR paper, eToro demo/virtual) or `LIVE`.
- **`OrderSize`** — `{ type: 'UNITS' | 'AMOUNT', value }`. `UNITS` = shares/coins (IBKR style), `AMOUNT` = cash in the account currency (eToro style). Used by `Signal.size` (null for HOLD) and `Order.size`.
- **`BrokerCapabilities`** — `orderTypes` (`MARKET`/`LIMIT`), `sizeTypes`, `fractionalUnits`, `shortSelling`, `leverage`, `assetClasses` (`STOCK`, `ETF`, `CRYPTO`, `FX`, `COMMODITY`, `INDEX`, `OTHER`).
- **`AccountSummary`** — accountId, broker, `equity` (IBKR net liquidation / eToro equity), cash, dailyPnl, unrealizedPnl, currency.
- **`Position`** — accountId, broker, symbol, assetClass, `side` (`LONG`/`SHORT`, quantity always positive), quantity (units, may be fractional), avgPrice, marketPrice, marketValue, unrealizedPnl, currency, `leverage` (null = none).
- **`Order`** — accountId, broker, `clientOrderId` (our idempotency key), `brokerOrderId` (null until the broker acknowledges), symbol, side, size, `orderType` (`MARKET`/`LIMIT`), limitPrice, status, filledQuantity (units), avgFillPrice, currency, signalId.
- **`SystemStatus`** — `activeAccountId`, `tradingEnabled`, `brokerConnected`, `marketOpen`, `lastRunAt`.
- **`RiskSettings`** — analysisIntervalMinutes, `accounts: { accountId, limits: RiskLimits }[]` (limits in the account's base currency; the UI edits only the active account's entry and sends the rest back unchanged). `RiskLimits` = maxPositionValue, maxOrderValue, maxDailyLoss, maxTradesPerDay, allowedSymbols.
- **`Signal`** — will also get `aiProvider` (alias), `aiModel` and `fallbackFrom` (_planned — not yet in frontend_).
- **`AiProviderType`** — open string type like `BrokerType` (`'OPENAI' | 'GEMINI' | 'GROK' | 'CLAUDE' | 'OLLAMA' | 'OPENAI_COMPATIBLE' | string`); the UI never branches on it.
- **`BrokerType`** — open string type (`'IBKR' | 'ETORO' | string`); adding a broker must not require a frontend change.

The mock backend (`frontend/src/mockData.ts`) has one IBKR paper and one eToro demo account (IBKR active, switchable); keep both in sync with `types.ts` when models change.
