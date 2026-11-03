# Database schema

PostgreSQL, managed by Flyway. Schema source of truth: `backend/src/main/resources/db/migration/` (`V1__initial_schema.sql` is the initial schema). **Never edit an applied migration** — add a new `V<n>__description.sql`, and update this file to match.

Rules: timestamps `TIMESTAMPTZ` in UTC; money/prices/quantities `NUMERIC(38,18)` ↔ `BigDecimal` (crypto prices and quantities need the decimals); every money value has a currency, `VARCHAR(12)` so crypto codes like `USDT` fit; no credentials in the DB (they're in `.env`).

## Broker neutrality

Adding a broker must never need a migration:

- **Open-ended, no `CHECK`:** values that come from brokers — `broker`, `asset_class`, all broker IDs. AI provider types too.
- **`TEXT` + `CHECK`:** our own vocabulary that core code branches on (order status, side, order type, cash transaction type, …). Adapters map native values to it; anything unclassifiable goes to `OTHER` where the list has one.
- **Broker-specific extras:** `JSONB` columns (`instrument_mappings.details`, `order_events.raw`, `fills.raw`, `cash_transactions.raw`), never new broker-named columns.
- **Missing IDs:** if a broker has no ID for a fill or cash transaction, the adapter derives a stable one (same event → same ID), so re-syncing can't create duplicates.

## Accounts & app state

| Table | Purpose |
|---|---|
| `broker_accounts` | Configured accounts (alias, broker, PAPER/LIVE, base currency, enabled). Upserted from `application.yml` + `.env` at startup. An alias never changes broker or PAPER/LIVE, because history hangs off it: the startup upsert refuses that, and a different one needs a new alias. |
| `app_settings` | Single row: `active_account_id`, `last_seen_env_active_account`, `trading_enabled` (kill switch), `analysis_interval_minutes`, `ai_router_strategy`. |
| `active_account_changes` | History of broker switches (from, to, source UI/ENV/STARTUP, time). |
| `risk_limits` | Limits per account (max position/order value, max daily loss, max trades/day, allowed symbols). |

## AI models

| Table | Purpose |
|---|---|
| `ai_providers` | List of AI models: type, model, FREE/PAID, **`priority`** (1 = first; order TBD), `enabled`, **`unavailable_reason` + `unavailable_until`** (tokens used up → date+time it's usable again), own budgets. |
| `ai_quota_events` | Every "tokens used / quota / rate limit" error: when detected, **`reset_at`**, whether the reset time came from the provider or was estimated. |
| `analysis_runs` | One analysis cycle for the active account (scheduled/manual, status, start/end). |
| `ai_calls` | Every AI request incl. failed ones: provider, model, attempt number (fallback order), outcome, tokens, cost, latency, prompt, raw response. |
| `v_ai_provider_status` (view) | Models in priority order with `available` (now) and `available_again_at`. |

A provider is available when `enabled AND (unavailable_until IS NULL OR unavailable_until <= now())`. On a quota error the router sets `unavailable_until` and inserts an `ai_quota_events` row in the same transaction (see [ai-providers.md](ai-providers.md)).

## Trading

| Table | Purpose |
|---|---|
| `signals` | Every AI decision: BUY/SELL/HOLD, size, confidence, **reasoning** (why), which AI model + fallbacks, status, risk-check rejection reason. |
| `orders` | Bids/orders; `source` = AI (`signal_id`), MANUAL (our UI) or EXTERNAL (found at the broker, not sent by us). **Reasoning**, our `client_order_id` (idempotency; required unless EXTERNAL, which needs `broker_order_id` instead), `instrument_mapping_id` (exactly what was traded), broker order ID, side, `position_effect` + `broker_position_id` (see below), type, size, limit price, status, fill summary, and lifecycle times: `created_at`, `submitted_at`, `filled_at`, `cancelled_at`, `rejected_at`, `close_reason`. Order type `OTHER` is for EXTERNAL orders only; we place only MARKET and LIMIT. |
| `order_events` | Full status history of each order with time and message (when it was submitted, partially filled, cancelled, rejected, …). |
| `fills` | Executions from the broker (partial fills supported), with `commission_currency` (it can differ from the fill currency), `source` (LIVE / BACKFILL) and the raw payload. `orders.commission` and `trades.fees` are summaries converted to the order/trade currency. |
| `trades` | Round trips: entry → exit. **Start price** (`entry_price`), **end price** (`exit_price`), quantity, fees, **profit** (`realized_pnl`, `realized_pnl_pct`), times, and links to the signals with the **reasoning for buying and selling** (`entry_signal_id`, `exit_signal_id`; `entry_note`/`exit_note` for manual). |
| `trade_fills` | Which fills opened/closed which trade (partial entries/exits). |
| `v_trade_history` (view) | Trades with entry/exit reasoning, profit and `successful` (= `realized_pnl > 0`). |

LONG trade = buy then sell; SHORT trade = sell then buy back. Only one OPEN trade per account + instrument + side + broker position.

**Netting vs. separate positions.** Some brokers net all BUY/SELL on an instrument into one position (IBKR style): `position_effect` and `broker_position_id` are NULL, and there is one OPEN trade per instrument + side. Others keep each opening as its own position with its own ID, closed by that ID, and may hold long and short together (eToro style): `position_effect` is OPEN/CLOSE, and each broker position is its own trade and its own `position_snapshots` row. So `symbol` + `side` is not unique for positions. Adapters declare which model they use in `BrokerCapabilities`.

## Balance & portfolio history

| Table | Purpose |
|---|---|
| `account_snapshots` | Account money over time, taken on a schedule and after every fill: **`total_value`** (all money in the broker), **`invested_value`** (positions' market value), **`reserved_for_orders`** (money under open bids), **`free_cash`** (available), `cash`, unrealized/realized/daily P&L. `reason` (SCHEDULED / AFTER_FILL / ANALYSIS_RUN / END_OF_DAY / MANUAL) and `source` (LIVE / BACKFILL); at most one END_OF_DAY per account per UTC day. Schedule and retention: [brokers.md](brokers.md). |
| `position_snapshots` | Positions belonging to each account snapshot, linked to `instrument_mappings`, with `broker_position_id` where the broker has one. |
| `cash_transactions` | Money moving other than through fills: deposits, withdrawals, dividends, interest, fees, financing, tax, FX. Signed `amount` (positive = in), `source` (LIVE / BACKFILL), unique per account + broker transaction ID. Lets the balance chart tell a deposit from profit. |

Relations: `free_cash = cash - reserved_for_orders`; `total_value ≈ cash + invested_value`. Store the broker's reported values; if the broker doesn't report `reserved_for_orders`, compute it from our open BUY orders.

LIVE snapshots must have the full breakdown. BACKFILL snapshots only require `total_value`, since broker history often has nothing else: leave the rest NULL, never a made-up 0. `daily_pnl` and `realized_pnl_today` are NULL whenever the broker doesn't report them.

## Other

| Table | Purpose |
|---|---|
| `instrument_mappings` | Our symbol + exchange + currency → broker's instrument ID, per broker, with its trading rules (`multiplier`, `min_quantity`, `quantity_step`, `price_tick`; NULL = not reported) and `details` (JSONB) for anything else the adapter needs. Orders, trades and position snapshots reference it (its `broker` must match the account's; the backend enforces this), so the same symbol on two exchanges stays separate. |
| `market_data` | Price bars (OHLCV) used for AI analysis, per symbol + exchange + source. |
| `audit_log` | Every other decision/event/error (kill switch, reconnects, startup checks, …) with UTC time. |
