# REST API contract (frontend ↔ backend)

Types live in `frontend/src/types.ts`; the backend DTOs must match. Only one broker account is active at a time; the UI shows only that account and passes its `accountId` explicitly. Money/price/quantity fields are JSON **strings** (serialized `BigDecimal`), currencies are strings of up to 12 characters (`USD`, `USDT`, …), timestamps ISO-8601 UTC. `broker` is a string (e.g. `IBKR`, `ETORO`) — the frontend must not assume a fixed list. `accountId` is always the internal alias, never the real broker account number.

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
| GET | `/api/ai/providers` | `AiProviderStatus[]` — id, type, model, tier (FREE/PAID), enabled, priority, available, unavailableReason, unavailableUntil (quota reset time, UTC), usageToday/usageThisMonth (tokens, requests, cost), budgets, costCurrency. _Planned — type only, no UI yet_ |
| PUT | `/api/ai/providers` | update `enabled` / `priority` / budgets / router strategy (never API keys). _Planned — not yet in frontend_ |
| GET | `/api/trades?accountId=X&status=OPEN\|CLOSED&limit=N` | `Trade[]` — round trips with entry/exit price, profit, buy/sell reasoning (from `v_trade_history`). _Planned — type only, no UI yet_ |
| GET | `/api/account/history?accountId=X&from=&to=` | `AccountSnapshot[]` — total value, invested, reserved for orders, free cash over time. _Planned — type only, no UI yet_ |
| GET | `/api/cash-transactions?accountId=X&from=&to=` | `CashTransaction[]` — deposits, withdrawals, dividends, fees, … (newest first). _Planned — type only, no UI yet_ |
| GET | `/api/orders/{id}/events` | `OrderEvent[]` — status history of one order (submitted, cancelled, …). _Planned — type only, no UI yet_ |
| GET | `/api/orders/{id}/fills` | `Fill[]` — executions of one order. _Planned — type only, no UI yet_ |

Every `Position`, `Signal`, `Order` includes `broker` and `accountId`. Paper/live mode and which brokers/accounts exist are backend config only; the frontend can display them and choose which configured account is active, but never change their mode or credentials.

## Generic (broker-neutral) API models

The same models are used for IBKR, eToro and any future broker. **Never add broker-specific fields** (no `conId`, `instrumentId`, `netLiquidation`, `MKT`/`LMT` etc.); express broker differences through `BrokerCapabilities`, and map native values to these shapes in the backend adapter. The frontend must never branch on the broker name.

- **`TradingMode`** — `PAPER` (any paper/demo/sandbox environment: IBKR paper, eToro demo/virtual) or `LIVE`.
- **`OrderSize`** — `{ type: 'UNITS' | 'AMOUNT', value }`. `UNITS` = shares/coins (IBKR style), `AMOUNT` = cash in the account currency (eToro style). Used by `Signal.size` (null for HOLD) and `Order.size`.
- **`AssetClass`** — open string type like `BrokerType` (`'STOCK' | 'ETF' | 'CRYPTO' | 'FX' | 'COMMODITY' | 'INDEX' | 'OTHER' | string`); the UI only displays it.
- **`BrokerCapabilities`** — `orderTypes` (`MARKET`/`LIMIT`), `sizeTypes`, `fractionalUnits`, `shortSelling`, `leverage`, `assetClasses`.
- **`AccountSummary`** — accountId, broker, `equity` (IBKR net liquidation / eToro equity), cash, dailyPnl, unrealizedPnl, currency.
- **`Position`** — accountId, broker, symbol, `exchange` (`''` = none), assetClass, `side` (`LONG`/`SHORT`, quantity always positive), `brokerPositionId` (null on netting brokers), quantity (units, may be fractional), avgPrice, marketPrice, marketValue, unrealizedPnl, currency, `leverage` (null = none). `symbol` + `side` is not unique, because the same symbol can trade on several exchanges and some brokers keep several positions per instrument.
- **`Order`** — accountId, broker, `source` (`AI`/`MANUAL`/`EXTERNAL`), reasoning, `clientOrderId` (our idempotency key; null for external orders), `brokerOrderId` (null until the broker acknowledges), symbol, assetClass, side, `positionEffect` (`OPEN`/`CLOSE`, null on netting brokers), `brokerPositionId`, size, `orderType` (`MARKET`/`LIMIT`, or `OTHER` on external orders), limitPrice, leverage, status (`CREATED`, `SUBMITTED`, `PARTIALLY_FILLED`, `FILLED`, `CANCELLED`, `REJECTED`, `UNKNOWN`), filledQuantity (units), avgFillPrice, commission (in the order currency), currency, signalId, submittedAt, filledAt, cancelledAt, rejectedAt, closeReason.
- **`OrderEvent`** (_planned endpoint_) — id, orderId, status, eventAt, message.
- **`Fill`** (_planned endpoint_) — id, orderId, brokerFillId, quantity, price, commission (negative = rebate), commissionCurrency, currency, filledAt, source (`LIVE`/`BACKFILL`).
- **`SystemStatus`** — `activeAccountId`, `tradingEnabled`, `brokerConnected`, `marketOpen`, `lastRunAt`.
- **`RiskSettings`** — analysisIntervalMinutes, `accounts: { accountId, limits: RiskLimits }[]` (limits in the account's base currency; the UI edits only the active account's entry and sends the rest back unchanged). `RiskLimits` = maxPositionValue, maxOrderValue, maxDailyLoss, maxTradesPerDay, allowedSymbols.
- **`Signal`** — also has `aiProvider` (alias), `aiModel` and `fallbackFrom` (`{ provider, reason }[]`, the providers skipped and why).
- **`Trade`** (_planned endpoint_) — id, accountId, broker, symbol, assetClass, side (LONG/SHORT), status (OPEN/CLOSED), quantity, entryPrice, exitPrice, fees, realizedPnl, realizedPnlPct, successful (null while open), currency, openedAt, closedAt, entryReasoning, exitReasoning, entryAiProvider, exitAiProvider.
- **`AccountSnapshot`** (_planned endpoint_) — id, accountId, takenAt, reason, source (LIVE/BACKFILL), currency, totalValue, and investedValue, reservedForOrders, freeCash, cash, unrealizedPnl, realizedPnlToday, dailyPnl (each nullable: broker history often has only the total, so charts must leave gaps, not show 0).
- **`CashTransaction`** (_planned endpoint_) — id, accountId, broker, type (`DEPOSIT`, `WITHDRAWAL`, `DIVIDEND`, `INTEREST`, `FEE`, `FINANCING`, `TAX`, `FX`, `ADJUSTMENT`, `OTHER`), amount (positive = in, negative = out), currency, symbol, description, occurredAt, source (LIVE/BACKFILL).
- **`AiProviderStatus`** (_planned endpoint_) — see `/api/ai/providers` above; `usageToday` and `usageThisMonth` are `{ tokens, requests, cost }`, budgets are `maxTokensPerDay`, `maxTokensPerMonth`, `maxRequestsPerDay`, `maxCostPerMonth` (null = no limit).
- **`AiProviderType`** — open string type like `BrokerType` (`'OPENAI' | 'GEMINI' | 'GROK' | 'CLAUDE' | 'OLLAMA' | 'OPENAI_COMPATIBLE' | string`); the UI never branches on it.
- **`BrokerType`** — open string type (`'IBKR' | 'ETORO' | string`); adding a broker must not require a frontend change.

The mock backend (`frontend/src/mockData.ts`) has one IBKR paper and one eToro demo account (IBKR active, switchable); keep both in sync with `types.ts` when models change.
